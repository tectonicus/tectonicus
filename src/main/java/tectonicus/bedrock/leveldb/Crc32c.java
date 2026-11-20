/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.util.zip.CRC32C;

/** LevelDB's masked CRC32C as stored in log record headers and block trailers. */
final class Crc32c {
	private static final int MASK_DELTA = 0xa282ead8;

	private Crc32c() {
	}

	/** CRC32C of {@code type} followed by {@code data[off, off+len)}. */
	static int value(int type, byte[] data, int off, int len) {
		CRC32C crc = new CRC32C();
		crc.update(type);
		crc.update(data, off, len);
		return (int) crc.getValue();
	}

	static int value(byte[] data, int off, int len) {
		CRC32C crc = new CRC32C();
		crc.update(data, off, len);
		return (int) crc.getValue();
	}

	static int mask(int crc) {
		return ((crc >>> 15) | (crc << 17)) + MASK_DELTA;
	}

	static int unmask(int masked) {
		int rot = masked - MASK_DELTA;
		return (rot >>> 17) | (rot << 15);
	}
}
