/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedExecutorTest {
	@Test
	void countsThrownAndRecordedFailuresAndRunsEveryTask() {
		BoundedExecutor executor = new BoundedExecutor("test", 2, 1);
		AtomicInteger ran = new AtomicInteger();
		for (int i = 0; i < 20; i++) {
			final int n = i;
			executor.execute("task " + n, () -> {
				ran.incrementAndGet();
				if (n % 5 == 0)
					throw new IllegalStateException("boom " + n);
				if (n == 7)
					executor.recordFailure();
			});
		}

		assertEquals(5, executor.awaitCompletion());
		assertEquals(20, ran.get());
	}

	@Test
	void awaitIdleDrainsTasksAndKeepsTheExecutorUsable() {
		BoundedExecutor executor = new BoundedExecutor("test", 2, 1);
		AtomicInteger ran = new AtomicInteger();
		for (int round = 0; round < 3; round++) {
			for (int i = 0; i < 10; i++) {
				final int n = i;
				executor.execute("task " + n, () -> {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					ran.incrementAndGet();
					if (n == 3)
						throw new IllegalStateException("boom " + n);
				});
			}

			// Every task of this round has finished, and only this round's failure is reported.
			assertEquals(1, executor.awaitIdle());
			assertEquals(10 * (round + 1), ran.get());
		}

		assertEquals(0, executor.awaitIdle());
		assertEquals(0, executor.awaitCompletion());
	}

	@Test
	void awaitCompletionReportsFailuresSinceTheLastAwaitIdle() {
		BoundedExecutor executor = new BoundedExecutor("test", 1, 0);
		executor.execute("first", () -> { throw new IllegalStateException("first"); });
		assertEquals(1, executor.awaitIdle());

		executor.execute("second", executor::recordFailure);
		assertEquals(1, executor.awaitCompletion());
	}

	@Test
	void mapTempAreasAreRemovedWithTheirFiles(@TempDir Path dir) throws Exception {
		TempArea root = new TempArea(dir.resolve("temp").toFile());
		TempArea map = root.subArea("world/overworld");
		File file = map.generateTempFile("signs", ".list");
		assertTrue(file.createNewFile());
		assertTrue(file.getParentFile().getName().equals("world_overworld"));

		map.delete();

		assertFalse(file.getParentFile().exists());
		assertTrue(dir.resolve("temp").toFile().isDirectory());
	}
}
