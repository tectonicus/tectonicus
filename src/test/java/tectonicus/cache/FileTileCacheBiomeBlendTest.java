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
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.DimensionConfig;

import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class FileTileCacheBiomeBlendTest {
	@TempDir
	Path directory;

	@Test
	void changingTheRadiusInvalidatesTheTileCache() throws Exception {
		DimensionConfig map = new DimensionConfig("map");
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
		map.setBiomeBlendRadius(7);
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
		map.setBiomeBlendRadius(0);
		assertFalse(openAndCloseCache(map));
	}

	private boolean openAndCloseCache(DimensionConfig map) throws Exception {
		FileTileCache cache = new FileTileCache(directory.resolve("tiles").toFile(), ImageFormat.PNG,
				map, new LayerConfig("layer", map.getId()), "options", MessageDigest.getInstance("SHA-1"));
		try {
			return cache.isUsingExistingCache();
		} finally {
			cache.closeTileCache();
		}
	}

	@Test
	void changingAutomaticRadiusToExplicitDefaultInvalidatesTheTileCache() throws Exception {
		DimensionConfig map = new DimensionConfig("map");
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
		map.setBiomeBlendRadius(2);
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
	}

	@Test
	void changingLayerRenderingOverridesInvalidatesOnlyItsCache() throws Exception {
		DimensionConfig dimension = new DimensionConfig("map");
		LayerConfig layer = new LayerConfig("day", dimension.getId());
		assertFalse(openAndCloseCache(dimension, layer));
		assertTrue(openAndCloseCache(dimension, layer));
		layer.setSmoothLit(false);
		assertFalse(openAndCloseCache(dimension, layer));
		assertTrue(openAndCloseCache(dimension, layer));
		layer.setBiomeBlendRadius(0);
		assertFalse(openAndCloseCache(dimension, layer));
		assertTrue(openAndCloseCache(dimension, layer));
	}

	private boolean openAndCloseCache(DimensionConfig dimension, LayerConfig layer) throws Exception {
		FileTileCache cache = new FileTileCache(directory.resolve("tiles").toFile(), ImageFormat.PNG,
				dimension, layer, "options", MessageDigest.getInstance("SHA-1"));
		try {
			return cache.isUsingExistingCache();
		} finally {
			cache.closeTileCache();
		}
	}
}
