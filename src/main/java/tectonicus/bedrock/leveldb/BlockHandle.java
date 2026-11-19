/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

/** Location of a block within a table file: {@code varint64 offset, varint64 size} (size excludes the trailer). */
record BlockHandle(long offset, long size) {
	static BlockHandle decode(SliceInput in) throws LevelDbException {
		return new BlockHandle(in.readVarint64(), in.readVarint64());
	}

	static BlockHandle decode(byte[] encoded) throws LevelDbException {
		return decode(new SliceInput(encoded));
	}
}
