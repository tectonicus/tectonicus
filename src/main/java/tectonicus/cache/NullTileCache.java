/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import tectonicus.TileCoord;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.ImageFormat;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NullTileCache implements TileCache
{
	private final java.util.Map<Integer, Set<TileCoord>> downsampleTiles = new HashMap<>();
	@Override
	public void reset()
	{
		
	}

	@Override
	public boolean isUsingExistingCache()
	{
		return false;
	}

	@Override
	public boolean hasCreatedDownsampleCache()
	{
		return false;
	}
	
	@Override
	public HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles, TileHashCache tileHashes, File layerDir)
	{
		return visibleTiles;
	}

	@Override
	public void calculateDownsampledTileCoordinates(HddTileList baseTiles, int zoomLevel) {
		Set<TileCoord> previous = baseTiles.toSet();
		while (zoomLevel >= 0) {
			Set<TileCoord> next = new HashSet<>();
			for (TileCoord tile : previous)
				next.add(new TileCoord(Math.floorDiv(tile.x, 2), Math.floorDiv(tile.y, 2)));
			downsampleTiles.computeIfAbsent(zoomLevel--, key -> new HashSet<>()).addAll(next);
			previous = next;
		}
	}

	@Override
	public HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat) {
		HddTileList result = factory.createList();
		for (TileCoord tile : downsampleTiles.getOrDefault(zoomLevel, Set.of()))
			result.add(tile);
		return result;
	}

	@Override
	public void writeImageCache(TileCoord coord)
	{
		
	}

	@Override
	public void updateTileDownsampleStatus(TileCoord coord, int zoomLevel) {

	}

	@Override
	public void closeTileCache() {

	}
}
