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

/**
 * Reader for the LevelDB log format used by both write-ahead logs ({@code *.log}) and MANIFEST files.
 * <p>
 * The file is a sequence of 32 KiB blocks. Each physical record has a 7-byte header
 * ({@code fixed32 maskedCrc32c, fixed16 length, byte type}) followed by the payload; a logical record is
 * either one FULL fragment or FIRST, MIDDLE*, LAST. A block tail shorter than a header is zero padding.
 * <p>
 * An incomplete record at the very end of the file (the writer died mid-write) is treated as end of file
 * and reported via {@link #hasTruncatedTail()}. Any other inconsistency throws {@link LevelDbException}.
 */
final class LogReader {
	static final int BLOCK_SIZE = 32768;
	static final int HEADER_SIZE = 7;

	static final int TYPE_ZERO = 0;
	static final int TYPE_FULL = 1;
	static final int TYPE_FIRST = 2;
	static final int TYPE_MIDDLE = 3;
	static final int TYPE_LAST = 4;

	private final byte[] data;
	private final String name;
	private int pos;
	private boolean truncatedTail;
	private boolean eof;

	LogReader(byte[] data, String name) {
		this.data = data;
		this.name = name;
	}

	boolean hasTruncatedTail() {
		return truncatedTail;
	}

	/** File offset just past the last record returned. */
	int position() {
		return pos;
	}

	/** Returns the next logical record, or null at end of file. */
	byte[] nextRecord() throws LevelDbException {
		ByteArrayOutputStream scratch = null;
		int recordStart = pos;
		while (true) {
			int fragmentStart = pos;
			int type = readPhysical();
			if (type < 0) {
				if (scratch != null) {
					// FIRST/MIDDLE without LAST at end of file: the writer died mid-record
					truncatedTail = true;
					pos = recordStart;
				}
				return null;
			}
			int length = SliceInput.getFixed32(data, fragmentStart + 4) & 0xffff;
			int payload = fragmentStart + HEADER_SIZE;
			switch (type) {
				case TYPE_FULL -> {
					if (scratch != null) {
						throw corruption("FULL record inside fragmented record", fragmentStart);
					}
					byte[] out = new byte[length];
					System.arraycopy(data, payload, out, 0, length);
					return out;
				}
				case TYPE_FIRST -> {
					if (scratch != null) {
						throw corruption("FIRST record inside fragmented record", fragmentStart);
					}
					scratch = new ByteArrayOutputStream(length * 2);
					scratch.write(data, payload, length);
				}
				case TYPE_MIDDLE, TYPE_LAST -> {
					if (scratch == null) {
						throw corruption((type == TYPE_MIDDLE ? "MIDDLE" : "LAST") + " record without FIRST", fragmentStart);
					}
					scratch.write(data, payload, length);
					if (type == TYPE_LAST) {
						return scratch.toByteArray();
					}
				}
				default -> throw corruption("unknown record type " + type, fragmentStart);
			}
		}
	}

	/**
	 * Reads one physical record header and validates it, leaving {@code pos} after its payload.
	 * Returns the record type, or -1 at end of file.
	 */
	private int readPhysical() throws LevelDbException {
		while (true) {
			if (eof) {
				return -1;
			}
			int blockRemaining = BLOCK_SIZE - (pos % BLOCK_SIZE);
			if (blockRemaining < HEADER_SIZE) {
				// Trailer padding
				pos += blockRemaining;
				continue;
			}
			if (pos >= data.length) {
				eof = true;
				return -1;
			}
			if (data.length - pos < HEADER_SIZE) {
				truncatedTail = true;
				eof = true;
				return -1;
			}
			int maskedCrc = SliceInput.getFixed32(data, pos);
			int length = (data[pos + 4] & 0xff) | (data[pos + 5] & 0xff) << 8;
			int type = data[pos + 6] & 0xff;
			if (HEADER_SIZE + length > blockRemaining) {
				if (pos + HEADER_SIZE + length > data.length) {
					truncatedTail = true;
					eof = true;
					return -1;
				}
				throw corruption("bad record length " + length, pos);
			}
			if (pos + HEADER_SIZE + length > data.length) {
				truncatedTail = true;
				eof = true;
				return -1;
			}
			if (type == TYPE_ZERO && length == 0) {
				// Zero-filled (preallocated) region: skip the rest of this block
				pos += blockRemaining;
				continue;
			}
			int expected = Crc32c.unmask(maskedCrc);
			int actual = Crc32c.value(type, data, pos + HEADER_SIZE, length);
			if (expected != actual) {
				throw corruption("checksum mismatch", pos);
			}
			pos += HEADER_SIZE + length;
			return type;
		}
	}

	private LevelDbException corruption(String message, int offset) {
		return new LevelDbException("Corrupt log record in " + name + " at offset " + offset + ": " + message);
	}
}
