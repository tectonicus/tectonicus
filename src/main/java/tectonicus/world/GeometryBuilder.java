/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.extern.slf4j.Slf4j;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Builds chunk geometry on worker threads. Chunks a draw is waiting for are built first; prefetched chunks,
 * which a later draw will need, are built only when no worker has a waiting chunk to build.
 * <p>
 * Builds read the raw and geometry caches, so evicted chunks must only be unloaded while no build is running:
 * do that through {@link #tryRunExclusive} or {@link #runExclusive}.
 */
@Slf4j
final class GeometryBuilder implements AutoCloseable
{
	private final Function<ChunkCoord, Chunk> build;
	private final ThreadPoolExecutor executor;
	/** Held shared by every build, and exclusively while evicted chunks are unloaded. */
	private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
	private final Set<ChunkCoord> prefetching = ConcurrentHashMap.newKeySet();
	private final AtomicLong order = new AtomicLong();

	/** @param build returns the chunk with its geometry built, from the geometry cache */
	GeometryBuilder(int workers, ThreadFactory threadFactory, Function<ChunkCoord, Chunk> build)
	{
		this.build = build;
		this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
				new PriorityBlockingQueue<>(), threadFactory);
	}

	/** Builds a chunk a draw is waiting for, ahead of any prefetched chunk not yet started. */
	CompletableFuture<Chunk> build(ChunkCoord coord)
	{
		CompletableFuture<Chunk> future = new CompletableFuture<>();
		executor.execute(new Task(true, order.getAndIncrement(), coord, () -> {
			try {
				future.complete(buildShared(coord));
			} catch (RuntimeException | Error e) {
				future.completeExceptionally(e);
				if (e instanceof Error error)
					throw error;
			}
		}));
		return future;
	}

	/** Starts building a chunk a later draw will need, unless it is already queued or being built. */
	void prefetch(ChunkCoord coord)
	{
		if (!prefetching.add(coord))
			return;
		executor.execute(new Task(false, order.getAndIncrement(), coord, () -> {
			try {
				buildShared(coord);
			} catch (RuntimeException e) {
				// A draw that needs this chunk builds it again and reports the failure.
				log.debug("Prefetching geometry for chunk {},{} failed", coord.x, coord.z, e);
			} finally {
				prefetching.remove(coord);
			}
		}));
	}

	/** Drops prefetched chunks that have not started building. Builds already running are left to finish. */
	void cancelPrefetches()
	{
		List<Task> cancelled = new ArrayList<>();
		executor.getQueue().removeIf(r -> {
			Task task = (Task) r;
			if (task.urgent)
				return false;
			cancelled.add(task);
			return true;
		});
		for (Task task : cancelled)
			prefetching.remove(task.coord);
	}

	/** Runs the action if no build is running, keeping new builds from starting until it is done. */
	boolean tryRunExclusive(Runnable action)
	{
		Lock lock = cacheLock.writeLock();
		if (!lock.tryLock())
			return false;
		try {
			action.run();
		} finally {
			lock.unlock();
		}
		return true;
	}

	/** Waits for running builds to finish, then runs the action before any new build starts. */
	void runExclusive(Runnable action)
	{
		Lock lock = cacheLock.writeLock();
		lock.lock();
		try {
			action.run();
		} finally {
			lock.unlock();
		}
	}

	/** Number of prefetched chunks queued or being built. */
	int prefetchesInProgress()
	{
		return prefetching.size();
	}

	@Override
	public void close()
	{
		cancelPrefetches();
		executor.shutdown();
	}

	private Chunk buildShared(ChunkCoord coord)
	{
		Lock lock = cacheLock.readLock();
		lock.lock();
		try {
			return build.apply(coord);
		} finally {
			lock.unlock();
		}
	}

	private record Task(boolean urgent, long order, ChunkCoord coord, Runnable work) implements Runnable, Comparable<Task>
	{
		@Override
		public void run()
		{
			work.run();
		}

		@Override
		public int compareTo(Task other)
		{
			if (urgent != other.urgent)
				return urgent ? -1 : 1;
			return Long.compare(order, other.order);
		}
	}
}
