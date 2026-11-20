/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.io.IOException;
import java.util.Arrays;

/**
 * Forward cursor over the live entries of a {@link LevelDbReader} in ascending (unsigned bytewise) key
 * order. Each user key appears once with its newest value; deleted keys are skipped.
 * <p>
 * Usage: {@code while (cursor.next()) { cursor.key(); cursor.value(); }}. A cursor is not thread-safe
 * and must be closed (it holds table files open).
 */
public final class LevelDbCursor implements AutoCloseable {
	private final InternalIterator merged;
	private final byte[] prefix;
	private boolean started;
	private boolean finished;
	/** Internal key of the newest entry of the last user key seen (used to skip older versions). */
	private byte[] lastInternalKey;
	private byte[] currentKey;

	LevelDbCursor(InternalIterator merged, byte[] prefix) {
		this.merged = merged;
		this.prefix = prefix;
	}

	/**
	 * Advances to the next live entry.
	 *
	 * @return false when there are no more entries (with the cursor's prefix, if any)
	 */
	public boolean next() throws IOException {
		if (finished) {
			return false;
		}
		if (!started) {
			started = true;
			if (prefix == null || prefix.length == 0) {
				merged.seekToFirst();
			} else {
				merged.seek(InternalKeys.lookupKey(prefix));
			}
		} else if (merged.isValid()) {
			merged.next();
		}
		currentKey = null;
		while (merged.isValid()) {
			byte[] ik = merged.key();
			if (prefix != null && !hasPrefix(ik)) {
				break;
			}
			if (lastInternalKey != null && InternalKeys.sameUserKey(ik, lastInternalKey)) {
				merged.next();
				continue;
			}
			lastInternalKey = ik;
			int type = InternalKeys.type(ik);
			if (type == InternalKeys.TYPE_VALUE) {
				currentKey = InternalKeys.userKey(ik);
				return true;
			} else if (type != InternalKeys.TYPE_DELETION) {
				throw new LevelDbException("Unknown internal key type " + type);
			}
			merged.next();
		}
		finished = true;
		close();
		return false;
	}

	/** The current entry's key (a fresh array owned by the caller). */
	public byte[] key() {
		if (currentKey == null) {
			throw new IllegalStateException("Cursor is not positioned on an entry");
		}
		return currentKey.clone();
	}

	/** The current entry's value (a fresh array owned by the caller). */
	public byte[] value() throws IOException {
		if (currentKey == null) {
			throw new IllegalStateException("Cursor is not positioned on an entry");
		}
		return merged.value();
	}

	private boolean hasPrefix(byte[] internalKey) {
		int userLen = internalKey.length - 8;
		return userLen >= prefix.length && Arrays.equals(internalKey, 0, prefix.length, prefix, 0, prefix.length);
	}

	@Override
	public void close() {
		merged.close();
	}
}
