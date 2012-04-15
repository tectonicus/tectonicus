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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import tectonicus.ChunkCoord;
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
