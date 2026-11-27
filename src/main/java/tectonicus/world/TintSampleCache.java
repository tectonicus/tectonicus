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

import java.util.Arrays;

/**
 * Remembers biome tint samples while one chunk's geometry is built.
 * <p>
 * Each tinted face averages a square of samples around it (25 with the default blend radius), and each sample
 * may need Minecraft's seeded Voronoi biome lookup. Neighbouring blocks share most of their samples and a
 * block often has several tinted faces, so without this the same sample is computed many times over.
 * A sample's colour depends only on its position, its kind and the loaded chunks, none of which change while
 * a chunk is built, so reusing it gives exactly the same result. Samples are kept for one chunk only.
 * Each geometry worker owns its cache.
 * <p>
 * Samples are kept in an open-addressing table keyed by the packed position rather than a {@code HashMap<Long, ...>}:
 * {@link Long#hashCode()} folds the packed coordinates onto each other, so neighbouring samples collided into the
 * same buckets, and every lookup boxed its key.
 */
final class TintSampleCache {
	static final int GRASS = 0;
	static final int FOLIAGE = 1;
	static final int DRY_FOLIAGE = 2;
	static final int WATER = 4;

	private static final int INITIAL_CAPACITY = 1024;

	private ChunkCoord chunk;
	// A slot is empty while its colour is null. The table grows to fit the largest chunk and is reused.
	private long[] keys = new long[INITIAL_CAPACITY];
	private Colour4f[] colours = new Colour4f[INITIAL_CAPACITY];
	private int size;

	/** Starts remembering samples for the chunk whose geometry is about to be built. */
	void begin(ChunkCoord chunk) {
		this.chunk = chunk;
		clear();
	}

	/** Stops remembering and forgets all samples. */
	void end() {
		chunk = null;
		clear();
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
			long key = key(sampleX, y, sampleZ, kind);
			Colour4f colour = get(key);
			if (colour == null) {
				colour = resolver.getColor(sampleX, sampleZ);
				putAbsent(key, colour);
			}
			return colour;
		};
	}

	/** Unique for chunk-relative positions within ±32768, which covers any blend radius and world height. */
	static long key(int x, int y, int z, int kind) {
		return (x & 0xFFFFL) | (z & 0xFFFFL) << 16 | (y & 0xFFFFL) << 32 | (long) kind << 48;
	}

	int size() {
		return size;
	}

	private Colour4f get(long key) {
		int mask = keys.length - 1;
		for (int i = slot(key, mask); ; i = (i + 1) & mask) {
			Colour4f colour = colours[i];
			if (colour == null || keys[i] == key)
				return colour;
		}
	}

	/** Adds a key that {@link #get} did not find. A null colour is not kept, so it is resolved again next time. */
	private void putAbsent(long key, Colour4f colour) {
		if (colour == null)
			return;
		if ((size + 1) * 4L > keys.length * 3L)
			grow();
		insert(keys, colours, key, colour);
		size++;
	}

	private void grow() {
		long[] oldKeys = keys;
		Colour4f[] oldColours = colours;
		keys = new long[oldKeys.length * 2];
		colours = new Colour4f[oldColours.length * 2];
		for (int i = 0; i < oldKeys.length; i++) {
			if (oldColours[i] != null)
				insert(keys, colours, oldKeys[i], oldColours[i]);
		}
	}

	private static void insert(long[] keys, Colour4f[] colours, long key, Colour4f colour) {
		int mask = keys.length - 1;
		int i = slot(key, mask);
		while (colours[i] != null)
			i = (i + 1) & mask;
		keys[i] = key;
		colours[i] = colour;
	}

	private void clear() {
		if (size > 0) {
			Arrays.fill(colours, null);
			size = 0;
		}
	}

	/** Mixes all bits of the key (MurmurHash3's finalizer), so nearby positions spread over the table. */
	static int slot(long key, int mask) {
		key ^= key >>> 33;
		key *= 0xff51afd7ed558ccdL;
		key ^= key >>> 33;
		key *= 0xc4ceb9fe1a85ec53L;
		key ^= key >>> 33;
		return (int) key & mask;
	}
}
