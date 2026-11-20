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

/** Immutable byte array wrapper with value equality and unsigned bytewise ordering. */
final class ByteKey implements Comparable<ByteKey> {
	private final byte[] bytes;
	private final int hash;

	ByteKey(byte[] bytes) {
		this.bytes = bytes;
		this.hash = Arrays.hashCode(bytes);
	}

	byte[] bytes() {
		return bytes;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof ByteKey other && hash == other.hash && Arrays.equals(bytes, other.bytes));
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public int compareTo(ByteKey o) {
		return Arrays.compareUnsigned(bytes, o.bytes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
		}
		return sb.toString();
	}
}
