/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.cache;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;

import tectonicus.ChunkCoord;
import tectonicus.ViewUtil;
import tectonicus.ViewUtil.Viewpoint;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.PerspectiveCamera;
import tectonicus.util.TempArea;
import tectonicus.world.Sign;
import tectonicus.world.World;

public class FileViewCache
{
	// Change this every time we have a major renderer change and need to invalidate the cache
	private static final int VIEW_RENDERER_VERSION = 6;
	
	private final File cacheDir;
	
	private final TempArea tempArea;
	private final MessageDigest hashAlgorithm;
	private RegionHashStore regionHashStore;
	
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
	
	public File findChangedViews(Rasteriser rasteriser, World world, File viewsFile, File viewsDir, ImageFormat imageFormat, final int drawDistance)
	{
		System.out.println("Finding changed views...");
		
		File changedViewsFile = tempArea.generateTempFile("views", ".list");
		
		HddObjectListReader<Sign> viewsIn = null;
		HddObjectListWriter<Sign> viewsOut = null;
		
		int totalViews = 0;
		
		try
		{
			viewsIn = new HddObjectListReader<Sign>(viewsFile);
			viewsOut = new HddObjectListWriter<Sign>(changedViewsFile, true);
			
			Sign sign = new Sign();
			while (viewsIn.hasNext())
			{
				viewsIn.read(sign);
				
				boolean cacheOk = false;
				
				final File imgFile = ViewUtil.createViewFile(viewsDir, sign, imageFormat);
				if (imgFile.exists())
				{
					final byte[] visibleHash = calculateHash(rasteriser, world, sign, drawDistance);
					
					final File viewHashFile = findViewHashFile(cacheDir, sign);
					final byte[] existingHash = CacheUtil.readHash(viewHashFile);
					
					cacheOk = CacheUtil.equal(visibleHash, existingHash);
				}
				
				if (!cacheOk)
				{
					viewsOut.add(sign);
				}
				
				totalViews++;
			}
			
			System.out.println("Found "+viewsOut.size()+" changed views (out of "+totalViews+" total views)");
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
		
		return changedViewsFile;
	}
	
	public byte[] calculateHash(Rasteriser rasteriser, World world, Sign sign, final int drawDistance)
	{
		hashAlgorithm.reset();
		
		hashAlgorithm.update(Integer.toString(VIEW_RENDERER_VERSION).getBytes());
		
		hashAlgorithm.update(sign.getText(0).getBytes());
		hashAlgorithm.update(sign.getText(1).getBytes());
		hashAlgorithm.update(sign.getText(2).getBytes());
		hashAlgorithm.update(sign.getText(3).getBytes());
	
		Viewpoint view = ViewUtil.findView(sign);
		PerspectiveCamera camera = ViewUtil.createCamera(rasteriser, view, drawDistance);
		camera.apply();
		
		ArrayList<ChunkCoord> visibleCoords = world.findVisible(camera);
		for (ChunkCoord chunkCoord : visibleCoords)
		{
			byte[] chunkHash = regionHashStore.getChunkHash(chunkCoord);
			hashAlgorithm.update(chunkHash);
		}
		
		final byte[] finalHash = hashAlgorithm.digest();
		
		return finalHash;
	}
	
	public void writeHash(Sign sign, Rasteriser rasteriser, World world, final int drawDistance)
	{
		final byte[] hash = calculateHash(rasteriser, world, sign, drawDistance);
		final File file = findViewHashFile(cacheDir, sign);
		
		CacheUtil.writeCacheFile(file, hash);
	}
	
	private static File findViewHashFile(File baseDir, Sign sign)
	{
		String name = "view_"+sign.getX()+"_"+sign.getY()+"_"+sign.getZ()+".cache";
		return new File(baseDir, name);
	}
}
