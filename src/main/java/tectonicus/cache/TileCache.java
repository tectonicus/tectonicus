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

public interface TileCache {
	void reset();
	
	boolean isUsingExistingCache();
	boolean hasCreatedDownsampleCache();
	
	/** Takes a set of visible output tiles and strips it down to only the tiles
	 *  that actually need rendering.
	 */
	HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles, RegionHashStore regionHashStore, World world, Map map, OrthoCamera camera, final int zoom, final int tileWidth, final int tileHeight, File layerDir);

	void calculateDownsampledTileCoordinates(HddTileList baseTiles, int zoomLevel);
	HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat);

	void writeImageCache(TileCoord coord);
	void updateTileDownsampleStatus(TileCoord coord, int zoomLevel);

	void closeTileCache();
}
