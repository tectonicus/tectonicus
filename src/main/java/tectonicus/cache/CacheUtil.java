/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

import lombok.extern.log4j.Log4j2;
import lombok.experimental.UtilityClass;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.configuration.Map;
import tectonicus.util.TempArea;

@Log4j2
@UtilityClass
public class CacheUtil
{

	public static void writeCacheFile(final File file, final byte[] hash)
	{
		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null; 
		try
		{
			file.getParentFile().mkdirs();
			
			fOut = new FileOutputStream(file);
			bOut = new BufferedOutputStream(fOut);
			
			bOut.write(hash);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (bOut != null)
					bOut.close();
				if (fOut != null)
					fOut.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static byte[] readHash(File file)
	{
		byte[] result = new byte[0];
		
		FileInputStream fileIn = null;
		BufferedInputStream bufferedIn = null;
		try
		{
			if (file.exists())
			{
				final int fileSize = (int)file.length();
				byte[] cacheContents = new byte[fileSize];
				
				fileIn = new FileInputStream(file);
				bufferedIn = new BufferedInputStream(fileIn);
				
				final int read = bufferedIn.read(cacheContents);
				if (read == fileSize)
					result = cacheContents;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (bufferedIn != null)
			{
				try
				{
					bufferedIn.close();
				}
				catch (Exception e) {}
			}
			
			if (fileIn != null)
			{
				try
				{
					fileIn.close();
				}
				catch (Exception e) {}
			}
		}
		
		return result;
	}

	public static byte[] calcHash(File file, MessageDigest hashAlgo)
	{
		byte[] result = new byte[0];
		
		FileInputStream fileIn = null;
		BufferedInputStream bufferedIn = null;
		try
		{
			if (file.exists())
			{
				fileIn = new FileInputStream(file);
				bufferedIn = new BufferedInputStream(fileIn);
				
				hashAlgo.reset();
				
				while (true)
				{
					byte[] buffer = new byte[1024 * 4];
					final int bytesRead = bufferedIn.read(buffer);
					if (bytesRead == -1)
						break;
					hashAlgo.update(buffer, 0, bytesRead);
				}
				
				result = hashAlgo.digest();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (bufferedIn != null)
			{
				try
				{
					bufferedIn.close();
				}
				catch (Exception e) {}
			}
			
			if (fileIn != null)
			{
				try
				{
					fileIn.close();
				}
				catch (Exception e) {}
			}
		}
		
		return result;
	}

	public static byte[] hash(String input, MessageDigest algo)
	{
		algo.reset();
		algo.update(input.getBytes());
		return algo.digest();
	}

	public static boolean equal(final byte[] lhs, final byte[] rhs)
	{
		if (lhs.length != rhs.length)
			return false;
		
		for (int i=0; i<lhs.length; i++)
		{
			if (lhs[i] != rhs[i])
				return false;
		}
		
		return true;
	}
	
	public static BiomeCache createBiomeCache(File minecraftJar, File baseCacheDir, Map map, MessageDigest hashAlgorithm)
	{
		BiomeCache biomeCache = null;
		if (map.useBiomeColours())
		{
			try
			{
				File actualDir = new File(baseCacheDir, map.getId());
				biomeCache = new FileBiomeCache(actualDir, map.getWorldDir(), minecraftJar, hashAlgorithm);
			}
			catch (Exception e)
			{
				log.warn("Couldn't create biome cache - biome colours will be disabled");
				e.printStackTrace();
			}
		}
		
		if (biomeCache == null)
		{
			biomeCache = new NullBiomeCache();
		}
		
		return biomeCache;
	}

	public static TileCache createTileCache(final boolean useCache, String optionString, ImageFormat imageFormat, File rootCacheDir, tectonicus.configuration.Map map, Layer layer, MessageDigest hashAlgorithm) {
		if (useCache) {
			File subDir = new File(rootCacheDir, "tileHashes");
			File mapDir = new File(subDir, layer.getMapId());
			File layerDir = new File(mapDir, layer.getId());

			return new FileTileCache(layerDir, imageFormat, map, layer, optionString, hashAlgorithm);
		} else {
			return new NullTileCache();
		}
	}

	public static FileViewCache createViewCache(File cacheDir, tectonicus.configuration.Map map, TempArea tempArea, MessageDigest hashAlgorithm, RegionHashStore regionHashStore) {
		File viewsCache = new File(cacheDir, "views");
		File mapViewsCache = new File(viewsCache, map.getId());

		return new FileViewCache(mapViewsCache, tempArea, hashAlgorithm, regionHashStore);
	}
}
