/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tectonicus.bedrock.leveldb.TestDbWriter.put;

class TableCacheTest {
	@TempDir
	Path dir;

	@Test
	void reusesOpenTablesAndClosesEvictedOnesAfterRelease() throws IOException {
		for (long n = 1; n <= 3; n++) {
			TestDbWriter.writeTable(dir, 1, n, List.of(put("k" + n, 1, "v")), Compression.NONE, 4096);
		}
		TableCache cache = new TableCache(dir, 1, true, null);
		Table first = cache.acquire(1);
		first.release();
		Table again = cache.acquire(1);
		assertSame(first, again, "a cached table is reused");
		assertTrue(again.isOpen());

		// Still referenced while other tables push it out of the cache
		for (int i = 0; i < 20; i++) {
			cache.acquire(2 + i % 2).release();
		}
		assertTrue(again.isOpen(), "an evicted table stays open while referenced");
		again.release();
		assertFalse(again.isOpen(), "an evicted table closes on its last release");
		Table reopened = cache.acquire(1);
		assertNotSame(first, reopened);
		reopened.release();

		cache.close();
		assertFalse(reopened.isOpen());
	}
}
