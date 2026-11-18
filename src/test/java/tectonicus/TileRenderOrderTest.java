/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileRenderOrderTest {
	@Test
	void visitsEveryTileOnceInColumnsWithinEachBlock(@TempDir Path tempDir) {
		try (HddTileListFactory factory = new HddTileListFactory(tempDir.toFile());
			 HddTileList tiles = factory.createList()) {
			Set<TileCoord> expected = new HashSet<>();
			for (int x = -150; x < 150; x += 3) {
				for (int y = -90; y < 200; y += 2) {
					TileCoord tile = new TileCoord(x, y);
					tiles.add(tile);
					expected.add(tile);
				}
			}

			List<TileCoord> ordered = new ArrayList<>();
			for (TileCoord tile : new TileRenderOrder(tiles))
				ordered.add(tile);

			assertEquals(expected.size(), ordered.size());
			assertEquals(expected, new HashSet<>(ordered));

			Set<String> finishedBlocks = new HashSet<>();
			for (int i = 1; i < ordered.size(); i++) {
				TileCoord previous = ordered.get(i - 1);
				TileCoord current = ordered.get(i);
				if (TileRenderOrder.sameBlock(previous, current)) {
					boolean columnOrder = previous.x < current.x || previous.x == current.x && previous.y < current.y;
					assertTrue(columnOrder, previous + " before " + current);
				} else {
					// Each block is rendered in one run.
					assertTrue(finishedBlocks.add(blockKey(previous)), "block revisited: " + blockKey(previous));
					assertFalse(finishedBlocks.contains(blockKey(current)), "block revisited: " + blockKey(current));
				}
			}
		}
	}

	@Test
	void emptyListHasNoTiles() {
		assertFalse(new TileRenderOrder(List.of()).iterator().hasNext());
	}

	private static String blockKey(TileCoord tile) {
		return (tile.x >> TileRenderOrder.BLOCK_SHIFT) + "," + (tile.y >> TileRenderOrder.BLOCK_SHIFT);
	}
}
