/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

/** The newest entry for a user key found in one source: either a value or a deletion tombstone. */
record LookupResult(byte[] value) {
	static final LookupResult DELETED = new LookupResult(null);

	boolean isDeleted() {
		return value == null;
	}
}
