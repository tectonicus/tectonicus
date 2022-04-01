/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import tectonicus.chunk.ChunkCoord;
import tectonicus.RegionCoord;

import com.google.code.minecraftbiomeextractor.FileUtils;

public class RegionHashStore
{
	private File hashStoreDir;
	
	private RegionHashes activeHashes;
	
	private HashCache hashCache;
	
	public RegionHashStore(File cacheDir)
	{
		hashStoreDir = new File(cacheDir, "hashStore");
		FileUtils.deleteDirectory(hashStoreDir);
		hashStoreDir.mkdirs();
		
		hashCache = new HashCache(32);
		hashCache.setMinSize(16);
	}
	
	public void startRegion(RegionCoord coord)
	{
		if (activeHashes != null)
			throw new RuntimeException("Region already active");
		
		activeHashes = new RegionHashes(coord);
	}
	
	public void addHash(ChunkCoord chunkCoord, byte[] hash)
	{
		if (hash == null)
			return;
		
		activeHashes.addHash(chunkCoord, hash);
	}
	
	public void endRegion()
	{
		activeHashes.write(hashStoreDir);
		
		hashCache.put(activeHashes.getRegionCoord(), activeHashes);
		
		activeHashes = null;
	}

	public byte[] getChunkHash(ChunkCoord chunkCoord)
	{
		RegionCoord regionCoord = RegionCoord.fromChunkCoord(chunkCoord);
		
		RegionHashes hashes = null;
		
		hashes = hashCache.get(regionCoord);
		if (hashes != null)
		{
			// Found in cache!
			hashCache.touch(regionCoord);
		}
		else
		{
			// Load from disk, insert into cache
			hashes = new RegionHashes(regionCoord);
			hashes.read(hashStoreDir);
			hashCache.put(regionCoord, hashes);
		}
		
		return hashes.getHash(chunkCoord);
	}
	
	private static class RegionHashes
	{
		private final RegionCoord regionCoord;
		private Map<ChunkCoord, byte[]> chunkHashes;
		
		public RegionHashes(RegionCoord coord)
		{
			this.regionCoord = coord;
			
			chunkHashes = new HashMap<ChunkCoord, byte[]>();
		}
		
		public RegionCoord getRegionCoord()
		{
			return regionCoord;
		}
		
		public void addHash(ChunkCoord chunkCoord, byte[] hash)
		{
			chunkHashes.put(chunkCoord, hash);
		}
		
		public void write(File baseDir)
		{
			FileOutputStream fOut = null;
			DataOutputStream out = null;
			try
			{
				File outFile = getHashFile(baseDir, regionCoord);
				
				fOut = new FileOutputStream(outFile);
				out = new DataOutputStream(fOut);
				
				// Magic
				out.writeInt(0xCAFEBABE);
				
				// Num hashes
				out.writeInt(chunkHashes.size());
				
				for (ChunkCoord coord : chunkHashes.keySet())
				{
					byte[] hash = chunkHashes.get(coord);
					
					// Chunk magic
					out.writeInt(0xFEEFEE);
					
					// Chunk coord
					out.writeLong(coord.x);
					out.writeLong(coord.z);
					
					// Hash length
					out.writeInt(hash.length);
					
					// Hash
					out.write(hash);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if (out != null)
						out.close();
					if (fOut != null)
						fOut.close();
				}
				catch (Exception e) {}
			}
		}
		
		public void read(File baseDir)
		{
			FileInputStream fIn = null;
			DataInputStream in = null;
			
			try
			{
				File inFile = getHashFile(baseDir, regionCoord);
				
				fIn = new FileInputStream(inFile);
				in = new DataInputStream(fIn);
				
				final int magic = in.readInt();
				assert (magic == 0xCAFEBABE);
				
				final int numHashes = in.readInt();
				for (int i=0; i<numHashes; i++)
				{
					final int chunkMagic = in.readInt();
					assert (chunkMagic == 0xFEEFEE);
					
					final long chunkX = in.readLong();
					final long chunkZ = in.readLong();
					
					final int hashLen = in.readInt();
					
					byte[] hash = new byte[hashLen];
					in.readFully(hash);
					
					addHash(new ChunkCoord(chunkX, chunkZ), hash);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if (in != null)
						in.close();
					if (fIn != null)
						fIn.close();
				}
				catch (Exception e) {}
			}
		}
		
		public byte[] getHash(ChunkCoord chunkCoord)
		{
			return chunkHashes.get(chunkCoord);
		}
		
		private static File getHashFile(File baseDir, RegionCoord coord)
		{
			return new File(baseDir, "r-"+coord.x+"-"+coord.z+".hashes");
		}
	}
	
	private static class HashCache extends LinkedHashMap<RegionCoord, RegionHashes>
	{
		private static final long serialVersionUID = 1L;

		private int maxSize;
		private int minSize;
		
		public HashCache(int maxSize)
		{
			this.maxSize = maxSize;
		}
		
		public void setMinSize(final int size)
		{
			this.minSize = size;
		}
		
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<RegionCoord, RegionHashes> eldest)
		{
			if (size() <= minSize)
				return false;
			
			final boolean remove = size() > maxSize;
			if (remove)
			{
				// Nothing to do
			}
			return remove;
		}
		
		public void touch(RegionCoord coord)
		{
			RegionHashes c = remove(coord);
			if (c != null)
				put(coord, c);
		}
	}
}
