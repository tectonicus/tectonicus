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

/**
 * Forward iterator over entries sorted by internal key ({@link InternalKeys#compare}).
 * Not thread-safe; each instance is used by a single thread.
 */
interface InternalIterator extends AutoCloseable {
	boolean isValid();

	void seekToFirst() throws IOException;

	/** Positions at the first entry whose key is {@code >= target}. */
	void seek(byte[] target) throws IOException;

	void next() throws IOException;

	/** Current key; the returned array must not be modified and stays valid after {@link #next()}. */
	byte[] key();

	/** A fresh copy of the current value. */
	byte[] value() throws IOException;

	@Override
	void close();
}
