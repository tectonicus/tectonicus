/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import org.h2.mvstore.MVMap;
import tectonicus.TileCoord;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A temporary disk-backed coordinate set owned by a factory. Not thread safe.
 * Iteration streams coordinates in Morton order; do not modify a list while iterating over it.
 */
public final class HddTileList implements Iterable<TileCoord>, AutoCloseable {
	private final HddTileListFactory factory;
	private final MVMap<Long, Boolean> tiles;
	private int minTileX = Integer.MAX_VALUE, maxTileX = Integer.MIN_VALUE;
	private int minTileY = Integer.MAX_VALUE, maxTileY = Integer.MIN_VALUE;

	HddTileList(HddTileListFactory factory, MVMap<Long, Boolean> tiles) {
		this.factory = factory;
		this.tiles = tiles;
	}

	@Override
	public Iterator<TileCoord> iterator() {
		checkOpen();
		Iterator<Long> keys = tiles.keyIterator(null);
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				checkOpen();
				return keys.hasNext();
			}

			@Override
			public TileCoord next() {
				checkOpen();
				return decode(keys.next());
			}
		};
	}

	/** Returns true only when this coordinate was not already present. */
	public boolean add(TileCoord coord) {
		checkOpen();
		if (tiles.putIfAbsent(encode(coord), Boolean.TRUE) != null)
			return false;
		minTileX = Math.min(coord.x, minTileX);
		maxTileX = Math.max(coord.x, maxTileX);
		minTileY = Math.min(coord.y, minTileY);
		maxTileY = Math.max(coord.y, maxTileY);
		return true;
	}

	public int size() {
		checkOpen();
		return tiles.size();
	}

	public boolean contains(TileCoord coord) {
		checkOpen();
		return tiles.containsKey(encode(coord));
	}

	/** Independent minimum x and y; this exact coordinate need not be present. */
	public TileCoord getAbsoluteMinCoord() {
		checkOpen();
		return new TileCoord(minTileX, minTileY);
	}

	/** Independent maximum x and y; this exact coordinate need not be present. */
	public TileCoord getAbsoluteMaxCoord() {
		checkOpen();
		return new TileCoord(maxTileX, maxTileY);
	}

	/** Materializes the entire list in heap memory. Use streaming iteration for large lists. */
	public Set<TileCoord> toSet() {
		Set<TileCoord> result = new HashSet<>();
		for (TileCoord coord : this)
			result.add(coord);
		return result;
	}

	@Override
	public void close() {
		if (!factory.isClosed() && !tiles.isClosed())
			factory.remove(tiles);
	}

	private void checkOpen() {
		if (factory.isClosed() || tiles.isClosed())
			throw new IllegalStateException("Tile list is closed");
	}

	// Bias signed coordinates before interleaving. Flip the final sign bit so signed long
	// comparison visits the unsigned Morton curve in order, including all four quadrants.
	static long encode(TileCoord coord) {
		return (spread(coord.x ^ Integer.MIN_VALUE) | (spread(coord.y ^ Integer.MIN_VALUE) << 1)) ^ Long.MIN_VALUE;
	}

	static TileCoord decode(long key) {
		long bits = key ^ Long.MIN_VALUE;
		return new TileCoord(compact(bits) ^ Integer.MIN_VALUE, compact(bits >>> 1) ^ Integer.MIN_VALUE);
	}

	private static long spread(int value) {
		long bits = Integer.toUnsignedLong(value);
		bits = (bits | bits << 16) & 0x0000FFFF0000FFFFL;
		bits = (bits | bits << 8) & 0x00FF00FF00FF00FFL;
		bits = (bits | bits << 4) & 0x0F0F0F0F0F0F0F0FL;
		bits = (bits | bits << 2) & 0x3333333333333333L;
		return (bits | bits << 1) & 0x5555555555555555L;
	}

	private static int compact(long bits) {
		bits &= 0x5555555555555555L;
		bits = (bits | bits >>> 1) & 0x3333333333333333L;
		bits = (bits | bits >>> 2) & 0x0F0F0F0F0F0F0F0FL;
		bits = (bits | bits >>> 4) & 0x00FF00FF00FF00FFL;
		bits = (bits | bits >>> 8) & 0x0000FFFF0000FFFFL;
		return (int)(bits | bits >>> 16);
	}
}
