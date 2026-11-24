/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.Hashing;
import lombok.extern.slf4j.Slf4j;
import tectonicus.cache.RegionHashStore;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.DimensionConfig;
import tectonicus.raw.BedEntity;
import tectonicus.raw.ContainerEntity;
import tectonicus.util.BoundedExecutor;
import tectonicus.world.Sign;

import java.security.MessageDigest;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static tectonicus.util.FindEntityUtil.findBeds;
import static tectonicus.util.FindEntityUtil.findContainers;
import static tectonicus.util.FindEntityUtil.findPortals;
import static tectonicus.util.FindEntityUtil.findSigns;
import static tectonicus.util.FindEntityUtil.findViews;

/**
 * Loads chunks, then hashes them and extracts their signs, portals, views, beds and containers, on a pool of worker
 * threads.
 * <p>
 * One queue serves a whole map: call {@link #waitUntilFinished()} at the end of each region and {@link #close()} once
 * the last region is done, so the worker threads are started once rather than for every region.
 */
@Slf4j
public class RegionLoadQueue implements AutoCloseable
{
	private final BoundedExecutor executor;
	private final IntFunction<byte[]> mapContentHash;
	private final ThreadLocal<ChunkDigest> chunkDigest = ThreadLocal.withInitial(ChunkDigest::new);
	
	public RegionLoadQueue(final int numThreads, IntFunction<byte[]> mapContentHash)
	{
		this.mapContentHash = mapContentHash;
		executor = new BoundedExecutor("region-loader", numThreads, 16);
	}
	
	/**
	 * Queues a chunk. On a worker thread, {@code loader} reads and parses it, then {@code discover} looks at it; only a
	 * chunk that {@code discover} accepts is hashed and searched for entities. Both run concurrently for different
	 * chunks, so they must be thread safe. A null chunk from {@code loader} is skipped.
	 */
	public void load(ChunkCoord coord, Supplier<Chunk> loader, Predicate<Chunk> discover, RegionHashStore regionHashStore,
					 DimensionConfig map, HddObjectListWriter<PortalColumn> portals, HddObjectListWriter<Sign> signs,
					 HddObjectListWriter<Sign> views, ConcurrentLinkedQueue<ContainerEntity> containers, Queue<BedEntity> beds) {
		LoadTask task = new LoadTask(loader, discover, regionHashStore, map, portals, signs, views, containers, beds);
		executor.execute("loading chunk " + coord, task);
	}
	
	/**
	 * Waits for all chunks queued so far and returns how many of them failed to load since the previous call. The queue
	 * stays usable for the next region.
	 */
	public int waitUntilFinished()
	{
		return logFailures(executor.awaitIdle());
	}
	
	/** Waits for any chunks still queued, then stops the worker threads. */
	@Override
	public void close()
	{
		logFailures(executor.awaitCompletion());
	}
	
	private static int logFailures(int failed)
	{
		if (failed > 0)
			log.warn("{} chunk(s) could not be loaded and were left out of the signs, portals, views and containers", failed);
		return failed;
	}
	
	private class LoadTask implements Runnable {
		private final Supplier<Chunk> loader;
		private final Predicate<Chunk> discover;
		private final RegionHashStore regionHashStore;
		private final DimensionConfig map;
		private final HddObjectListWriter<PortalColumn> portals;
		private final HddObjectListWriter<Sign> signs;
		private final HddObjectListWriter<Sign> views;
		private final ConcurrentLinkedQueue<ContainerEntity> containers;
		private final Queue<BedEntity> beds;
		
		public LoadTask(Supplier<Chunk> loader, Predicate<Chunk> discover, RegionHashStore regionHashStore,
						DimensionConfig map, HddObjectListWriter<PortalColumn> portals, HddObjectListWriter<Sign> signs,
						HddObjectListWriter<Sign> views, ConcurrentLinkedQueue<ContainerEntity> containers, Queue<BedEntity> beds) {
			this.loader = loader;
			this.discover = discover;
			this.regionHashStore = regionHashStore;
			this.map = map;
			this.portals = portals;
			this.signs = signs;
			this.views = views;
			this.containers = containers;
			this.beds = beds;
		}
		
		@Override
		public void run() {
			Chunk chunk = loader.get();
			if (chunk == null || !discover.test(chunk))
				return;
			
			chunk.calculateHash(chunkDigest.get(), mapContentHash);
			regionHashStore.addHash(chunk.getCoord(), chunk.getHash());
			
			findSigns(chunk.getRawChunk(), signs, map.getSignFilter());
			
			findPortals(chunk.getRawChunk(), portals, map.getPortalFilter(), map.getWorldSubset());
			findBeds(chunk.getRawChunk(), beds);
			
			findViews(chunk.getRawChunk(), views, map.getViewFilter());
			
			findContainers(chunk.getRawChunk(), map.getContainerFilter(), containers);
		}
	}

	/** Adapts XXH3 to the existing chunk serialization and confines it to change detection. */
	private static final class ChunkDigest extends MessageDigest {
		private final HashStream128 stream = Hashing.xxh3_128().hashStream();

		private ChunkDigest() {
			super("XXH3-128");
		}

		@Override
		protected void engineUpdate(byte value) {
			stream.putByte(value);
		}

		@Override
		protected void engineUpdate(byte[] bytes, int offset, int length) {
			stream.putBytes(bytes, offset, length);
		}

		@Override
		protected byte[] engineDigest() {
			byte[] hash = stream.get().toByteArray();
			engineReset();
			return hash;
		}

		@Override
		protected void engineReset() {
			stream.reset();
		}
	}
}
