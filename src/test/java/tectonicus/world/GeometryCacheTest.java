/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import org.junit.jupiter.api.Test;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static tectonicus.world.GeometryCache.MB;

class GeometryCacheTest {
	@Test
	void unknownVideoMemoryUsesDefault() {
		assertEquals(GeometryCache.DEFAULT_VERTEX_MEMORY_BYTES, GeometryCache.chooseVertexMemoryBytes(-1, -1, null));
		assertEquals(GeometryCache.DEFAULT_VERTEX_MEMORY_BYTES, GeometryCache.chooseVertexMemoryBytes(0, 500 * MB, ""));
	}

	@Test
	void shareOfVideoMemoryWhenPlentyIsFree() {
		assertEquals((long) (8192 * MB * GeometryCache.VIDEO_MEMORY_SHARE),
				GeometryCache.chooseVertexMemoryBytes(8192 * MB, 8000 * MB, null));
		assertEquals((long) (8192 * MB * GeometryCache.VIDEO_MEMORY_SHARE),
				GeometryCache.chooseVertexMemoryBytes(8192 * MB, -1, null));
	}

	@Test
	void leavesReserveOfFreeVideoMemory() {
		// 2 GB card with 1 GB free: the reserve is 15% of 2 GB.
		long reserve = (long) (2048 * MB * GeometryCache.VIDEO_MEMORY_RESERVE_SHARE);
		assertEquals(1024 * MB - reserve, GeometryCache.chooseVertexMemoryBytes(2048 * MB, 1024 * MB, null));
	}

	@Test
	void staysWithinLimits() {
		assertEquals(GeometryCache.MIN_VERTEX_MEMORY_BYTES, GeometryCache.chooseVertexMemoryBytes(2048 * MB, 100 * MB, null));
		assertEquals(GeometryCache.MAX_VERTEX_MEMORY_BYTES, GeometryCache.chooseVertexMemoryBytes(48 * 1024 * MB, -1, null));
	}

	@Test
	void overrideWins() {
		assertEquals(300 * MB, GeometryCache.chooseVertexMemoryBytes(8192 * MB, 100 * MB, " 300 "));
	}

	@Test
	void invalidOverrideIsIgnored() {
		long expected = GeometryCache.chooseVertexMemoryBytes(8192 * MB, -1, null);
		assertEquals(expected, GeometryCache.chooseVertexMemoryBytes(8192 * MB, -1, "lots"));
		assertEquals(expected, GeometryCache.chooseVertexMemoryBytes(8192 * MB, -1, "-5"));
	}

	@Test
	void cacheBudgetLeavesRoomInVertexMemory() {
		assertTrue(GeometryCache.cacheBudgetBytes(1000 * MB) < 1000 * MB);
	}

	@Test
	void weightRoundsUpToKilobytesWithFloor() {
		assertEquals(GeometryCache.MIN_WEIGHT_KB, GeometryCache.weightKb(0));
		assertEquals(GeometryCache.MIN_WEIGHT_KB, GeometryCache.weightKb(1024));
		assertEquals(2049, GeometryCache.weightKb(2 * MB + 1));
		assertEquals(Integer.MAX_VALUE, GeometryCache.weightKb(Long.MAX_VALUE / 2));
	}

	@Test
	void weightedCacheStaysWithinBudgetAndUnloadsEvictedChunks() {
		final long maxWeight = 100;
		List<Chunk> unloaded = new ArrayList<>();
		ChunkCache cache = new ChunkCache(maxWeight, (coord, chunk) -> 10, coord -> new Chunk(coord, null)) {
			@Override
			protected void unloadInvalidatedChunk(Chunk chunk) {
				unloaded.add(chunk);
			}
		};

		for (int i = 0; i < 50; i++)
			cache.get(new ChunkCoord(i, 0));
		cache.unloadInvalidatedChunks();

		assertTrue(cache.weightedSize().isPresent());
		assertTrue(cache.weightedSize().getAsLong() <= maxWeight);
		assertEquals(50, cache.size() + unloaded.size());
		for (Chunk chunk : unloaded)
			assertNull(cache.getIfPresent(chunk.getCoord()));
	}

	@Test
	void loweringWeightLimitEvictsChunks() {
		ChunkCache cache = new ChunkCache(1000, (coord, chunk) -> 10, coord -> new Chunk(coord, null));
		for (int i = 0; i < 50; i++)
			cache.get(new ChunkCoord(i, 0));
		cache.unloadInvalidatedChunks();
		assertEquals(500, cache.weightedSize().getAsLong());

		cache.chunks.policy().eviction().orElseThrow().setMaximum(200);
		cache.unloadInvalidatedChunks();
		assertTrue(cache.weightedSize().getAsLong() <= 200);
	}

	@Test
	void countLimitedCacheHasNoWeight() {
		ChunkCache cache = new ChunkCache(10, coord -> new Chunk(coord, null));
		assertTrue(cache.weightedSize().isEmpty());
	}
}
