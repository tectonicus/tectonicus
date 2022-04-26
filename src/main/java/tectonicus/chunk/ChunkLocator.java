/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.chunk;

import lombok.extern.log4j.Log4j2;
import tectonicus.Region;
import tectonicus.RegionCache;
import tectonicus.RegionCoord;
import tectonicus.SaveFormat;
import tectonicus.cache.BiomeCache;
import tectonicus.util.Util;
import tectonicus.world.WorldInfo;
import tectonicus.world.filter.BlockFilter;

import java.io.File;

@Log4j2
public class ChunkLocator
{
	private final BiomeCache biomeCache;
	private final RegionCache regionCache;
	
	public ChunkLocator(BiomeCache biomeCache, RegionCache regionCache)
	{
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
	
	public Chunk loadChunkFromRegion(ChunkCoord chunkCoord, BlockFilter filter, WorldInfo worldInfo)
	{
		Chunk chunk = null;
		Region region = null;
		
		try
		{
			RegionCoord regionCoord = RegionCoord.fromChunkCoord(chunkCoord);
			
			region = regionCache.getRegion(regionCoord);
			if (region != null)
			{
				chunk = region.loadChunk(chunkCoord, biomeCache, filter, null, worldInfo);
			}
		}
		catch (Exception e)
		{
			log.error("ChunkLocator: Error while trying to load chunk at ({}, {}) from region {}", chunkCoord.x, chunkCoord.z, region.getRegionFile().getAbsolutePath(), e);
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

		return new File(secondDir, "c."+first+"."+second+".dat");
	}
	
	public static File findRegionFile(File baseDir, RegionCoord coord, SaveFormat format)
	{
		// world/region/r.0.0.mca
		
		File regionDir = new File(baseDir, "region");

		String builder = "r." + coord.x + "." + coord.z + "." + format.extension;

		return new File(regionDir, builder);
	}
}
