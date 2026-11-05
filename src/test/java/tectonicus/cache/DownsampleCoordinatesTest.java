/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.TileCoord;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LayerConfig;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownsampleCoordinatesTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void deduplicatesSignedParentsAndMergesSuccessiveInputs(boolean cached) throws Exception {
		try (HddTileListFactory factory = new HddTileListFactory(directory.resolve("lists").toFile())) {
			TileCache cache = open(cached);
			try {
				HddTileList base = factory.createList();
				for (TileCoord tile : Set.of(new TileCoord(-5, 3), new TileCoord(-6, 2),
						new TileCoord(16777219, -16777219), new TileCoord(Integer.MIN_VALUE, Integer.MAX_VALUE),
						new TileCoord(Integer.MAX_VALUE, Integer.MIN_VALUE)))
					base.add(tile);
				cache.calculateDownsampledTileCoordinates(factory, base, 1);
				HddTileList removed = factory.createList();
				removed.add(new TileCoord(-200, 200));
				cache.calculateDownsampledTileCoordinates(factory, removed, 1);
				cache.calculateDownsampledTileCoordinates(factory, removed, 1);
				assertPending(factory, cache, 1, Set.of(new TileCoord(-3, 1), new TileCoord(8388609, -8388610),
						new TileCoord(-1073741824, 1073741823), new TileCoord(1073741823, -1073741824),
						new TileCoord(-100, 100)));
				Set<TileCoord> topLevel = Set.of(new TileCoord(-2, 0), new TileCoord(4194304, -4194305),
						new TileCoord(-536870912, 536870911), new TileCoord(536870911, -536870912), new TileCoord(-50, 50));
				assertPending(factory, cache, 0, topLevel);
				assertPending(factory, cache, 0, topLevel); // Closing a result must not discard retained parents.
			} finally {
				cache.closeTileCache();
			}
		}
	}

	private TileCache open(boolean cached) throws Exception {
		if (!cached)
			return new NullTileCache();
		DimensionConfig map = new DimensionConfig("map");
		return new FileTileCache(directory.resolve("cache").toFile(), ImageFormat.PNG, map,
				new LayerConfig("day", map.getId()), "coordinates", MessageDigest.getInstance("SHA-1"));
	}

	private void assertPending(HddTileListFactory factory, TileCache cache, int zoom, Set<TileCoord> expected) {
		try (HddTileList pending = cache.findTilesForDownsampling(factory, zoom, directory.toFile(), ImageFormat.PNG)) {
			assertEquals(expected, pending.toSet());
			assertEquals(expected.size(), pending.size());
		}
	}
}
