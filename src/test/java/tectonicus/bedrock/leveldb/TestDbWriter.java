/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;

/**
 * Minimal LevelDB writer for tests: builds sstables (with any compression type, including Mojang's zlib
 * variants that real fixture generators can't produce), MANIFEST files and write-ahead logs.
 */
final class TestDbWriter {
	private TestDbWriter() {
	}

	record Entry(byte[] userKey, long sequence, boolean deletion, byte[] value) {
		byte[] internalKey() {
			return InternalKeys.make(userKey, (sequence << 8) | (deletion ? 0 : 1));
		}
	}

	record TableFile(int level, long number, long size, byte[] smallest, byte[] largest) {
	}

	static Entry put(String key, long seq, String value) {
		return new Entry(bytes(key), seq, false, bytes(value));
	}

	static Entry del(String key, long seq) {
		return new Entry(bytes(key), seq, true, new byte[0]);
	}

	static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.ISO_8859_1);
	}

	// ---------------------------------------------------------------- sstables

	/** Writes a table containing {@code entries} (sorted here) and returns its metadata. */
	static TableFile writeTable(Path dir, int level, long number, List<Entry> entries, int compression, int blockSize) throws IOException {
		List<Entry> sorted = new ArrayList<>(entries);
		sorted.sort((a, b) -> InternalKeys.compare(a.internalKey(), b.internalKey()));

		ByteArrayOutputStream file = new ByteArrayOutputStream();
		BlockBuilder data = new BlockBuilder(16);
		BlockBuilder index = new BlockBuilder(1);
		byte[] lastKey = null;
		for (Entry e : sorted) {
			byte[] ik = e.internalKey();
			data.add(ik, e.deletion() ? new byte[0] : e.value());
			lastKey = ik;
			if (data.estimatedSize() >= blockSize) {
				index.add(lastKey, writeBlock(file, data.finish(), compression));
				data = new BlockBuilder(16);
			}
		}
		if (!data.isEmpty()) {
			index.add(lastKey, writeBlock(file, data.finish(), compression));
		}
		byte[] metaindexHandle = writeBlock(file, new BlockBuilder(1).finish(), Compression.NONE);
		byte[] indexHandle = writeBlock(file, index.finish(), compression);

		byte[] footer = new byte[Table.FOOTER_SIZE];
		System.arraycopy(metaindexHandle, 0, footer, 0, metaindexHandle.length);
		System.arraycopy(indexHandle, 0, footer, metaindexHandle.length, indexHandle.length);
		InternalKeys.putFixed64(footer, Table.FOOTER_SIZE - 8, Table.MAGIC);
		file.write(footer);

		byte[] bytes = file.toByteArray();
		Files.write(dir.resolve(String.format("%06d.ldb", number)), bytes);
		return new TableFile(level, number, bytes.length, sorted.getFirst().internalKey(), sorted.getLast().internalKey());
	}

	private static byte[] writeBlock(ByteArrayOutputStream file, byte[] contents, int compression) {
		byte[] stored = compress(contents, compression);
		long offset = file.size();
		file.writeBytes(stored);
		byte[] typeAndCrc = new byte[5];
		typeAndCrc[0] = (byte) compression;
		byte[] withType = Arrays.copyOf(stored, stored.length + 1);
		withType[stored.length] = (byte) compression;
		putFixed32(typeAndCrc, 1, Crc32c.mask(Crc32c.value(withType, 0, withType.length)));
		file.writeBytes(typeAndCrc);
		ByteArrayOutputStream handle = new ByteArrayOutputStream();
		putVarint(handle, offset);
		putVarint(handle, stored.length);
		return handle.toByteArray();
	}

	static byte[] compress(byte[] data, int compression) {
		return switch (compression) {
			case Compression.NONE -> data;
			case Compression.ZLIB -> deflate(data, false);
			case Compression.ZLIB_RAW -> deflate(data, true);
			default -> throw new IllegalArgumentException("compression " + compression);
		};
	}

	private static byte[] deflate(byte[] data, boolean nowrap) {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap);
		deflater.setInput(data);
		deflater.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		while (!deflater.finished()) {
			int n = deflater.deflate(buf);
			out.write(buf, 0, n);
		}
		deflater.end();
		return out.toByteArray();
	}

	/** Block builder with prefix compression and restart points, matching LevelDB's BlockBuilder. */
	static final class BlockBuilder {
		private final int restartInterval;
		private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
		private final List<Integer> restarts = new ArrayList<>(List.of(0));
		private byte[] lastKey = new byte[0];
		private int counter;

		BlockBuilder(int restartInterval) {
			this.restartInterval = restartInterval;
		}

		boolean isEmpty() {
			return buf.size() == 0;
		}

		int estimatedSize() {
			return buf.size() + restarts.size() * 4 + 4;
		}

		void add(byte[] key, byte[] value) {
			int shared = 0;
			if (counter < restartInterval) {
				int max = Math.min(lastKey.length, key.length);
				while (shared < max && lastKey[shared] == key[shared]) {
					shared++;
				}
			} else {
				restarts.add(buf.size());
				counter = 0;
			}
			putVarint(buf, shared);
			putVarint(buf, key.length - shared);
			putVarint(buf, value.length);
			buf.write(key, shared, key.length - shared);
			buf.writeBytes(value);
			lastKey = key;
			counter++;
		}

		byte[] finish() {
			for (int r : restarts) {
				byte[] b = new byte[4];
				putFixed32(b, 0, r);
				buf.writeBytes(b);
			}
			byte[] b = new byte[4];
			putFixed32(b, 0, restarts.size());
			buf.writeBytes(b);
			return buf.toByteArray();
		}
	}

	// ---------------------------------------------------------------- logs and manifests

	/** Writes records in the LevelDB log format (32 KiB blocks, fragmenting as needed). */
	static byte[] logFile(List<byte[]> records) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] record : records) {
			int pos = 0;
			boolean begin = true;
			do {
				int leftover = LogReader.BLOCK_SIZE - out.size() % LogReader.BLOCK_SIZE;
				if (leftover < LogReader.HEADER_SIZE) {
					out.writeBytes(new byte[leftover]);
					leftover = LogReader.BLOCK_SIZE;
				}
				int avail = leftover - LogReader.HEADER_SIZE;
				int n = Math.min(avail, record.length - pos);
				boolean end = pos + n == record.length;
				int type = begin && end ? LogReader.TYPE_FULL
					: begin ? LogReader.TYPE_FIRST
					: end ? LogReader.TYPE_LAST
					: LogReader.TYPE_MIDDLE;
				byte[] header = new byte[LogReader.HEADER_SIZE];
				putFixed32(header, 0, Crc32c.mask(Crc32c.value(type, record, pos, n)));
				header[4] = (byte) n;
				header[5] = (byte) (n >>> 8);
				header[6] = (byte) type;
				out.writeBytes(header);
				out.write(record, pos, n);
				pos += n;
				begin = false;
			} while (pos < record.length);
		}
		return out.toByteArray();
	}

	/** Encodes a WriteBatch: sequence, count, then put/delete records. */
	static byte[] writeBatch(long sequence, List<Entry> ops) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] header = new byte[12];
		InternalKeys.putFixed64(header, 0, sequence);
		putFixed32(header, 8, ops.size());
		out.writeBytes(header);
		for (Entry e : ops) {
			out.write(e.deletion() ? 0 : 1);
			putLengthPrefixed(out, e.userKey());
			if (!e.deletion()) {
				putLengthPrefixed(out, e.value());
			}
		}
		return out.toByteArray();
	}

	/** A single VersionEdit describing the given files. */
	static byte[] versionEdit(long logNumber, long lastSequence, long nextFile, List<TableFile> files) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		putVarint(out, 1);
		putLengthPrefixed(out, bytes(Version.BYTEWISE_COMPARATOR));
		putVarint(out, 2);
		putVarint(out, logNumber);
		putVarint(out, 3);
		putVarint(out, nextFile);
		putVarint(out, 4);
		putVarint(out, lastSequence);
		for (TableFile f : files) {
			putVarint(out, 7);
			putVarint(out, f.level());
			putVarint(out, f.number());
			putVarint(out, f.size());
			putLengthPrefixed(out, f.smallest());
			putLengthPrefixed(out, f.largest());
		}
		return out.toByteArray();
	}

	static void writeManifest(Path dir, long number, List<byte[]> edits) throws IOException {
		String name = String.format("MANIFEST-%06d", number);
		Files.write(dir.resolve(name), logFile(edits));
		Files.writeString(dir.resolve("CURRENT"), name + "\n");
	}

	// ---------------------------------------------------------------- encoding helpers

	static void putFixed32(byte[] b, int off, int v) {
		b[off] = (byte) v;
		b[off + 1] = (byte) (v >>> 8);
		b[off + 2] = (byte) (v >>> 16);
		b[off + 3] = (byte) (v >>> 24);
	}

	static void putVarint(ByteArrayOutputStream out, long v) {
		while ((v & ~0x7fL) != 0) {
			out.write((int) ((v & 0x7f) | 0x80));
			v >>>= 7;
		}
		out.write((int) v);
	}

	static void putLengthPrefixed(ByteArrayOutputStream out, byte[] b) {
		putVarint(out, b.length);
		out.writeBytes(b);
	}
}
