/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
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

import tectonicus.configuration.Map;

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
				System.out.println("Couldn't create biome cache - biome colours will be disabled");
				e.printStackTrace();
			}
		}
		
		if (biomeCache == null)
		{
			biomeCache = new NullBiomeCache();
		}
		
		return biomeCache;
	}
}
