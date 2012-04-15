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
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.google.code.minecraftbiomeextractor.WorldProcessor;

import tectonicus.ChunkCoord;
import tectonicus.Util;
import tectonicus.util.FileUtils;

public class FileBiomeCache implements BiomeCache
{
	private static final int VERSION = 2;
	
	private final File cacheDir;
	
	private WorldProcessor worldProcessor;

	private BiomeCacheMap biomeDataCache;
	
	public FileBiomeCache(File baseCacheDir, File worldDir, File minecraftJar, MessageDigest hashAlgo)
	{
		cacheDir = new File(baseCacheDir, "biomeCache");
		
		if (isCacheValid(cacheDir, hashAlgo))
		{
			System.out.println("Biome cache directory found and is valid. Using cache at "+cacheDir.getAbsolutePath());
		}
		else
		{
			System.out.println("Biome cache not present or outdated. Deleting biome cache at "+cacheDir.getAbsolutePath());
			System.out.println("New biome cache will be created for this render");
			
			// Recreate the biome cache dir
			FileUtils.deleteDirectory(cacheDir);
			cacheDir.mkdirs();
			
			// Write cache index file
			CacheUtil.writeCacheFile(findIndexFile(cacheDir), getCacheHashString().getBytes());
		}
		
		biomeDataCache = new BiomeCacheMap(128);
		
		// Start up minecraft to extract biome data out of
		File stubWorldDir = new File(cacheDir, "stubWorld");
		FileUtils.deleteDirectory(stubWorldDir);
		stubWorldDir.mkdirs();
		
		try
		{
			FileUtils.copyFiles(new File(worldDir, "level.dat"), new File(stubWorldDir, "level.dat"), new HashSet<String>());
			FileUtils.copyFiles(new File(worldDir, "session.lock"), new File(stubWorldDir, "session.lock"), new HashSet<String>());
		}
		catch (IOException e) {}
		
		worldProcessor = new WorldProcessor();
		worldProcessor.setJarLocation(minecraftJar);
		worldProcessor.setWorldFolder(stubWorldDir);
		
		final boolean bindOk = worldProcessor.bindToMinecraft();
		if (!bindOk)
		{
			throw new RuntimeException("Failed to bind to minecraft.jar");
		}
		
		final boolean loadOk = worldProcessor.loadWorld();
		if (!loadOk)
		{
			throw new RuntimeException("Failed to load world into biome extractor");
		}
	}
	
	public WorldProcessor getWorldProcessor()
	{
		return worldProcessor;
	}
	
	private static boolean isCacheValid(File cacheDir, MessageDigest hashAlgo)
	{
		File indexFile = findIndexFile(cacheDir);
		if (!indexFile.exists())
			return false;
		
		String versionStr = getCacheHashString();
		
		final byte[] existingHash = CacheUtil.calcHash(indexFile, hashAlgo);
		final byte[] expectedHash = CacheUtil.hash(versionStr, hashAlgo);
		
		return CacheUtil.equal(existingHash, expectedHash);
	}
	
	private static String getCacheHashString()
	{
		return "" + VERSION;
	}
	
	private static File findIndexFile(File cacheDir)
	{
		return new File(cacheDir, "biomes.cache");
	}
	
	public BiomeData loadBiomeData(ChunkCoord coord)
	{
		BiomeData data;
		
		data = biomeDataCache.get(coord);
		if (data != null)
		{
			// Touch cache
			biomeDataCache.touch(coord);
		}
		else
		{
			// Try and find cache file
			File cacheFile = findCacheFile(cacheDir, coord);
			data = loadFromFile(cacheFile);
			if (data != null)
			{
				// Loaded from file, add to cache
				biomeDataCache.put(coord, data);	
			}
			else
			{
				System.out.println("Creating biome cache for chunk @ "+coord.x+", "+coord.z);
				
				// Generate from world processor
				data = new BiomeData(worldProcessor, coord);
			
				// Add to cache
				biomeDataCache.put(coord, data);
			
				// Write to disk
				data.write(cacheFile);
				
				// Test!
				/*
				try
				{
					BiomeData backAgain = new BiomeData(cacheFile);
					if (data.equals(backAgain))
					{
						System.out.println("match!");
					}
					else
					{
						System.out.println("oh noes");
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				*/
			}
		}
		
		return data;
	}
	
	private static BiomeData loadFromFile(File file)
	{
		if (!file.exists())
			return null;
		
		BiomeData data = null;
		
		try
		{
			data = new BiomeData(file);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return data;
	}
	
	private static File findCacheFile(File baseDir, ChunkCoord coord)
	{
	//	String fileName = "biomeData_"+coord.x+"_"+coord.z+".dat";
		
		String first = Util.toBase36(coord.x);
		String second = Util.toBase36(coord.z);
		
		File firstDir = new File(baseDir, first);
		File secondDir = new File(firstDir, second);
		File actual = new File(secondDir, "biomeData_"+first+"_"+second+".dat");
		
		return actual;
	}
	
	private static class BiomeCacheMap extends LinkedHashMap<ChunkCoord, BiomeData>
	{
		private static final long serialVersionUID = 1L;

		private int maxSize;
		private int minSize;
		
		public BiomeCacheMap(int maxSize)
		{
			this.maxSize = maxSize;
		}
		
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<ChunkCoord, BiomeData> eldest)
		{
			if (size() <= minSize)
				return false;
			
			final boolean remove = size() > maxSize;
			if (remove)
			{
				// nothing to do to unload it
			}
			return remove;
		}
		
		public void touch(ChunkCoord coord)
		{
			BiomeData b = remove(coord);
			if (b != null)
				put(coord, b);
		}
	}
}
