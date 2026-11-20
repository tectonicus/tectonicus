/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.util.Arrays;

/**
 * Sequential reader over a byte array range with the LevelDB primitive encodings
 * (little-endian fixed-width integers, base-128 varints and length-prefixed slices).
 * All reads are bounds checked and throw {@link LevelDbException} on truncation.
 */
final class SliceInput {
	private final byte[] data;
	private final int limit;
	private int pos;

	SliceInput(byte[] data) {
		this(data, 0, data.length);
	}

	SliceInput(byte[] data, int offset, int length) {
		this.data = data;
		this.pos = offset;
		this.limit = offset + length;
	}

	int position() {
		return pos;
	}

	int remaining() {
		return limit - pos;
	}

	boolean hasRemaining() {
		return pos < limit;
	}

	private void require(int n) throws LevelDbException {
		if (n < 0 || n > limit - pos) {
			throw new LevelDbException("Truncated data: need " + n + " bytes at offset " + pos + ", " + (limit - pos) + " available");
		}
	}

	int readByte() throws LevelDbException {
		require(1);
		return data[pos++] & 0xff;
	}

	int readFixed32() throws LevelDbException {
		require(4);
		int v = getFixed32(data, pos);
		pos += 4;
		return v;
	}

	long readFixed64() throws LevelDbException {
		require(8);
		long v = getFixed64(data, pos);
		pos += 8;
		return v;
	}

	int readVarint32() throws LevelDbException {
		long v = readVarint64();
		if ((v >>> 32) != 0) {
			throw new LevelDbException("varint32 out of range at offset " + pos);
		}
		return (int) v;
	}

	long readVarint64() throws LevelDbException {
		long result = 0;
		for (int shift = 0; shift <= 63; shift += 7) {
			int b = readByte();
			result |= (long) (b & 0x7f) << shift;
			if ((b & 0x80) == 0) {
				return result;
			}
		}
		throw new LevelDbException("Malformed varint at offset " + pos);
	}

	/** Reads a varint32 length that must be non-negative and fit in the remaining input. */
	int readLength() throws LevelDbException {
		int len = readVarint32();
		require(len);
		return len;
	}

	byte[] readBytes(int n) throws LevelDbException {
		require(n);
		byte[] out = Arrays.copyOfRange(data, pos, pos + n);
		pos += n;
		return out;
	}

	byte[] readLengthPrefixed() throws LevelDbException {
		return readBytes(readLength());
	}

	void skip(int n) throws LevelDbException {
		require(n);
		pos += n;
	}

	static int getFixed32(byte[] b, int off) {
		return (b[off] & 0xff)
			| (b[off + 1] & 0xff) << 8
			| (b[off + 2] & 0xff) << 16
			| (b[off + 3] & 0xff) << 24;
	}

	static long getFixed64(byte[] b, int off) {
		return (getFixed32(b, off) & 0xffffffffL) | ((long) getFixed32(b, off + 4) << 32);
	}
}
