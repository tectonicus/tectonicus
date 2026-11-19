/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed thread pool that makes the submitting thread wait while too many tasks are pending, and that counts and logs
 * tasks that fail instead of losing their exceptions in an unread Future.
 */
@Slf4j
public class BoundedExecutor {
	private final ThreadPoolExecutor executor;
	private final Semaphore capacity;
	private final AtomicInteger failures = new AtomicInteger();

	/**
	 * @param name        thread name prefix, also used in log messages
	 * @param numThreads  worker threads
	 * @param maxQueued   tasks that may wait for a worker before {@link #execute} blocks
	 */
	public BoundedExecutor(String name, int numThreads, int maxQueued) {
		int threads = Math.max(1, numThreads);
		capacity = new Semaphore(threads + Math.max(0, maxQueued));
		AtomicInteger threadNumber = new AtomicInteger();
		ThreadFactory factory = runnable -> {
			Thread thread = new Thread(runnable, name + "-" + threadNumber.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		};
		executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), factory);
	}

	/**
	 * Runs the task on a worker, waiting first if the queue is full. A task that throws is logged with its
	 * description and counted; see {@link #awaitCompletion()}.
	 */
	public void execute(String description, Runnable task) {
		try {
			capacity.acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while queueing " + description, e);
		}
		try {
			executor.execute(() -> {
				try {
					task.run();
				} catch (Throwable t) {
					failures.incrementAndGet();
					log.error("Failed: {}", description, t);
				} finally {
					capacity.release();
				}
			});
		} catch (RuntimeException e) {
			capacity.release();
			throw e;
		}
	}

	/** Records a failure that a task handled itself, so it is included in {@link #awaitCompletion()}'s count. */
	public void recordFailure() {
		failures.incrementAndGet();
	}

	/** Stops accepting tasks, waits for the queued ones to finish and returns how many failed. */
	public int awaitCompletion() {
		executor.shutdown();
		try {
			while (!executor.awaitTermination(1, TimeUnit.MINUTES))
				log.debug("Still waiting for {} queued task(s)", executor.getQueue().size() + executor.getActiveCount());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			executor.shutdownNow();
			log.error("Interrupted while waiting for tasks to finish", e);
		}
		return failures.get();
	}
}
