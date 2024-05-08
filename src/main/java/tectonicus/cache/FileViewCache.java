/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import lombok.extern.slf4j.Slf4j;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.ViewConfig;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.PerspectiveCamera;
import tectonicus.util.TempArea;
import tectonicus.view.ChangedViews;
import tectonicus.view.ViewUtil;
import tectonicus.view.ViewUtil.Viewpoint;
import tectonicus.world.Sign;
import tectonicus.world.World;

import java.io.File;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
public class FileViewCache
{
	// Change this every time we have a major renderer change and need to invalidate the cache
	private static final int VIEW_RENDERER_VERSION = 7;
	
	private final File cacheDir;
	private final TempArea tempArea;
	private final MessageDigest hashAlgorithm;
	private final RegionHashStore regionHashStore;
	
	public FileViewCache(File cacheDir, TempArea tempArea, MessageDigest hashAlgorithm, RegionHashStore regionHashStore)
	{
		this.cacheDir = cacheDir;
		this.tempArea = tempArea;
		this.hashAlgorithm = hashAlgorithm;
		this.regionHashStore = regionHashStore;
		
		// TODO: Verify cache? Should take into account config / layer
		// ..
		
		cacheDir.mkdirs();
	}
	
	public ChangedViews findChangedViews(Rasteriser rasteriser, World world, File viewsFile, File viewsDir, ViewConfig viewConfig)
	{
		log.info("Finding changed views...");

		File changedViewsFile = tempArea.generateTempFile("views", ".list");
		
		HddObjectListReader<Sign> viewsIn = null;
		HddObjectListWriter<Sign> viewsOut = null;
		
		int totalViews = 0;
		int changedViewsCount = 0;
		try
		{
			viewsIn = new HddObjectListReader<>(viewsFile);
			viewsOut = new HddObjectListWriter<>(changedViewsFile, true);
			
			while (viewsIn.hasNext())
			{
                                Sign sign = new Sign();
				viewsIn.read(sign);

				if (world.getWorldSubset().containsBlock(sign.getX(), sign.getZ())) {
					boolean cacheOk = false;

					final File imgFile = ViewUtil.createViewFile(viewsDir, sign, viewConfig.getImageFormat());
					if (imgFile.exists()) {
						final byte[] visibleHash = calculateHash(rasteriser, world, sign, viewConfig);

						final File viewHashFile = findViewHashFile(cacheDir, sign);
						final byte[] existingHash = CacheUtil.readHash(viewHashFile);

						cacheOk = CacheUtil.equal(visibleHash, existingHash);
					}

					if (!cacheOk) {
						viewsOut.add(sign);
						changedViewsCount++;
					}

					totalViews++;
				}
			}
			
			log.debug("Found {} changed views (out of {} total views)", viewsOut.size(), totalViews);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (viewsIn != null)
				viewsIn.close();
			if (viewsOut != null)
				viewsOut.close();
		}
		
		return new ChangedViews(changedViewsFile, changedViewsCount);
	}
	
	public byte[] calculateHash(Rasteriser rasteriser, World world, Sign sign, ViewConfig viewConfig)
	{
		hashAlgorithm.reset();
		
		hashAlgorithm.update(Integer.toString(VIEW_RENDERER_VERSION).getBytes());
		
		hashAlgorithm.update(sign.getText(0).getBytes());
		hashAlgorithm.update(sign.getText(1).getBytes());
		hashAlgorithm.update(sign.getText(2).getBytes());
		hashAlgorithm.update(sign.getText(3).getBytes());
                
                hashAlgorithm.update(Integer.toString(viewConfig.getWidth()).getBytes());
                hashAlgorithm.update(Integer.toString(viewConfig.getHeight()).getBytes());
	
		Viewpoint view = ViewUtil.findView(sign);
		PerspectiveCamera camera = ViewUtil.createCamera(rasteriser, view, viewConfig);
		camera.apply();
                
		List<ChunkCoord> visibleCoords = world.findVisible(camera);
		for (ChunkCoord chunkCoord : visibleCoords)
		{
			byte[] chunkHash = regionHashStore.getChunkHash(chunkCoord);
			hashAlgorithm.update(chunkHash);
		}
		
		return hashAlgorithm.digest();
	}
	
	public void writeHash(Sign sign, Rasteriser rasteriser, World world, ViewConfig viewConfig)
	{
		final byte[] hash = calculateHash(rasteriser, world, sign, viewConfig);
		final File file = findViewHashFile(cacheDir, sign);
		
		CacheUtil.writeCacheFile(file, hash);
	}
	
	private static File findViewHashFile(File baseDir, Sign sign)
	{
		String name = "view_"+sign.getX()+"_"+sign.getY()+"_"+sign.getZ()+".cache";
		return new File(baseDir, name);
	}
}
