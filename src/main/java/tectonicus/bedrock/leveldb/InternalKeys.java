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
 * Helpers for LevelDB internal keys: {@code userKey + fixed64le(sequence << 8 | type)}.
 * Ordering is user key ascending (unsigned bytewise), then sequence descending, then type descending.
 */
final class InternalKeys {
	static final int TYPE_DELETION = 0;
	static final int TYPE_VALUE = 1;
	static final long MAX_SEQUENCE = (1L << 56) - 1;
	/** Tag used for lookups: sorts before every real entry of the same user key. */
	static final long LOOKUP_TAG = (MAX_SEQUENCE << 8) | TYPE_VALUE;

	private InternalKeys() {
	}

	static byte[] make(byte[] userKey, long tag) {
		byte[] k = Arrays.copyOf(userKey, userKey.length + 8);
		putFixed64(k, userKey.length, tag);
		return k;
	}

	static byte[] lookupKey(byte[] userKey) {
		return make(userKey, LOOKUP_TAG);
	}

	static int userKeyLength(byte[] internalKey) {
		return internalKey.length - 8;
	}

	static byte[] userKey(byte[] internalKey) {
		return Arrays.copyOf(internalKey, internalKey.length - 8);
	}

	static long tag(byte[] internalKey) {
		return SliceInput.getFixed64(internalKey, internalKey.length - 8);
	}

	static int type(byte[] internalKey) {
		return internalKey[internalKey.length - 8] & 0xff;
	}

	static void checkValid(byte[] internalKey) throws LevelDbException {
		if (internalKey.length < 8) {
			throw new LevelDbException("Internal key too short: " + internalKey.length + " bytes");
		}
	}

	static int compareUserKeys(byte[] a, int aLen, byte[] b, int bLen) {
		return Arrays.compareUnsigned(a, 0, aLen, b, 0, bLen);
	}

	/** Compares the user key part of {@code internalKey} with {@code userKey}. */
	static int compareUserKey(byte[] internalKey, byte[] userKey) {
		return Arrays.compareUnsigned(internalKey, 0, internalKey.length - 8, userKey, 0, userKey.length);
	}

	static boolean sameUserKey(byte[] a, byte[] b) {
		return Arrays.equals(a, 0, a.length - 8, b, 0, b.length - 8);
	}

	static int compare(byte[] a, byte[] b) {
		int c = Arrays.compareUnsigned(a, 0, a.length - 8, b, 0, b.length - 8);
		if (c != 0) {
			return c;
		}
		// Higher tag (newer sequence) sorts first
		return Long.compareUnsigned(tag(b), tag(a));
	}

	static void putFixed64(byte[] b, int off, long v) {
		for (int i = 0; i < 8; i++) {
			b[off + i] = (byte) (v >>> (8 * i));
		}
	}
}
