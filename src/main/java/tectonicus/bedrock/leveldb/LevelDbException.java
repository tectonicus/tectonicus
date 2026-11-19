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
 * Thrown when a LevelDB database is malformed, corrupt or uses a feature this reader does not support.
 */
public class LevelDbException extends IOException {
	public LevelDbException(String message) {
		super(message);
	}

	public LevelDbException(String message, Throwable cause) {
		super(message, cause);
	}
}
