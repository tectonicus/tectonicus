/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.extern.slf4j.Slf4j;
import tectonicus.cache.RegionHashStore;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.DimensionConfig;
import tectonicus.raw.BedEntity;
import tectonicus.raw.ContainerEntity;
import tectonicus.util.BoundedExecutor;
import tectonicus.world.Sign;

import java.security.MessageDigest;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

import static tectonicus.util.FindEntityUtil.findBeds;
import static tectonicus.util.FindEntityUtil.findContainers;
import static tectonicus.util.FindEntityUtil.findPortals;
import static tectonicus.util.FindEntityUtil.findSigns;
import static tectonicus.util.FindEntityUtil.findViews;

@Slf4j
public class RegionLoadQueue
{
	private final BoundedExecutor executor;
	private final IntFunction<byte[]> mapContentHash;
	
	public RegionLoadQueue(final int numThreads, IntFunction<byte[]> mapContentHash)
	{
		this.mapContentHash = mapContentHash;
		executor = new BoundedExecutor("region-loader", numThreads, 16);
	}
	
	public void load(Chunk chunk, RegionHashStore regionHashStore, MessageDigest hashAlgorithm,
					 DimensionConfig map, HddObjectListWriter<PortalColumn> portals, HddObjectListWriter<Sign> signs,
					 HddObjectListWriter<Sign> views, ConcurrentLinkedQueue<ContainerEntity> containers, Queue<BedEntity> beds) {
		LoadTask task = new LoadTask(chunk, regionHashStore, hashAlgorithm, map, portals, signs, views, containers, beds);
		executor.execute("loading chunk " + chunk.getCoord(), task);
	}
	
	/** Waits for all queued chunks and returns how many of them failed to load. */
	public int waitUntilFinished()
	{
		final int failed = executor.awaitCompletion();
		if (failed > 0)
			log.warn("{} chunk(s) could not be loaded and were left out of the signs, portals, views and containers", failed);
		return failed;
	}
	
	private class LoadTask implements Runnable {
		private final Chunk chunk;
		private final RegionHashStore regionHashStore;
		private final MessageDigest hashAlgorithm;
		private final DimensionConfig map;
		private final HddObjectListWriter<PortalColumn> portals;
		private final HddObjectListWriter<Sign> signs;
		private final HddObjectListWriter<Sign> views;
		private final ConcurrentLinkedQueue<ContainerEntity> containers;
		private final Queue<BedEntity> beds;
		
		public LoadTask(Chunk chunk, RegionHashStore regionHashStore, MessageDigest hashAlgorithm,
						DimensionConfig map, HddObjectListWriter<PortalColumn> portals, HddObjectListWriter<Sign> signs,
						HddObjectListWriter<Sign> views, ConcurrentLinkedQueue<ContainerEntity> containers, Queue<BedEntity> beds) {
			this.chunk = chunk;
			this.regionHashStore = regionHashStore;
			this.hashAlgorithm = hashAlgorithm;
			this.map = map;
			this.portals = portals;
			this.signs = signs;
			this.views = views;
			this.containers = containers;
			this.beds = beds;
		}
		
		@Override
		public void run() {
			chunk.calculateHash(hashAlgorithm, mapContentHash);
			regionHashStore.addHash(chunk.getCoord(), chunk.getHash());
			
			findSigns(chunk.getRawChunk(), signs, map.getSignFilter());
			
			findPortals(chunk.getRawChunk(), portals, map.getPortalFilter(), map.getWorldSubset());
			findBeds(chunk.getRawChunk(), beds);
			
			findViews(chunk.getRawChunk(), views, map.getViewFilter());
			
			findContainers(chunk.getRawChunk(), map.getContainerFilter(), containers);
		}
	}
}
