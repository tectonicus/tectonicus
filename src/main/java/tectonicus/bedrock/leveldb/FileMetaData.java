/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

/** A table file in the current version: its number, size and smallest/largest internal keys. */
record FileMetaData(long number, long fileSize, byte[] smallest, byte[] largest) {
	/** Whether {@code userKey} lies within this file's user key range. */
	boolean mayContain(byte[] userKey) {
		return InternalKeys.compareUserKey(smallest, userKey) <= 0 && InternalKeys.compareUserKey(largest, userKey) >= 0;
	}
}
