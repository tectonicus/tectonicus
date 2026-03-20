/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.chunk;

import lombok.extern.slf4j.Slf4j;
import org.jnbt.NBTInputStream.Compression;
import tectonicus.Region;
import tectonicus.RegionCache;
import tectonicus.RegionCoord;
import tectonicus.SaveFormat;
import tectonicus.WorldStats;
import tectonicus.cache.BiomeCache;
import tectonicus.util.Util;
import tectonicus.world.WorldInfo;
import tectonicus.world.filter.BlockFilter;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChunkLocator {
	private static final Pattern CHUNK_FILE_PATTERN = Pattern.compile("^c\\.([-.0-9a-z]+)\\.([-.0-9a-z]+)\\.dat$");

	private final BiomeCache biomeCache;
	private final RegionCache regionCache;
	private final File baseDir;
	private final SaveFormat format;
	
	public ChunkLocator(BiomeCache biomeCache, RegionCache regionCache, File baseDir) {
		if (biomeCache == null)
			throw new NullPointerException("Biome cache is null");
		if (regionCache == null)
			throw new NullPointerException("Region cache is null");
		if (baseDir == null)
			throw new NullPointerException("baseDir is null");
		
		this.biomeCache = biomeCache;
		this.regionCache = regionCache;
		this.baseDir = baseDir;
		this.format = regionCache.getFormat();
	}
	
	public boolean exists(ChunkCoord coord) {
		if (format == SaveFormat.ALPHA) {
			File chunkFile = findChunkFile(baseDir, coord);
			return chunkFile.exists() && chunkFile.length() > 0;
		}

		boolean found = false;
		
		RegionCoord regionCoord = RegionCoord.fromChunkCoord(coord);
		Region region = regionCache.getRegion(regionCoord);
		if (region != null) {
			// See if it's in the region file
			found = region.containsChunk(coord);
			
			// TODO: Return region to cache to call close() on it later
		}
		
		return found;
	}
	
	public Chunk loadChunk(ChunkCoord chunkCoord, BlockFilter filter, WorldStats worldStats, WorldInfo worldInfo) {
		Chunk chunk = null;
		
		if (format == SaveFormat.ALPHA) {
			File chunkFile = findChunkFile(baseDir, chunkCoord);
			if (chunkFile.exists() && chunkFile.length() > 0) {
				try {
					chunk = new Chunk(chunkCoord, biomeCache);
					byte[] chunkBytes = Files.readAllBytes(chunkFile.toPath());
					chunk.loadRaw(new ChunkData(chunkBytes, Compression.Gzip), filter, worldStats, worldInfo);
				} catch (Exception e) {
					log.error("ChunkLocator: Error while trying to load alpha chunk at ({}, {}) from {}", chunkCoord.x, chunkCoord.z, chunkFile.getAbsolutePath(), e);
				}
			}
			
			return chunk;
		}
		
		Region region = null;
		try {
			RegionCoord regionCoord = RegionCoord.fromChunkCoord(chunkCoord);
			
			region = regionCache.getRegion(regionCoord);
			if (region != null) {
				chunk = region.loadChunk(chunkCoord, biomeCache, filter, worldStats, worldInfo);
			}
		} catch (Exception e) {
			String regionPath = region != null ? region.getRegionFile().getAbsolutePath() : "unknown region file";
			log.error("ChunkLocator: Error while trying to load chunk at ({}, {}) from {}", chunkCoord.x, chunkCoord.z, regionPath, e);
		}
		
		return chunk;
	}
	
	public ChunkCoord[] getAlphaChunkCoords() {
		List<ChunkCoord> coords = new ArrayList<>();
		
		File[] xDirs = baseDir.listFiles(File::isDirectory);
		if (xDirs == null)
			return coords.toArray(new ChunkCoord[0]);
		
		for (File xDir : xDirs) {
			File[] zDirs = xDir.listFiles(File::isDirectory);
			if (zDirs == null)
				continue;
			
			for (File zDir : zDirs) {
				File[] chunkFiles = zDir.listFiles((dir, name) -> name.startsWith("c.") && name.endsWith(".dat"));
				if (chunkFiles == null)
					continue;
				
				for (File chunkFile : chunkFiles) {
					ChunkCoord coord = parseChunkCoord(chunkFile);
					if (coord != null)
						coords.add(coord);
				}
			}
		}
		
		return coords.toArray(new ChunkCoord[0]);
	}
	
	private static ChunkCoord parseChunkCoord(File chunkFile) {
		Matcher matcher = CHUNK_FILE_PATTERN.matcher(chunkFile.getName());
		if (!matcher.matches())
			return null;
		
		try {
			long x = Util.fromBase36(matcher.group(1));
			long z = Util.fromBase36(matcher.group(2));
			return new ChunkCoord(x, z);
		} catch (Exception e) {
			log.warn("Could not parse chunk coordinate from {}", chunkFile.getAbsolutePath(), e);
			return null;
		}
	}
	
	public static File findChunkFile(File baseDir, ChunkCoord coord) {
		// world/0/0/c.0.0.dat
		
		String firstFrag = Util.toBase36((coord.x & 63));
		String secondFrag = Util.toBase36((coord.z & 63));
		
		File firstDir = new File(baseDir, firstFrag);
		File secondDir = new File(firstDir, secondFrag);
		
		String first = Util.toBase36(coord.x);
		String second = Util.toBase36(coord.z);
		
		return new File(secondDir, "c." + first + "." + second + ".dat");
	}
	
	public static File findRegionFile(File baseDir, RegionCoord coord, SaveFormat format) {
		// world/region/r.0.0.mca
		
		File regionDir = new File(baseDir, "region");
		
		String builder = "r." + coord.x + "." + coord.z + "." + format.extension;
		
		return new File(regionDir, builder);
	}
}
