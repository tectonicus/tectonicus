/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import org.junit.jupiter.api.Test;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryBuilderTest {
	private static final ChunkCoord BLOCKER = new ChunkCoord(-1, -1);

	/** Builds chunks on one worker, recording the order; building {@link #BLOCKER} waits until released. */
	private static final class OneWorker implements AutoCloseable {
		final List<ChunkCoord> built = Collections.synchronizedList(new ArrayList<>());
		final CountDownLatch blockerStarted = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final GeometryBuilder builder;

		OneWorker() {
			this(coord -> new Chunk(coord, null));
		}

		OneWorker(Function<ChunkCoord, Chunk> build) {
			builder = new GeometryBuilder(1, Thread.ofPlatform().daemon().factory(), coord -> {
				if (coord.equals(BLOCKER)) {
					blockerStarted.countDown();
					try {
						release.await();
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				built.add(coord);
				return build.apply(coord);
			});
		}

		/** Keeps the only worker busy so later requests queue up. */
		void occupy() throws InterruptedException {
			builder.prefetch(BLOCKER);
			assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
		}

		/** Waits until every prefetch has been built, then for the last build to finish. */
		void drain() throws InterruptedException {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (builder.prefetchesInProgress() > 0) {
				assertTrue(System.nanoTime() < deadline, "prefetches did not finish");
				Thread.sleep(1);
			}
			builder.runExclusive(() -> {});
		}

		List<ChunkCoord> built() {
			synchronized (built) {
				return List.copyOf(built);
			}
		}

		@Override
		public void close() {
			release.countDown();
			builder.close();
		}
	}

	@Test
	void chunksADrawWaitsForAreBuiltBeforeQueuedPrefetches() throws Exception {
		try (OneWorker worker = new OneWorker()) {
			worker.occupy();
			worker.builder.prefetch(new ChunkCoord(1, 0));
			worker.builder.prefetch(new ChunkCoord(2, 0));
			CompletableFuture<Chunk> urgent = worker.builder.build(new ChunkCoord(3, 0));
			worker.builder.prefetch(new ChunkCoord(4, 0));
			worker.release.countDown();

			assertEquals(new ChunkCoord(3, 0), urgent.get(5, TimeUnit.SECONDS).getCoord());
			worker.drain();
			assertEquals(List.of(BLOCKER, new ChunkCoord(3, 0), new ChunkCoord(1, 0), new ChunkCoord(2, 0),
					new ChunkCoord(4, 0)), worker.built());
		}
	}

	@Test
	void aChunkIsPrefetchedOnceWhileQueued() throws Exception {
		try (OneWorker worker = new OneWorker()) {
			worker.occupy();
			worker.builder.prefetch(new ChunkCoord(1, 0));
			worker.builder.prefetch(new ChunkCoord(1, 0));
			assertEquals(2, worker.builder.prefetchesInProgress());
			worker.release.countDown();
			worker.drain();
			assertEquals(1, Collections.frequency(worker.built(), new ChunkCoord(1, 0)));
			assertEquals(0, worker.builder.prefetchesInProgress());
		}
	}

	@Test
	void cancellingDropsQueuedPrefetchesButNotWaitingBuilds() throws Exception {
		try (OneWorker worker = new OneWorker()) {
			worker.occupy();
			worker.builder.prefetch(new ChunkCoord(1, 0));
			CompletableFuture<Chunk> urgent = worker.builder.build(new ChunkCoord(2, 0));
			worker.builder.cancelPrefetches();
			assertEquals(1, worker.builder.prefetchesInProgress(), "only the running prefetch is left");
			worker.release.countDown();

			urgent.get(5, TimeUnit.SECONDS);
			worker.drain();
			assertFalse(worker.built().contains(new ChunkCoord(1, 0)));

			// A cancelled chunk can be prefetched again later.
			worker.builder.prefetch(new ChunkCoord(1, 0));
			worker.drain();
			assertTrue(worker.built().contains(new ChunkCoord(1, 0)));
		}
	}

	@Test
	void exclusiveActionsWaitForRunningBuilds() throws Exception {
		try (OneWorker worker = new OneWorker()) {
			worker.occupy();
			assertFalse(worker.builder.tryRunExclusive(() -> {}));

			AtomicBoolean ran = new AtomicBoolean();
			Thread waiting = Thread.ofPlatform().daemon().start(() -> worker.builder.runExclusive(() -> ran.set(true)));
			waiting.join(100);
			assertFalse(ran.get());

			worker.release.countDown();
			waiting.join(5000);
			assertTrue(ran.get());
			worker.drain();
			assertTrue(worker.builder.tryRunExclusive(() -> {}));
		}
	}

	@Test
	void buildFailuresReachTheWaitingDraw() throws Exception {
		try (OneWorker worker = new OneWorker(coord -> {
			if (coord.x == 5)
				throw new IllegalStateException("unreadable chunk");
			return new Chunk(coord, null);
		})) {
			CompletionException e = assertThrows(CompletionException.class, () -> worker.builder.build(new ChunkCoord(5, 0)).join());
			assertInstanceOf(IllegalStateException.class, e.getCause());

			worker.builder.prefetch(new ChunkCoord(5, 0));
			worker.drain();
			assertEquals(0, worker.builder.prefetchesInProgress());
		}
	}
}
