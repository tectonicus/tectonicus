/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.log4j.Log4j2;
import tectonicus.chunk.ChunkLocator;

import java.io.File;

@Log4j2
public class RegionCache
{
	private final SaveFormat format;
	
	private final File worldDir;
	
	private final Cache<RegionCoord, Region> cache;
	
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
		
		this.cache = Caffeine.newBuilder().maximumSize(16).build();
	}
	
	public SaveFormat getFormat()
	{
		return this.format;
	}
	
	public Region getRegion(RegionCoord coord)
	{
		return cache.get(coord, (c) -> {
                        File regionFile = ChunkLocator.findRegionFile(worldDir, c, format);
			if (regionFile.exists() && regionFile.length() > 0)
			{
				try
				{
					return new Region(regionFile);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
                        return null;
                });
	}
}
