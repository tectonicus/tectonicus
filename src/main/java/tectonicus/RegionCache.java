/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.extern.log4j.Log4j2;
import tectonicus.chunk.ChunkLocator;

import java.io.File;
import java.util.LinkedHashMap;

@Log4j2
public class RegionCache
{
	private final SaveFormat format;
	
	private final File worldDir;
	
	// TODO: Use a hash map of SoftReferences so we grow up to max memory?
	private final RegionCacheMap cache;
	
	public RegionCache(File worldDir)
	{
		if (worldDir == null)
			throw new IllegalArgumentException("worldDir cannot be null");
		if (!worldDir.exists())
			throw new IllegalArgumentException("worldDir must exist ("+worldDir.getAbsolutePath()+")");
		
		this.worldDir = worldDir;
		
		File regionDir = new File(worldDir, "region");
		
		File[] anvilFiles = regionDir.listFiles(new AnvilFileFilter());
		if (anvilFiles.length > 0)
			format = SaveFormat.Anvil;
		else
			format = SaveFormat.McRegion;
		
		log.info("Detected {} save format", format);
		
		this.cache = new RegionCacheMap(8);
		this.cache.setMinSize(4);
	}
	
	public SaveFormat getFormat()
	{
		return this.format;
	}
	
	public Region getRegion(RegionCoord coord)
	{
		Region region = null;
		
		region = cache.get(coord);
		if (region == null)
		{
			File regionFile = ChunkLocator.findRegionFile(worldDir, coord, format);
			if (regionFile.exists())
			{
				try
				{
					region = new Region(regionFile);
					
					cache.put(coord, region);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			cache.touch(coord);
		}
		
		return region;
	}
	
	private static class RegionCacheMap extends LinkedHashMap<RegionCoord, Region>
	{
		private static final long serialVersionUID = 1L;

		private final int maxSize;
		private int minSize;
		
		public RegionCacheMap(final int maxSize)
		{
			this.maxSize = maxSize;
		}
		
		public void setMinSize(final int size)
		{
			this.minSize = size;
		}
		
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<RegionCoord, Region> eldest)
		{
			if (size() <= minSize)
				return false;

			return size() > maxSize;
		}
		
		public void touch(RegionCoord coord)
		{
			Region r = remove(coord);
			if (r != null)
				put(coord, r);
		}
	}
}
