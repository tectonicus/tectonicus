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
import tectonicus.chunk.ChunkCoord;
import tectonicus.util.Colour4f;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TintSampleCacheTest {
	private static final ChunkCoord CHUNK = new ChunkCoord(3, -7);

	/** Resolves a distinct colour per sample and counts how often each sample is resolved. */
	private static final class CountingResolver implements BiomeColorBlender.ColorResolver {
		final Map<Long, Integer> calls = new HashMap<>();
		final int seed;

		CountingResolver(int seed) {
			this.seed = seed;
		}

		@Override
		public Colour4f getColor(int x, int z) {
			calls.merge(((long) x << 32) | (z & 0xFFFFFFFFL), 1, Integer::sum);
			return new Colour4f(x, z, seed, 1);
		}
	}

	@Test
	void resolvesEachSampleOnceWhileAChunkIsBuilt() {
		TintSampleCache cache = new TintSampleCache();
		CountingResolver resolver = new CountingResolver(0);
		cache.begin(CHUNK);
		BiomeColorBlender.ColorResolver wrapped = cache.wrap(CHUNK, 64, TintSampleCache.GRASS, resolver);

		Map<Long, Colour4f> first = new HashMap<>();
		// Enough samples to grow the table several times, including negative chunk-relative positions.
		for (int z = -40; z < 56; z++)
			for (int x = -40; x < 56; x++)
				first.put(((long) x << 32) | (z & 0xFFFFFFFFL), wrapped.getColor(x, z));
		for (int z = -40; z < 56; z++)
			for (int x = -40; x < 56; x++)
				assertSame(first.get(((long) x << 32) | (z & 0xFFFFFFFFL)), wrapped.getColor(x, z));

		assertEquals(96 * 96, cache.size());
		assertEquals(96 * 96, resolver.calls.size());
		resolver.calls.values().forEach(count -> assertEquals(1, count.intValue()));
	}

	@Test
	void keepsSamplesOfDifferentHeightsAndKindsApart() {
		TintSampleCache cache = new TintSampleCache();
		cache.begin(CHUNK);
		Colour4f grass = cache.wrap(CHUNK, 64, TintSampleCache.GRASS, new CountingResolver(1)).getColor(5, 5);
		Colour4f foliage = cache.wrap(CHUNK, 64, TintSampleCache.FOLIAGE, new CountingResolver(2)).getColor(5, 5);
		Colour4f higher = cache.wrap(CHUNK, 65, TintSampleCache.GRASS, new CountingResolver(3)).getColor(5, 5);

		assertEquals(1, grass.b);
		assertEquals(2, foliage.b);
		assertEquals(3, higher.b);
		assertEquals(3, cache.size());
	}

	@Test
	void forgetsSamplesBetweenChunksAndPassesOtherChunksThrough() {
		TintSampleCache cache = new TintSampleCache();
		CountingResolver resolver = new CountingResolver(0);
		cache.begin(CHUNK);
		cache.wrap(CHUNK, 64, TintSampleCache.WATER, resolver).getColor(1, 2);
		assertSame(resolver, cache.wrap(new ChunkCoord(4, -7), 64, TintSampleCache.WATER, resolver));
		cache.end();
		assertEquals(0, cache.size());
		assertSame(resolver, cache.wrap(CHUNK, 64, TintSampleCache.WATER, resolver));

		cache.begin(CHUNK);
		cache.wrap(CHUNK, 64, TintSampleCache.WATER, resolver).getColor(1, 2);
		assertEquals(2, resolver.calls.get((1L << 32) | 2).intValue());
	}
}
