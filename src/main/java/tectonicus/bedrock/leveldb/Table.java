/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Read-only SSTable ({@code .ldb}/{@code .sst}) reader.
 * <p>
 * Layout: data blocks, meta blocks, a metaindex block, an index block, and a 48-byte footer holding the
 * metaindex and index block handles (padded to 40 bytes) and the 8-byte magic number. Every block is
 * followed by a 5-byte trailer: a compression type byte and a masked CRC32C of the block and type byte.
 * <p>
 * Thread-safe: reads use positional {@link FileChannel} reads and parsed blocks are immutable.
 * Instances are reference counted by {@link TableCache}; the file is closed once the table has been
 * evicted and the last user released it.
 */
@Slf4j
final class Table {
	static final long MAGIC = 0xdb4775248b80fb57L;
	static final int FOOTER_SIZE = 48;
	static final int BLOCK_TRAILER_SIZE = 5;

	record BlockCacheKey(long fileNumber, long offset) {
	}

	private final long fileNumber;
	private final Path path;
	private final FileChannel channel;
	private final boolean verifyChecksums;
	private final Cache<BlockCacheKey, Block> blockCache;
	private final long fileSize;
	private final Block indexBlock;

	private int refs;
	private boolean evicted;

	private Table(long fileNumber, Path path, FileChannel channel, boolean verifyChecksums, Cache<BlockCacheKey, Block> blockCache) throws IOException {
		this.fileNumber = fileNumber;
		this.path = path;
		this.channel = channel;
		this.verifyChecksums = verifyChecksums;
		this.blockCache = blockCache;

		this.fileSize = channel.size();
		if (fileSize < FOOTER_SIZE) {
			throw new LevelDbException("Table " + path + " is too short (" + fileSize + " bytes) to be an sstable");
		}
		byte[] footer = readFully(fileSize - FOOTER_SIZE, FOOTER_SIZE);
		long magic = SliceInput.getFixed64(footer, FOOTER_SIZE - 8);
		if (magic != MAGIC) {
			throw new LevelDbException("Table " + path + " has bad magic number " + Long.toHexString(magic));
		}
		SliceInput in = new SliceInput(footer, 0, FOOTER_SIZE - 8);
		BlockHandle.decode(in); // metaindex (filters are not used)
		BlockHandle index = BlockHandle.decode(in);
		this.indexBlock = readBlock(index, false);
	}

	/** Opens a table file read-only. */
	static Table open(long fileNumber, Path path, boolean verifyChecksums, Cache<BlockCacheKey, Block> blockCache) throws IOException {
		FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
		try {
			return new Table(fileNumber, path, channel, verifyChecksums, blockCache);
		} catch (IOException | RuntimeException e) {
			channel.close();
			throw e;
		}
	}

	long fileNumber() {
		return fileNumber;
	}

	Path path() {
		return path;
	}

	boolean isOpen() {
		return channel.isOpen();
	}

	// ---- reference counting (used by TableCache) ----

	synchronized boolean tryAcquire() {
		if (evicted) {
			return false;
		}
		refs++;
		return true;
	}

	synchronized void release() {
		refs--;
		if (refs == 0 && evicted) {
			closeChannel();
		}
	}

	synchronized void evict() {
		evicted = true;
		if (refs == 0) {
			closeChannel();
		}
	}

	private void closeChannel() {
		try {
			channel.close();
		} catch (IOException e) {
			log.debug("Failed to close {}", path, e);
		}
	}

	// ---- reading ----

	/** Looks up the newest entry for the user key of {@code lookupKey} in this table, or null if none. */
	LookupResult get(byte[] lookupKey) throws IOException {
		try (Iter it = iterator(true)) {
			it.seek(lookupKey);
			if (it.isValid() && InternalKeys.sameUserKey(it.key(), lookupKey)) {
				return InternalKeys.type(it.key()) == InternalKeys.TYPE_VALUE ? new LookupResult(it.value()) : LookupResult.DELETED;
			}
			return null;
		}
	}

	Block.Iter indexIterator() {
		return indexBlock.iterator();
	}

	/**
	 * @param fillCache whether data blocks read by this iterator go into the shared block cache
	 *                  (false for full scans so they don't evict the working set)
	 */
	Iter iterator(boolean fillCache) {
		return new Iter(fillCache);
	}

	Block readBlock(BlockHandle handle, boolean fillCache) throws IOException {
		if (blockCache != null) {
			BlockCacheKey key = new BlockCacheKey(fileNumber, handle.offset());
			Block cached = blockCache.getIfPresent(key);
			if (cached != null) {
				return cached;
			}
			Block block = readBlockUncached(handle);
			if (fillCache) {
				blockCache.put(key, block);
			}
			return block;
		}
		return readBlockUncached(handle);
	}

	/** Reads the raw block contents and trailer, verifying the checksum if enabled. Returns {@code [contents..., type]}. */
	byte[] readRawBlock(BlockHandle handle) throws IOException {
		long size = handle.size();
		if (handle.offset() < 0 || size < 0 || size > Integer.MAX_VALUE - BLOCK_TRAILER_SIZE
			|| handle.offset() + size + BLOCK_TRAILER_SIZE > fileSize) {
			throw new LevelDbException("Block handle " + handle + " out of range in " + path);
		}
		int n = (int) size;
		byte[] raw = readFully(handle.offset(), n + BLOCK_TRAILER_SIZE);
		if (verifyChecksums) {
			int expected = Crc32c.unmask(SliceInput.getFixed32(raw, n + 1));
			int actual = Crc32c.value(raw, 0, n + 1);
			if (expected != actual) {
				throw new LevelDbException("Block checksum mismatch at offset " + handle.offset() + " in " + path);
			}
		}
		return raw;
	}

	private Block readBlockUncached(BlockHandle handle) throws IOException {
		byte[] raw = readRawBlock(handle);
		int n = (int) handle.size();
		int type = raw[n] & 0xff;
		byte[] contents = Compression.decompress(type, raw, 0, n);
		try {
			return new Block(contents);
		} catch (LevelDbException e) {
			throw new LevelDbException(e.getMessage() + " (block at offset " + handle.offset() + " in " + path + ")", e);
		}
	}

	private byte[] readFully(long position, int length) throws IOException {
		byte[] out = new byte[length];
		ByteBuffer buf = ByteBuffer.wrap(out);
		while (buf.hasRemaining()) {
			int n = channel.read(buf, position + buf.position());
			if (n < 0) {
				throw new LevelDbException("Unexpected end of file reading " + length + " bytes at " + position + " in " + path);
			}
		}
		return out;
	}

	/** Two-level iterator: index block entries point at data blocks. */
	final class Iter implements InternalIterator {
		private final boolean fillCache;
		private final Block.Iter index = indexBlock.iterator();
		private Block.Iter data;

		private Iter(boolean fillCache) {
			this.fillCache = fillCache;
		}

		@Override
		public boolean isValid() {
			return data != null && data.isValid();
		}

		@Override
		public void seekToFirst() throws IOException {
			index.seekToFirst();
			loadDataBlock();
			if (data != null) {
				data.seekToFirst();
			}
			skipEmptyBlocks();
		}

		@Override
		public void seek(byte[] target) throws IOException {
			index.seek(target);
			loadDataBlock();
			if (data != null) {
				data.seek(target);
			}
			skipEmptyBlocks();
		}

		@Override
		public void next() throws IOException {
			data.next();
			skipEmptyBlocks();
		}

		@Override
		public byte[] key() {
			return data.key();
		}

		@Override
		public byte[] value() {
			return data.value();
		}

		@Override
		public void close() {
			data = null;
		}

		private void loadDataBlock() throws IOException {
			if (!index.isValid()) {
				data = null;
				return;
			}
			BlockHandle handle = BlockHandle.decode(index.value());
			data = readBlock(handle, fillCache).iterator();
		}

		private void skipEmptyBlocks() throws IOException {
			while (data != null && !data.isValid()) {
				index.next();
				loadDataBlock();
				if (data != null) {
					data.seekToFirst();
				}
			}
		}
	}
}
