/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.cache;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.DimensionConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTileCacheDownsamplingTest {
	@AutoClose
	private HddTileListFactory factory;

	@TempDir
	Path directory;

	@BeforeEach
	void setUpTileLists() {
		factory = new HddTileListFactory(directory.resolve("lists").toFile());
	}

	@Test
	void propagatesChangesToSharedParentsAndPreservesUnchangedBranches() throws Exception {
		FileTileCache cache = open();
		try {
			HddTileList tiles = factory.createList();
			for (TileCoord coord : Set.of(new TileCoord(4, -4), new TileCoord(5, -4),
					new TileCoord(4, -3), new TileCoord(5, -3), new TileCoord(8, 8))) {
				tiles.add(coord);
			}
			cache.calculateDownsampledTileCoordinates(factory, tiles, 2);
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
			HddTileList changed = factory.createList();
			changed.add(new TileCoord(5, -3));
			cache.calculateDownsampledTileCoordinates(factory, changed, 2);
			assertEquals(Set.of(new TileCoord(2, -2)), pending(cache, 2));
			assertEquals(Set.of(new TileCoord(1, -1)), pending(cache, 1));
			assertEquals(Set.of(new TileCoord(0, -1)), pending(cache, 0));
		} finally {
			cache.closeTileCache();
		}
	}

	@Test
	void reopensWithOnlyUnfinishedParentsPendingAfterInterruption() throws Exception {
		HddTileList children = factory.createList();
		children.add(new TileCoord(4, 4));
		children.add(new TileCoord(8, 8));
		FileTileCache cache = open();
		try {
			cache.calculateDownsampledTileCoordinates(factory, children, 1);
			cache.updateTileDownsampleStatus(new TileCoord(2, 2), 1, true);
		} finally {
			cache.closeTileCache();
		}
		cache = open();
		try {
			assertEquals(Set.of(new TileCoord(4, 4)), pending(cache, 1));
			assertEquals(Set.of(new TileCoord(1, 1), new TileCoord(2, 2)), pending(cache, 0));
		} finally {
			cache.closeTileCache();
		}
	}

	private FileTileCache open() throws Exception {
		DimensionConfig map = new DimensionConfig("map");
		return new FileTileCache(directory.resolve("cache").toFile(), ImageFormat.PNG, map,
				new LayerConfig("day", map.getId()), "options", MessageDigest.getInstance("SHA-1"));
	}

	private Set<TileCoord> pending(FileTileCache cache, int zoom) {
		try (HddTileList pending = cache.findTilesForDownsampling(factory, zoom, directory.resolve("zoom" + zoom).toFile(), ImageFormat.PNG)) {
			return pending.toSet();
		}
	}

	private void createImage(TileCoord coord, int zoom) throws Exception {
		Path image = TileRenderer.getImageFile(directory.resolve("zoom" + zoom).toFile(), coord.x, coord.y, ImageFormat.PNG).toPath();
		Files.createDirectories(image.getParent());
		Files.createFile(image);
	}
}
