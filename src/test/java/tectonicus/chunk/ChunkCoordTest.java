/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.chunk;

import org.junit.jupiter.api.Test;
import tectonicus.RegionCoord;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkCoordTest {
	@Test
	void equalCoordsHaveEqualHashes() {
		assertEquals(new ChunkCoord(-12, 345), new ChunkCoord(-12, 345));
		assertEquals(new ChunkCoord(-12, 345).hashCode(), new ChunkCoord(-12, 345).hashCode());
		assertNotEquals(new ChunkCoord(1, 2), new ChunkCoord(2, 1));
		assertNotEquals(new ChunkCoord(1, 2), "1,2");
	}

	@Test
	void nearbyChunksSpreadAcrossHashBuckets() {
		// Index bits a hash table of 4096 buckets would use after Java's hash spreading.
		Set<Integer> buckets = new HashSet<>();
		for (long x = 0; x < 64; x++) {
			for (long z = 0; z < 32; z++) {
				int h = new ChunkCoord(x, z).hashCode();
				buckets.add((h ^ (h >>> 16)) & 4095);
			}
		}
		assertTrue(buckets.size() > 1400, "only " + buckets.size() + " buckets used by 2048 chunks");
	}

	@Test
	void regionOfChunkUsesFloorDivision() {
		assertEquals(new RegionCoord(-1, 0), RegionCoord.fromChunkCoord(new ChunkCoord(-1, 31)));
		assertEquals(new RegionCoord(-2, 1), RegionCoord.fromChunkCoord(new ChunkCoord(-33, 32)));
		assertEquals(new RegionCoord(58593, -58594), RegionCoord.fromChunkCoord(new ChunkCoord(1874999, -1874999)));
		// Beyond float precision the old conversion could pick the wrong region.
		assertEquals(new RegionCoord(524288, 0), RegionCoord.fromChunkCoord(new ChunkCoord(16777247, 0)));
	}
}
