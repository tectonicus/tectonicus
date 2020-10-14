/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;

import lombok.extern.log4j.Log4j2;
import tectonicus.cache.BiomeCache;
import tectonicus.world.filter.BlockFilter;

@Log4j2
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
				chunk = region.loadChunk(chunkCoord, biomeCache, filter, null);
			}
		}
		catch (Exception e)
		{
			log.error("ChunkLocator: Error while trying to load chunk at ({}, {}) from region {}", chunkCoord.x, chunkCoord.z, region.getFile().getAbsolutePath(), e);
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
