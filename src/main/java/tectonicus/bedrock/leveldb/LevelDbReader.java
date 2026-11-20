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
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only reader for LevelDB databases, including Minecraft Bedrock Edition worlds ({@code <world>/db})
 * written by Mojang's leveldb-mcpe fork (zlib / raw deflate block compression).
 * <p>
 * Opening reads {@code CURRENT}, replays the MANIFEST to find the live table files per level, and replays
 * the unflushed write-ahead logs into an in-memory table. The reader never writes, locks, renames, creates
 * or deletes anything in the database directory; all files are opened read-only and the {@code LOCK} file
 * is ignored. The view is a snapshot as of {@link #open}: the database should not be modified by another
 * process (e.g. a running game) while it is being read.
 * <p>
 * {@link #get} is thread-safe. Cursors are single-threaded, but any number may be used concurrently.
 */
@Slf4j
public final class LevelDbReader implements AutoCloseable {
	private static final Pattern LOG_FILE = Pattern.compile("(\\d+)\\.log");
	private static final int MAX_CLOSED_CHANNEL_RETRIES = 3;

	/** Tuning options for {@link LevelDbReader#open(Path, Options)}. */
	@Getter
	@Builder
	public static final class Options {
		/** Maximum total size of decompressed data blocks kept in memory; 0 disables the block cache. */
		@Builder.Default
		private final long blockCacheBytes = 32L * 1024 * 1024;
		/** Maximum number of table files held open at once. */
		@Builder.Default
		private final int maxOpenTables = 256;
		/** Verify block checksums when reading tables (log and manifest records are always verified). */
		@Builder.Default
		private final boolean verifyChecksums = true;

		public static Options defaults() {
			return builder().build();
		}
	}

	private final Path dbDir;
	private final Version version;
	private final MemTable memTable;
	private final Cache<Table.BlockCacheKey, Block> blockCache;
	private final TableCache tableCache;
	private volatile boolean closed;

	private LevelDbReader(Path dbDir, Version version, MemTable memTable, Options options) {
		this.dbDir = dbDir;
		this.version = version;
		this.memTable = memTable;
		this.blockCache = options.getBlockCacheBytes() > 0
			? Caffeine.newBuilder()
				.maximumWeight(options.getBlockCacheBytes())
				.<Table.BlockCacheKey, Block>weigher((key, block) -> block.size() + 64)
				.build()
			: null;
		this.tableCache = new TableCache(dbDir, Math.max(1, options.getMaxOpenTables()), options.isVerifyChecksums(), blockCache);
	}

	/** Opens the database in {@code dbDir} (e.g. a Bedrock world's {@code db} folder) with default options. */
	public static LevelDbReader open(Path dbDir) throws IOException {
		return open(dbDir, Options.defaults());
	}

	public static LevelDbReader open(Path dbDir, Options options) throws IOException {
		Objects.requireNonNull(dbDir, "dbDir");
		Objects.requireNonNull(options, "options");
		if (!Files.isDirectory(dbDir)) {
			throw new LevelDbException("LevelDB directory does not exist: " + dbDir);
		}
		Version version = Version.load(dbDir);

		List<Long> logNumbers = new ArrayList<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(dbDir, "*.log")) {
			for (Path file : files) {
				Matcher m = LOG_FILE.matcher(file.getFileName().toString());
				if (m.matches()) {
					long number = Long.parseLong(m.group(1));
					if (number >= version.logNumber() || (version.prevLogNumber() != 0 && number == version.prevLogNumber())) {
						logNumbers.add(number);
					}
				}
			}
		}
		logNumbers.sort(null);
		MemTable memTable = new MemTable();
		for (long number : logNumbers) {
			memTable.replayLog(dbDir.resolve(String.format("%06d.log", number)));
		}

		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (int level = 0; level < version.numLevels(); level++) {
				sb.append(' ').append(version.files(level).size());
			}
			log.debug("Opened LevelDB {} ({}): files per level [{} ], {} log(s) with {} live memtable entries",
				dbDir, version.manifestName(), sb, logNumbers.size(), memTable.size());
		}
		return new LevelDbReader(dbDir, version, memTable, options);
	}

	public Path getDirectory() {
		return dbDir;
	}

	/**
	 * Returns the newest value stored for {@code key}, or null if the key is absent or deleted.
	 * Thread-safe.
	 */
	public byte[] get(byte[] key) throws IOException {
		Objects.requireNonNull(key, "key");
		ensureOpen();
		LookupResult r = memTable.get(key);
		if (r != null) {
			return r.value();
		}
		byte[] lookup = InternalKeys.lookupKey(key);

		// Level 0 files may overlap each other; search newest first
		for (FileMetaData f : version.files(0)) {
			if (f.mayContain(key)) {
				r = tableGet(f, lookup);
				if (r != null) {
					return r.value();
				}
			}
		}
		// Deeper levels hold disjoint, sorted files: at most one candidate per level
		for (int level = 1; level < version.numLevels(); level++) {
			List<FileMetaData> files = version.files(level);
			int lo = 0;
			int hi = files.size();
			while (lo < hi) {
				int mid = (lo + hi) >>> 1;
				if (InternalKeys.compare(files.get(mid).largest(), lookup) < 0) {
					lo = mid + 1;
				} else {
					hi = mid;
				}
			}
			if (lo < files.size()) {
				FileMetaData f = files.get(lo);
				if (InternalKeys.compareUserKey(f.smallest(), key) <= 0) {
					r = tableGet(f, lookup);
					if (r != null) {
						return r.value();
					}
				}
			}
		}
		return null;
	}

	/** Returns whether a live value exists for {@code key}. */
	public boolean contains(byte[] key) throws IOException {
		return get(key) != null;
	}

	/** Opens a cursor over all live entries in key order. The caller must close it. */
	public LevelDbCursor cursor() throws IOException {
		return cursor(null);
	}

	/**
	 * Opens a cursor over the live entries whose key starts with {@code prefix} (all entries if null or
	 * empty), in key order. The caller must close it.
	 */
	public LevelDbCursor cursor(byte[] prefix) throws IOException {
		ensureOpen();
		List<InternalIterator> sources = new ArrayList<>();
		sources.add(memTable.iterator());
		for (FileMetaData f : version.files(0)) {
			sources.add(new LevelIterator(tableCache, List.of(f), false));
		}
		for (int level = 1; level < version.numLevels(); level++) {
			if (!version.files(level).isEmpty()) {
				sources.add(new LevelIterator(tableCache, version.files(level), false));
			}
		}
		return new LevelDbCursor(new MergingIterator(sources), prefix == null ? null : prefix.clone());
	}

	/** Calls {@code action} with every live key and value, in key order. */
	public void forEach(BiConsumer<byte[], byte[]> action) throws IOException {
		forEachWithPrefix(null, action);
	}

	/** Calls {@code action} with every live key, in key order (values are not copied). */
	public void forEachKey(Consumer<byte[]> action) throws IOException {
		forEachKeyWithPrefix(null, action);
	}

	/** Calls {@code action} with every live key and value whose key starts with {@code prefix}, in key order. */
	public void forEachWithPrefix(byte[] prefix, BiConsumer<byte[], byte[]> action) throws IOException {
		try (LevelDbCursor cursor = cursor(prefix)) {
			while (cursor.next()) {
				action.accept(cursor.key(), cursor.value());
			}
		}
	}

	/** Calls {@code action} with every live key starting with {@code prefix}, in key order. */
	public void forEachKeyWithPrefix(byte[] prefix, Consumer<byte[]> action) throws IOException {
		try (LevelDbCursor cursor = cursor(prefix)) {
			while (cursor.next()) {
				action.accept(cursor.key());
			}
		}
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			tableCache.close();
			if (blockCache != null) {
				blockCache.invalidateAll();
			}
		}
	}

	// ---- package-private, for tests and diagnostics ----

	Version version() {
		return version;
	}

	MemTable memTable() {
		return memTable;
	}

	private LookupResult tableGet(FileMetaData f, byte[] lookup) throws IOException {
		for (int attempt = 1; ; attempt++) {
			Table table = tableCache.acquire(f.number());
			try {
				return table.get(lookup);
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				// Another thread was interrupted mid-read, which closes the shared channel; reopen and retry
				if (attempt >= MAX_CLOSED_CHANNEL_RETRIES || closed) {
					throw e;
				}
			} finally {
				table.release();
			}
		}
	}

	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("LevelDbReader is closed: " + dbDir);
		}
	}
}
