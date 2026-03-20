/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tectonicus.chunk.ChunkLocator;

import java.io.File;

@Slf4j
public class RegionCache {
	@Getter
	private final SaveFormat format;
	
	private final File dimensionDir;
	
	private final Cache<RegionCoord, Region> cache;
	
	public RegionCache(File dimensionDir) {
		if (dimensionDir == null)
			throw new IllegalArgumentException("dimensionDir cannot be null");
		if (!dimensionDir.exists())
			throw new IllegalArgumentException("dimensionDir must exist (" + dimensionDir.getAbsolutePath() + ")");
		
		this.dimensionDir = dimensionDir;
		
		File regionDir = new File(dimensionDir, "region");
		
		SaveFormat detectedFormat = SaveFormat.ALPHA;
		if (regionDir.exists()) {
			File[] anvilFiles = regionDir.listFiles(new AnvilFileFilter());
			if (anvilFiles != null && anvilFiles.length > 0) {
				detectedFormat = SaveFormat.ANVIL;
			} else {
				File[] mcRegionFiles = regionDir.listFiles(new McRegionFileFilter());
				if (mcRegionFiles != null && mcRegionFiles.length > 0)
					detectedFormat = SaveFormat.MC_REGION;
			}
		}

		format = detectedFormat;
		
		log.info("Detected {} save format", format);
		
		this.cache = Caffeine.newBuilder().maximumSize(16).build();
	}
	
	public Region getRegion(RegionCoord coord)
	{
		if (format == SaveFormat.ALPHA)
			return null;

		return cache.get(coord, c -> {
			File regionFile = ChunkLocator.findRegionFile(dimensionDir, c, format);
			if (regionFile.exists() && regionFile.length() > 0)
			{
				try
				{
					return new Region(regionFile);
				}
				catch (Exception e)
				{
					log.error("Exception: ", e);
				}
			}
			return null;
		});
	}
}
