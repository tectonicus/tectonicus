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

public interface TileCache {
	void reset();
	
	boolean isUsingExistingCache();
	boolean hasCreatedDownsampleCache();
	
	/** Takes a set of visible output tiles and strips it down to only the tiles
	 *  that actually need rendering.
	 *  The returned list can alias visibleTiles when caching is disabled.
	 */
	HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles, TileHashCache tileHashes, File layerDir);

	/** Borrows baseTiles and uses factory for disk-backed parent-coordinate sets. */
	void calculateDownsampledTileCoordinates(HddTileListFactory factory, HddTileList baseTiles, int zoomLevel);
	/** Returns an independent temporary list which the caller must close. */
	HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat);

	void writeImageCache(TileCoord coord);
	void updateTileDownsampleStatus(TileCoord coord, int zoomLevel);

	default void writeImageCache(TileCoord coord, boolean empty) {
		writeImageCache(coord);
	}

	default void updateTileDownsampleStatus(TileCoord coord, int zoomLevel, boolean empty) {
		updateTileDownsampleStatus(coord, zoomLevel);
	}

	void closeTileCache();
}
