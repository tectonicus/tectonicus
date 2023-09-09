/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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
import tectonicus.configuration.Map;
import tectonicus.renderer.OrthoCamera;
import tectonicus.world.World;

import java.io.File;

public class NullTileCache implements TileCache
{
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
	public HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles,
										RegionHashStore regionHashStore, World world, Map map, OrthoCamera camera, int zoom, int tileWidth, int tileHeight, File layerDir)
	{
		return visibleTiles;
	}

	@Override
	public void calculateDownsampledTileCoordinates(HddTileList baseTiles, int zoomLevel) {

	}

	@Override
	public HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat) {
		return null;
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
