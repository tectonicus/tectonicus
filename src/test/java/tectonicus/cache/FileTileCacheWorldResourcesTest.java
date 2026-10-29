/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.WorldConfig;

import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class FileTileCacheWorldResourcesTest {
	@TempDir
	Path directory;

	@Test
	void togglingProgrammerArtInvalidatesOnlyTheAffectedWorld() throws Exception {
		Configuration config = new Configuration();
		DimensionConfig first = addWorld(config, "World0");
		DimensionConfig second = addWorld(config, "World1");
		assertFalse(openCache(config, first));
		assertFalse(openCache(config, second));
		assertTrue(openCache(config, first));
		assertTrue(openCache(config, second));

		first.getWorld().setUsingProgrammerArt(true);
		assertFalse(openCache(config, first));
		assertTrue(openCache(config, second));
		assertTrue(openCache(config, first));
		first.getWorld().setUsingProgrammerArt(false);
		assertFalse(openCache(config, first));
	}

	@Test
	void globalProgrammerArtIsHashedForAnUnattachedDimension() {
		Configuration config = new Configuration();
		String before = FileTileCache.calcOptionsString(config, null);
		config.setUsingProgrammerArt(true);
		assertNotEquals(before, FileTileCache.calcOptionsString(config, null));
	}

	private DimensionConfig addWorld(Configuration config, String id) {
		WorldConfig world = new WorldConfig(id, directory.resolve(id).toFile());
		DimensionConfig dimension = new DimensionConfig(id + ".Dimension0");
		dimension.addLayer(new LayerConfig(id + ".Layer0", dimension.getId()));
		world.addDimension(dimension);
		config.addWorld(world);
		return dimension;
	}

	private boolean openCache(Configuration config, DimensionConfig dimension) throws Exception {
		LayerConfig layer = dimension.getLayer(0);
		FileTileCache cache = new FileTileCache(directory.resolve(dimension.getId()).toFile(), layer.getImageFormat(),
				dimension, layer, FileTileCache.calcOptionsString(config, dimension), MessageDigest.getInstance("SHA-1"));
		try {
			return cache.isUsingExistingCache();
		} finally {
			cache.closeTileCache();
		}
	}
}
