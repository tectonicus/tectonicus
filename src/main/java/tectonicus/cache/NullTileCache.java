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
import java.util.Map;

public class NullTileCache implements TileCache
{
	private final Map<Integer, HddTileList> downsampleTiles = new HashMap<>();
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
	public void calculateDownsampledTileCoordinates(HddTileListFactory factory, HddTileList baseTiles, int zoomLevel) {
		HddTileList previous = baseTiles;
		while (zoomLevel >= 0) {
			HddTileList parents = downsampleTiles.computeIfAbsent(zoomLevel, key -> factory.createList());
			for (TileCoord tile : previous)
				parents.add(new TileCoord(Math.floorDiv(tile.x, 2), Math.floorDiv(tile.y, 2)));
			previous = parents;
			zoomLevel--;
		}
	}

	@Override
	public HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat) {
		HddTileList result = factory.createList();
		HddTileList retained = downsampleTiles.get(zoomLevel);
		if (retained != null) {
			for (TileCoord tile : retained)
				result.add(tile);
		}
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
		for (HddTileList tiles : downsampleTiles.values())
			tiles.close();
		downsampleTiles.clear();
	}
}
