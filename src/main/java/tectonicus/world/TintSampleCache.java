/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import tectonicus.chunk.ChunkCoord;
import tectonicus.util.Colour4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers biome tint samples while one chunk's geometry is built.
 * <p>
 * Each tinted face averages a square of samples around it (25 with the default blend radius), and each sample
 * may need Minecraft's seeded Voronoi biome lookup. Neighbouring blocks share most of their samples and a
 * block often has several tinted faces, so without this the same sample is computed many times over.
 * A sample's colour depends only on its position, its kind and the loaded chunks, none of which change while
 * a chunk is built, so reusing it gives exactly the same result. Samples are kept for one chunk only.
 * Not thread safe: geometry is built on the rendering thread.
 */
final class TintSampleCache {
	static final int GRASS = 0;
	static final int FOLIAGE = 1;
	static final int DRY_FOLIAGE = 2;
	static final int WATER = 4;

	private ChunkCoord chunk;
	private final Map<Long, Colour4f> colours = new HashMap<>();

	/** Starts remembering samples for the chunk whose geometry is about to be built. */
	void begin(ChunkCoord chunk) {
		this.chunk = chunk;
		colours.clear();
	}

	/** Stops remembering and forgets all samples. */
	void end() {
		chunk = null;
		colours.clear();
	}

	static int plantKind(boolean isFoliage, boolean isDryFoliage) {
		return (isFoliage ? FOLIAGE : 0) | (isDryFoliage ? DRY_FOLIAGE : 0);
	}

	/**
	 * Wraps a resolver for samples around a block of {@code coord} at height {@code y}. Samples are relative to
	 * that chunk. Outside a chunk build, or for another chunk, the resolver is returned unchanged.
	 */
	BiomeColorBlender.ColorResolver wrap(ChunkCoord coord, int y, int kind, BiomeColorBlender.ColorResolver resolver) {
		if (chunk == null || !chunk.equals(coord))
			return resolver;
		return (sampleX, sampleZ) -> {
			Long key = key(sampleX, y, sampleZ, kind);
			Colour4f colour = colours.get(key);
			if (colour == null) {
				colour = resolver.getColor(sampleX, sampleZ);
				colours.put(key, colour);
			}
			return colour;
		};
	}

	/** Unique for chunk-relative positions within ±32768, which covers any blend radius and world height. */
	static long key(int x, int y, int z, int kind) {
		return (x & 0xFFFFL) | (z & 0xFFFFL) << 16 | (y & 0xFFFFL) << 32 | (long) kind << 48;
	}

	int size() {
		return colours.size();
	}
}
