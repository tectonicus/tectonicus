/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.MutableLayer;
import tectonicus.configuration.MutableMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTileCacheDownsamplingTest {
	@TempDir
	Path directory;

	@Test
	void propagatesChangesToSharedParentsAndPreservesUnchangedBranches() throws Exception {
		FileTileCache cache = open();
		try {
			HddTileList tiles = new HddTileList(directory.resolve("base").toFile());
			for (TileCoord coord : Set.of(new TileCoord(4, -4), new TileCoord(5, -4),
					new TileCoord(4, -3), new TileCoord(5, -3), new TileCoord(8, 8))) {
				tiles.add(coord);
			}
			cache.calculateDownsampledTileCoordinates(tiles, 2);
			assertEquals(Set.of(new TileCoord(2, -2), new TileCoord(4, 4)), pending(cache, 2));
			assertEquals(Set.of(new TileCoord(1, -1), new TileCoord(2, 2)), pending(cache, 1));
			assertEquals(Set.of(new TileCoord(0, -1), new TileCoord(1, 1)), pending(cache, 0));
			for (int zoom = 0; zoom <= 2; zoom++) {
				for (TileCoord coord : pending(cache, zoom)) {
					createImage(coord, zoom);
					cache.updateTileDownsampleStatus(coord, zoom);
				}
				assertEquals(Set.of(), pending(cache, zoom));
			}
			HddTileList changed = new HddTileList(directory.resolve("changed").toFile());
			changed.add(new TileCoord(5, -3));
			cache.calculateDownsampledTileCoordinates(changed, 2);
			assertEquals(Set.of(new TileCoord(2, -2)), pending(cache, 2));
			assertEquals(Set.of(new TileCoord(1, -1)), pending(cache, 1));
			assertEquals(Set.of(new TileCoord(0, -1)), pending(cache, 0));
		} finally {
			cache.closeTileCache();
		}
	}

	private FileTileCache open() throws Exception {
		MutableMap map = new MutableMap("map");
		return new FileTileCache(directory.resolve("cache").toFile(), ImageFormat.PNG, map,
				new MutableLayer("day", map.getId()), "options", MessageDigest.getInstance("SHA-1"));
	}

	private Set<TileCoord> pending(FileTileCache cache, int zoom) {
		HddTileListFactory factory = new HddTileListFactory(directory.resolve("lists").toFile());
		return cache.findTilesForDownsampling(factory, zoom, directory.resolve("zoom" + zoom).toFile(), ImageFormat.PNG).toSet();
	}

	private void createImage(TileCoord coord, int zoom) throws Exception {
		Path image = TileRenderer.getImageFile(directory.resolve("zoom" + zoom).toFile(), coord.x, coord.y, ImageFormat.PNG).toPath();
		Files.createDirectories(image.getParent());
		Files.createFile(image);
	}
}
