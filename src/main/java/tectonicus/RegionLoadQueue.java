/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
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
import tectonicus.configuration.Map;
import tectonicus.configuration.filter.PortalFilter;
import tectonicus.raw.BeaconEntity;
import tectonicus.raw.BedEntity;
import tectonicus.raw.ContainerEntity;
import tectonicus.world.Sign;

import java.security.MessageDigest;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static tectonicus.util.FindEntityUtil.findBeacons;
import static tectonicus.util.FindEntityUtil.findBeds;
import static tectonicus.util.FindEntityUtil.findChests;
import static tectonicus.util.FindEntityUtil.findPortals;
import static tectonicus.util.FindEntityUtil.findPortalsOld;
import static tectonicus.util.FindEntityUtil.findSigns;
import static tectonicus.util.FindEntityUtil.findViews;

@Slf4j
public class RegionLoadQueue
{
	private final ThreadPoolExecutor executor;
        
        private final AtomicInteger numberOfRegionsStarted;
        public static final AtomicInteger numberOfHashesAdded = new AtomicInteger();
        
	public RegionLoadQueue(final int numThreads)
	{
                numberOfRegionsStarted = new AtomicInteger();
            
		executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16), new ResubmitHandler());
	}
	
	public void load(Chunk chunk, RegionHashStore regionHashStore, MessageDigest hashAlgorithm,
					 Map map, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs,
					 HddObjectListWriter<Sign> views, ConcurrentLinkedQueue<ContainerEntity> chests, Queue<BedEntity> beds, Queue<BeaconEntity> beacons) {
		numberOfRegionsStarted.incrementAndGet();
		LoadTask task = new LoadTask(chunk, regionHashStore, hashAlgorithm, map, portals, signs, views, chests, beds, beacons);
		executor.submit(task);
	}
	
	public void waitUntilFinished()
	{
		try
		{                
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.DAYS);
		}
                catch (InterruptedException e)
                {
			log.error("Exception: ", e);
		}
	}
	
	private static class LoadTask implements Callable<Void> {
		private final Chunk chunk;
		private final RegionHashStore regionHashStore;
		private final MessageDigest hashAlgorithm;
		private final tectonicus.configuration.Map map;
		private final HddObjectListWriter<Portal> portals;
		private final HddObjectListWriter<Sign> signs;
		private final HddObjectListWriter<Sign> views;
		private final ConcurrentLinkedQueue<ContainerEntity> chests;
		private final Queue<BedEntity> beds;
		private final Queue<BeaconEntity> beacons;
		
		public LoadTask(Chunk chunk, RegionHashStore regionHashStore, MessageDigest hashAlgorithm,
						tectonicus.configuration.Map map, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs,
						HddObjectListWriter<Sign> views, ConcurrentLinkedQueue<ContainerEntity> chests, Queue<BedEntity> beds, Queue<BeaconEntity> beacons) {
			this.chunk = chunk;
			this.regionHashStore = regionHashStore;
			this.hashAlgorithm = hashAlgorithm;
			this.map = map;
			this.portals = portals;
			this.signs = signs;
			this.views = views;
			this.chests = chests;
			this.beds = beds;
			this.beacons = beacons;
		}
		
		@Override
		public Void call() {
			try {
				chunk.calculateHash(hashAlgorithm);
				regionHashStore.addHash(chunk.getCoord(), chunk.getHash());
				
				numberOfHashesAdded.incrementAndGet();
				
				findSigns(chunk.getRawChunk(), signs, map.getSignFilter());
				
				PortalFilter portalFilter = map.getPortalFilter();
				if (Minecraft.getWorldVersion() < 13) {
					findPortalsOld(chunk.getRawChunk(), portals, portalFilter);
				} else {
					findPortals(chunk.getRawChunk(), portals, portalFilter);
					findBeds(chunk.getRawChunk(), beds);
				}
				
				findViews(chunk.getRawChunk(), views, map.getViewFilter());
				
				findChests(chunk.getRawChunk(), map.getChestFilter(), chests);
				findBeacons(chunk.getRawChunk(), beacons, map.getBeaconFilter());
			} catch (Exception e) {
				log.error("Exception: ", e);
			}
			
			return null;
		}
	}
	
	private static class ResubmitHandler implements RejectedExecutionHandler
	{
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
		{
			try
			{
				executor.getQueue().put(r);
			}
			catch (Exception e)
			{
				log.error("Exception: ", e);
			}
		}
	}
}
