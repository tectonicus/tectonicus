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
package tectonicus;

import java.io.File;

import tectonicus.cache.BiomeCache;
import tectonicus.world.filter.BlockFilter;

public class ChunkLocator
{
	private final BiomeCache biomeCache;
	private final RegionCache regionCache;
	
	public ChunkLocator(File worldDir, BiomeCache biomeCache, RegionCache regionCache)
	{
		if (worldDir == null)
			throw new NullPointerException("World dir is null");
		if (biomeCache == null)
			throw new NullPointerException("Biome cache is null");
		if (regionCache == null)
			throw new NullPointerException("Region cache is null");
		
		this.biomeCache = biomeCache;
		this.regionCache = regionCache;
	}
	
	public boolean exists(ChunkCoord coord)
	{
		boolean found = false;
		
		RegionCoord regionCoord = RegionCoord.fromChunkCoord(coord);
		{
			Region region = regionCache.getRegion(regionCoord);
			if (region != null)
			{
				// See if it's in the region file
				found = region.containsChunk(coord);
				
				// TODO: Return region to cache to call close() on it later
				// ..
			}
		}
		
		// TODO: Remove all calling instances of findChunkFile?
		
		return found;
	}
	
	public Chunk loadChunk(ChunkCoord coord, BlockFilter filter)
	{
		Chunk chunk = null;
		
		chunk = loadChunkFromRegion(coord, filter);
		
		return chunk;
	}
	
	private Chunk loadChunkFromRegion(ChunkCoord chunkCoord, BlockFilter filter)
	{
		Chunk chunk = null;
		Region region = null;
		
		try
		{
			RegionCoord regionCoord = RegionCoord.fromChunkCoord(chunkCoord);
			
			region = regionCache.getRegion(regionCoord);
			if (region != null)
			{
				chunk = region.loadChunk(chunkCoord, biomeCache, filter);
			}
		}
		catch (Exception e)
		{
			System.err.println("Error while trying to load chunk at ("+chunkCoord.x+", "+chunkCoord.z+") from region "+region.getFile().getAbsolutePath());
			e.printStackTrace();
		}
		finally
		{
			
		}
		
		return chunk;
	}
	
	public static File findChunkFile(File baseDir, ChunkCoord coord)
	{
		// world/0/0/c.0.0.dat
		
		String firstFrag = Util.toBase36( (coord.x & 63) );
		String secondFrag = Util.toBase36( (coord.z & 63) );
		
		File firstDir = new File(baseDir, firstFrag);
		File secondDir = new File(firstDir, secondFrag);
		
		String first = Util.toBase36(coord.x);
		String second = Util.toBase36(coord.z); 
		File actual = new File(secondDir, "c."+first+"."+second+".dat");
		
		return actual;
	}
	
	public static File findRegionFile(File baseDir, RegionCoord coord, SaveFormat format)
	{
		// world/region/r.0.0.mca
		
		File regionDir = new File(baseDir, "region");
		
		StringBuilder builder = new StringBuilder();
		builder.append("r.");
		builder.append(coord.x);
		builder.append(".");
		builder.append(coord.z);
		builder.append(".");
		builder.append(format.extension);
		
		File actual = new File(regionDir, builder.toString());
		
		return actual;
	}
}
