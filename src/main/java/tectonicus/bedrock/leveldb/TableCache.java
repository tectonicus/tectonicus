/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Bounded cache of open {@link Table}s keyed by file number, so that databases with thousands of table
 * files don't exhaust file descriptors. Tables are reference counted: callers must {@link Table#release()}
 * every table returned by {@link #acquire(long)}.
 */
final class TableCache implements AutoCloseable {
	private final Path dbDir;
	private final boolean verifyChecksums;
	private final Cache<Table.BlockCacheKey, Block> blockCache;
	private final Cache<Long, Table> tables;

	TableCache(Path dbDir, int maxOpenTables, boolean verifyChecksums, Cache<Table.BlockCacheKey, Block> blockCache) {
		this.dbDir = dbDir;
		this.verifyChecksums = verifyChecksums;
		this.blockCache = blockCache;
		this.tables = Caffeine.newBuilder()
			.maximumSize(maxOpenTables)
			.executor(Runnable::run)
			.<Long, Table>removalListener((number, table, cause) -> {
				if (table != null) {
					table.evict();
				}
			})
			.build();
	}

	static Path tablePath(Path dbDir, long number) {
		Path ldb = dbDir.resolve(String.format("%06d.ldb", number));
		if (Files.exists(ldb)) {
			return ldb;
		}
		Path sst = dbDir.resolve(String.format("%06d.sst", number));
		return Files.exists(sst) ? sst : ldb;
	}

	Table acquire(long number) throws IOException {
		try {
			// Acquire inside compute so that the reference is taken while the table is still mapped; if it is
			// evicted afterwards (even immediately, by the admission policy) it stays open until released.
			return tables.asMap().compute(number, (n, existing) -> {
				if (existing != null && existing.isOpen() && existing.tryAcquire()) {
					return existing;
				}
				// Absent, or its channel was closed underneath us (a reading thread was interrupted): (re)open.
				// A replaced table is passed to the removal listener and closes once its users release it.
				try {
					Table table = Table.open(n, tablePath(dbDir, n), verifyChecksums, blockCache);
					table.tryAcquire();
					return table;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	@Override
	public void close() {
		tables.invalidateAll();
		tables.cleanUp();
	}
}
