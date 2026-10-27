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
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.MutableLayer;
import tectonicus.configuration.MutableMap;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyTileCacheTest {
	@TempDir Path directory;
	private final TileCoord tile = new TileCoord(-5, 3);

	@Test
	void emptyBaseTilesSurviveReopeningButChangedHashesRenderAgain() throws Exception {
		HddTileListFactory factory = new HddTileListFactory(directory.resolve("lists").toFile());
		HddTileList visible = factory.createList();
		visible.add(tile);
		FileTileCache cache = open();
		try (TileHashCache hashes = new TileHashCache(null, coord -> new byte[] {1})) {
			assertEquals(Set.of(tile), cache.findChangedTiles(factory, visible, hashes, directory.toFile()).toSet());
			cache.writeImageCache(tile, true);
		} finally { cache.closeTileCache(); }
		cache = open();
		try (TileHashCache hashes = new TileHashCache(null, coord -> new byte[] {1})) {
			assertEquals(Set.of(), cache.findChangedTiles(factory, visible, hashes, directory.toFile()).toSet());
		} finally { cache.closeTileCache(); }
		cache = open();
		try (TileHashCache hashes = new TileHashCache(null, coord -> new byte[] {2})) {
			assertEquals(Set.of(tile), cache.findChangedTiles(factory, visible, hashes, directory.toFile()).toSet());
			cache.writeImageCache(tile, false);
			// A real image which failed to write is still retried, even after being empty before.
			assertEquals(Set.of(tile), cache.findChangedTiles(factory, visible, hashes, directory.toFile()).toSet());
		} finally { cache.closeTileCache(); }
	}

	@Test
	void emptyParentsAreCompletedUntilAChildChanges() throws Exception {
		HddTileListFactory factory = new HddTileListFactory(directory.resolve("lists").toFile());
		HddTileList children = factory.createList();
		children.add(new TileCoord(tile.x * 2, tile.y * 2));
		FileTileCache cache = open();
		try {
			cache.calculateDownsampledTileCoordinates(children, 1);
			cache.updateTileDownsampleStatus(tile, 1, true);
			assertEquals(Set.of(), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
		} finally { cache.closeTileCache(); }
		cache = open();
		try {
			assertEquals(Set.of(), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
			cache.calculateDownsampledTileCoordinates(children, 1);
			assertEquals(Set.of(tile), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
			cache.updateTileDownsampleStatus(tile, 1, false);
			assertEquals(Set.of(tile), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
		} finally { cache.closeTileCache(); }
	}

	@Test
	void uncachedRendersStillCalculateNegativeParentCoordinates() {
		HddTileListFactory factory = new HddTileListFactory(directory.resolve("lists").toFile());
		HddTileList children = factory.createList();
		children.add(new TileCoord(-5, 3));
		NullTileCache cache = new NullTileCache();
		cache.calculateDownsampledTileCoordinates(children, 1);
		assertEquals(Set.of(new TileCoord(-3, 1)), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
		assertEquals(Set.of(new TileCoord(-2, 0)), cache.findTilesForDownsampling(factory, 0, directory.toFile(), ImageFormat.PNG).toSet());
	}

	private FileTileCache open() throws Exception {
		MutableMap map = new MutableMap("map");
		return new FileTileCache(directory.resolve("cache").toFile(), ImageFormat.PNG, map,
				new MutableLayer("day", map.getId()), "empty-test", MessageDigest.getInstance("SHA-1"));
	}
}
