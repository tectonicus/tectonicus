/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LightStyle;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.DimensionConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FileTileCacheSharedHashesTest {
	@TempDir
	Path directory;
	private final DimensionConfig map = new DimensionConfig("map");
	private final LayerConfig day = new LayerConfig("day", map.getId());
	private final LayerConfig night = new LayerConfig("night", map.getId());
	private final TileCoord first = new TileCoord(1, -2);
	private final TileCoord second = new TileCoord(-3, 4);
	private HddTileListFactory factory;
	private HddTileList visible;

	@BeforeEach
	void setup() {
		night.setLightStyle(LightStyle.Night);
		factory = new HddTileListFactory(directory.resolve("lists").toFile());
		visible = factory.createList();
		visible.add(first);
		visible.add(second);
	}

	@Test
	void sharesCalculationsButKeepsEachLayersSavedHashesIndependent() throws Exception {
		prime(day, Set.of(first, second));
		prime(night, Set.of(first)); // The second image exists, but its render was never committed.
		AtomicInteger calculations = new AtomicInteger();
		try (TileHashCache hashes = currentHashes(calculations, (byte) 1)) {
			assertEquals(Set.of(), changed(day, hashes));
			assertEquals(Set.of(second), changed(night, hashes));
			assertEquals(2, calculations.get());
		}
	}

	@Test
	void aMissingImageOnlyInvalidatesItsOwnLayer() throws Exception {
		prime(day, Set.of(first, second));
		prime(night, Set.of(first, second));
		Files.delete(image(night, first));
		AtomicInteger calculations = new AtomicInteger();
		try (TileHashCache hashes = currentHashes(calculations, (byte) 1)) {
			assertEquals(Set.of(), changed(day, hashes));
			assertEquals(Set.of(first), changed(night, hashes));
			assertEquals(2, calculations.get());
		}
	}

	@Test
	void changedLayerOptionsOnlyInvalidateThatLayer() throws Exception {
		prime(day, Set.of(first, second));
		prime(night, Set.of(first, second));
		night.setImageCompressionLevel(0.5f);
		try (TileHashCache hashes = currentHashes(new AtomicInteger(), (byte) 1)) {
			assertEquals(Set.of(), changed(day, hashes));
			assertEquals(Set.of(first, second), changed(night, hashes));
		}
	}

	@Test
	void displayNameChangesKeepBothLayersCached() throws Exception {
		prime(day, Set.of(first, second));
		prime(night, Set.of(first, second));
		try (TileHashCache hashes = currentHashes(new AtomicInteger(), (byte) 1)) {
			map.setName("Renamed dimension");
			assertEquals(Set.of(), changed(day, hashes));
			assertEquals(Set.of(), changed(night, hashes));

			day.setName("Renamed day layer");
			assertEquals(Set.of(), changed(day, hashes));
			assertEquals(Set.of(), changed(night, hashes));
		}
	}

	@Test
	void aWorldChangeInvalidatesBothLayersOnTheNextRender() throws Exception {
		prime(day, Set.of(first, second));
		prime(night, Set.of(first, second));
		AtomicInteger calculations = new AtomicInteger();
		try (TileHashCache hashes = currentHashes(calculations, (byte) 2)) {
			assertEquals(Set.of(first, second), changed(day, hashes));
			assertEquals(Set.of(first, second), changed(night, hashes));
			assertEquals(2, calculations.get());
		}
	}

	@Test
	void disabledCachingDoesNotCalculateHashes() {
		try (TileHashCache hashes = new TileHashCache(null, tile -> fail("Caching is disabled"))) {
			assertSame(visible, new NullTileCache().findChangedTiles(factory, visible, hashes, directory.toFile()));
		}
	}

	private TileHashCache currentHashes(AtomicInteger calculations, byte version) {
		return new TileHashCache(directory.resolve("current.cache").toFile(), tile -> {
			calculations.incrementAndGet();
			return new byte[]{version, (byte) tile.x, (byte) tile.y};
		});
	}

	private FileTileCache open(LayerConfig layer) throws Exception {
		return new FileTileCache(directory.resolve("saved").resolve(layer.getId()).toFile(), ImageFormat.PNG,
				map, layer, "options", MessageDigest.getInstance("SHA-1"));
	}

	private File output(LayerConfig layer) {
		return directory.resolve("images").resolve(layer.getId()).toFile();
	}

	private Path image(LayerConfig layer, TileCoord tile) {
		return TileRenderer.getImageFile(output(layer), tile.x, tile.y, ImageFormat.PNG).toPath();
	}

	private void prime(LayerConfig layer, Set<TileCoord> completed) throws Exception {
		FileTileCache cache = open(layer);
		try (TileHashCache hashes = currentHashes(new AtomicInteger(), (byte) 1)) {
			HddTileList changed = cache.findChangedTiles(factory, visible, hashes, output(layer));
			assertEquals(Set.of(first, second), changed.toSet());
			for (TileCoord tile : changed) {
				Path image = image(layer, tile);
				Files.createDirectories(image.getParent());
				Files.createFile(image);
				if (completed.contains(tile))
					cache.writeImageCache(tile);
			}
		} finally {
			cache.closeTileCache();
		}
	}

	private Set<TileCoord> changed(LayerConfig layer, TileHashCache hashes) throws Exception {
		FileTileCache cache = open(layer);
		try {
			return cache.findChangedTiles(factory, visible, hashes, output(layer)).toSet();
		} finally {
			cache.closeTileCache();
		}
	}
}
