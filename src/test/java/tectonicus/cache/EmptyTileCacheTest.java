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

import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyTileCacheTest {
	@AutoClose
	private HddTileListFactory factory;

	@TempDir Path directory;
	private final TileCoord tile = new TileCoord(-5, 3);

	@BeforeEach
	void setUpTileLists() {
		factory = new HddTileListFactory(directory.resolve("lists").toFile());
	}

	@Test
	void emptyBaseTilesSurviveReopeningButChangedHashesRenderAgain() throws Exception {
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
		HddTileList children = factory.createList();
		children.add(new TileCoord(tile.x * 2, tile.y * 2));
		FileTileCache cache = open();
		try {
			cache.calculateDownsampledTileCoordinates(factory, children, 1);
			cache.updateTileDownsampleStatus(tile, 1, true);
			assertEquals(Set.of(), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
		} finally { cache.closeTileCache(); }
		cache = open();
		try {
			assertEquals(Set.of(), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
			cache.calculateDownsampledTileCoordinates(factory, children, 1);
			assertEquals(Set.of(tile), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
			cache.updateTileDownsampleStatus(tile, 1, false);
			assertEquals(Set.of(tile), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
		} finally { cache.closeTileCache(); }
	}

	@Test
	void selectsPendingAndMissingParentsButSkipsCompletedEmptyAndOtherZooms() throws Exception {
		HddTileList children = factory.createList();
		children.add(new TileCoord(tile.x * 2, tile.y * 2));
		TileCoord completedEmpty = new TileCoord(2, 3);
		TileCoord completedImage = new TileCoord(4, 5);
		TileCoord missingImage = new TileCoord(6, 7);
		TileCoord otherZoom = new TileCoord(8, 9);
		FileTileCache cache = open();
		try {
			cache.updateTileDownsampleStatus(tile, 1, true);
			cache.calculateDownsampledTileCoordinates(factory, children, 1);
			cache.updateTileDownsampleStatus(completedEmpty, 1, true);
			cache.updateTileDownsampleStatus(completedImage, 1, false);
			cache.updateTileDownsampleStatus(missingImage, 1, false);
			cache.updateTileDownsampleStatus(otherZoom, 10, false);
			Path image = TileRenderer.getImageFile(directory.toFile(), completedImage.x, completedImage.y, ImageFormat.PNG).toPath();
			Files.createDirectories(image.getParent());
			Files.createFile(image);
			assertEquals(Set.of(tile, missingImage), cache.findTilesForDownsampling(factory, 1, directory.toFile(), ImageFormat.PNG).toSet());
			assertEquals(Set.of(otherZoom), cache.findTilesForDownsampling(factory, 10, directory.toFile(), ImageFormat.PNG).toSet());
		} finally { cache.closeTileCache(); }
	}

	private FileTileCache open() throws Exception {
		DimensionConfig map = new DimensionConfig("map");
		return new FileTileCache(directory.resolve("cache").toFile(), ImageFormat.PNG, map,
				new LayerConfig("day", map.getId()), "empty-test", MessageDigest.getInstance("SHA-1"));
	}
}
