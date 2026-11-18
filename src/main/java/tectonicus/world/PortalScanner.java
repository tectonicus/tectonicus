/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.world;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import tectonicus.Minecraft;
import tectonicus.Portal;
import tectonicus.PortalColumn;
import tectonicus.RegionCache;
import tectonicus.RegionCoord;
import tectonicus.SaveFormat;
import tectonicus.Version;
import tectonicus.cache.BiomeCache;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkLocator;
import tectonicus.configuration.Dimension;
import tectonicus.world.filter.NullBlockFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static tectonicus.util.FindEntityUtil.findPortalColumns;

/** Reads portal geometry from saved chunks without applying map filters. Used after chunk discovery has finished. */
public class PortalScanner {
	@Getter
	private final SaveFormat format;
	private final Function<ChunkCoord, List<PortalColumn>> loader;
	private final Cache<ChunkCoord, List<PortalColumn>> chunks = Caffeine.newBuilder().maximumSize(1024).build();
	private final Cache<PortalColumn, Portal> completePortals = Caffeine.newBuilder().maximumSize(4096).build();

	public PortalScanner(Function<ChunkCoord, List<PortalColumn>> loader) {
		this(null, loader);
	}

	private PortalScanner(SaveFormat format, Function<ChunkCoord, List<PortalColumn>> loader) {
		this.format = format;
		this.loader = loader;
	}

	public static PortalScanner open(File directory, Version version, BiomeCache biomeCache, Dimension dimension) {
		return open(directory, version, biomeCache, format -> DimensionHeight.resolve(null, Map.of(), List.of(),
				dimension.getId(), dimension, version, format));
	}

	/** @param heightFor the dimension's vertical range for the save format found in {@code directory} */
	public static PortalScanner open(File directory, Version version, BiomeCache biomeCache, Function<SaveFormat, DimensionHeight> heightFor) {
		var regions = new RegionCache(directory);
		var locator = new ChunkLocator(biomeCache, regions, directory);
		DimensionHeight dimensionHeight = heightFor.apply(regions.getFormat());
		var worldInfo = new WorldInfo(version, dimensionHeight.minY() != 0, dimensionHeight.minY());
		return new PortalScanner(regions.getFormat(), coord -> {
			// RegionCache and ChunkLocator log read failures. Distinguish a failed read from an absent saved chunk.
			if (regions.getFormat() == SaveFormat.ALPHA) {
				File file = ChunkLocator.findChunkFile(directory, coord);
				if (file.exists() && file.length() == 0)
					throw new IllegalStateException("Empty portal chunk " + file);
			} else {
				var regionCoord = RegionCoord.fromChunkCoord(coord);
				File file = ChunkLocator.findRegionFile(directory, regionCoord, regions.getFormat());
				if (file.exists() && regions.getRegion(regionCoord) == null)
					throw new IllegalStateException("Could not read portal region " + file);
			}
			if (!locator.exists(coord))
				return List.of();
			DimensionHeight previousHeight = Minecraft.getDimensionHeight();
			Minecraft.setDimensionHeight(dimensionHeight);
			try {
				var chunk = locator.loadChunk(coord, new NullBlockFilter(), null, worldInfo);
				if (chunk == null || chunk.getRawChunk() == null || !chunk.getRawChunk().isFullChunk())
					throw new IllegalStateException("Could not read portal chunk " + coord + " in " + directory);
				return findPortalColumns(chunk.getRawChunk());
			} finally {
				Minecraft.setDimensionHeight(previousHeight);
			}
		});
	}

	public List<PortalColumn> columns(ChunkCoord coord) {
		return chunks.get(coord, loader);
	}

	private List<PortalColumn> columns(long x, long z) {
		return columns(new ChunkCoord(Math.floorDiv(x, 16), Math.floorDiv(z, 16))).stream()
				.filter(column -> column.getX() == x && column.getZ() == z).toList();
	}

	public Portal complete(PortalColumn seed) {
		Portal cached = completePortals.getIfPresent(seed);
		if (cached != null)
			return cached;
		PortalColumn first = columns(seed.getX(), seed.getZ()).stream()
				.filter(column -> column.getMinY() <= seed.getMinY() && column.getMaxY() >= seed.getMaxY())
				.findFirst().orElseThrow(() -> new IllegalStateException("Portal blocks missing from saved chunk"));
		var visited = new HashSet<PortalColumn>();
		var connected = new ArrayList<PortalColumn>();
		visited.add(first);
		connected.add(first);
		for (int i = 0; i < connected.size(); i++) {
			PortalColumn column = connected.get(i);
			long x = column.getX();
			long z = column.getZ();
			var neighbors = new ArrayList<PortalColumn>();
			if (column.getAxis() != PortalColumn.Axis.Z) {
				neighbors.addAll(columns(x - 1, z));
				neighbors.addAll(columns(x + 1, z));
			}
			if (column.getAxis() != PortalColumn.Axis.X) {
				neighbors.addAll(columns(x, z - 1));
				neighbors.addAll(columns(x, z + 1));
			}
			for (PortalColumn neighbor : neighbors) {
				if (Portal.connects(column, neighbor) && visited.add(neighbor))
					connected.add(neighbor);
			}
		}
		Portal portal = Portal.groupColumns(connected).getFirst();
		for (PortalColumn column : connected)
			completePortals.put(column, portal);
		completePortals.put(seed, portal);
		return portal;
	}

	public Portal findExit(Portal.EntryPosition entry, double scale, PortalSearch rules) {
		double exitX = entry.x() * scale;
		double exitZ = entry.z() * scale;
		boolean centers = rules.mode() == PortalSearch.Mode.COLUMN_CENTERS;
		boolean bottoms = centers || rules.mode() == PortalSearch.Mode.COLUMN_BLOCKS;
		long x = rules.mode() == PortalSearch.Mode.COLUMN_BLOCKS ? (long) exitX : (long) Math.floor(exitX);
		long z = rules.mode() == PortalSearch.Mode.COLUMN_BLOCKS ? (long) exitZ : (long) Math.floor(exitZ);
		boolean chunkSearch = rules.mode() == PortalSearch.Mode.CHUNKS;
		long minChunkX = chunkSearch ? Math.floorDiv(x, 16) - rules.radius() : Math.floorDiv(x - rules.radius(), 16);
		long maxChunkX = chunkSearch ? Math.floorDiv(x, 16) + rules.radius() : Math.floorDiv(x + rules.radius(), 16);
		long minChunkZ = chunkSearch ? Math.floorDiv(z, 16) - rules.radius() : Math.floorDiv(z - rules.radius(), 16);
		long maxChunkZ = chunkSearch ? Math.floorDiv(z, 16) + rules.radius() : Math.floorDiv(z + rules.radius(), 16);
		double bestDistance = Double.POSITIVE_INFINITY;
		int bestY = Integer.MAX_VALUE;
		var bestColumns = new ArrayList<PortalColumn>();
		for (long chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (long chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				for (PortalColumn column : columns(new ChunkCoord(chunkX, chunkZ))) {
					if (!chunkSearch && (Math.abs(column.getX() - x) > rules.radius() || Math.abs(column.getZ() - z) > rules.radius()))
						continue;
					if (bottoms && (column.getMinY() < 0 || column.getMinY() > rules.maxY()))
						continue;
					int y = bottoms ? column.getMinY() : Math.clamp(entry.y(), column.getMinY(), column.getMaxY());
					double dx = centers ? column.getX() + 0.5 - exitX : column.getX() - x;
					double dy = centers ? y + 0.5 - entry.y() : y - entry.y();
					double dz = centers ? column.getZ() + 0.5 - exitZ : column.getZ() - z;
					double distance = dx * dx + dy * dy + dz * dz;
					int comparison = Double.compare(distance, bestDistance);
					if (comparison == 0 && !bottoms)
						comparison = Integer.compare(y, bestY);
					if (comparison < 0) {
						bestDistance = distance;
						bestY = y;
						bestColumns.clear();
					}
					if (comparison <= 0)
						bestColumns.add(column);
				}
			}
		}
		Portal destination = null;
		for (PortalColumn column : bestColumns) {
			Portal candidate = complete(column);
			if (destination != null && !destination.id().equals(candidate.id()))
				return null;
			destination = candidate;
		}
		return destination;
	}
}
