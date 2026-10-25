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
import tectonicus.configuration.MutableLayer;
import tectonicus.configuration.MutableMap;

import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class FileTileCacheBiomeBlendTest {
	@TempDir
	Path directory;

	@Test
	void changingTheRadiusInvalidatesTheTileCache() throws Exception {
		MutableMap map = new MutableMap("map");
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
		map.setBiomeBlendRadius(7);
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
		map.setBiomeBlendRadius(0);
		assertFalse(openAndCloseCache(map));
	}

	private boolean openAndCloseCache(MutableMap map) throws Exception {
		FileTileCache cache = new FileTileCache(directory.resolve("tiles").toFile(), ImageFormat.PNG,
				map, new MutableLayer("layer", map.getId()), "options", MessageDigest.getInstance("SHA-1"));
		try {
			return cache.isUsingExistingCache();
		} finally {
			cache.closeTileCache();
		}
	}

	@Test
	void changingAutomaticRadiusToExplicitDefaultInvalidatesTheTileCache() throws Exception {
		MutableMap map = new MutableMap("map");
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
		map.setBiomeBlendRadius(2);
		assertFalse(openAndCloseCache(map));
		assertTrue(openAndCloseCache(map));
	}
}
