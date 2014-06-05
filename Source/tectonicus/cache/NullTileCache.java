/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import java.io.File;

import tectonicus.TileCoord;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.Map;
import tectonicus.renderer.OrthoCamera;
import tectonicus.world.World;

public class NullTileCache implements TileCache
{
	public void reset()
	{
		
	}
	
	public boolean isUsingExistingCache()
	{
		return false;
	}
	
	@Override
	public HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles,
										RegionHashStore regionHashStore, World world, Map map, OrthoCamera camera, int zoom, int tileWidth, int tileHeight, File layerDir)
	{
		return visibleTiles;
	}
	
	public void writeImageCache(TileCoord coord)
	{
		
	}
}
