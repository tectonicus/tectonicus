/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reorders base tiles so that consecutive tiles share most of their chunks.
 * <p>
 * Tile lists are stored in Morton order. Vertically adjacent tiles see almost the same chunks,
 * because each chunk column projects to a tall strip, but Morton order alternates between
 * horizontal and vertical steps, so the geometry cache rebuilds the same chunks many times.
 * Within each aligned block of {@value #BLOCK_SIZE}x{@value #BLOCK_SIZE} tiles this renders
 * column by column instead, and the blocks themselves keep their Morton order.
 * <p>
 * Each block is contiguous in the Morton order of the input, so tiles are streamed one block at a time
 * and at most {@code BLOCK_SIZE * BLOCK_SIZE} tiles are held in memory.
 */
public final class TileRenderOrder implements Iterable<TileCoord> {
	static final int BLOCK_SHIFT = 5;
	static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;

	private static final Comparator<TileCoord> COLUMN_ORDER =
			Comparator.<TileCoord>comparingInt(t -> t.x).thenComparingInt(t -> t.y);

	private final Iterable<TileCoord> mortonOrdered;

	/** @param mortonOrdered tiles in Morton order, such as an {@link tectonicus.cache.swap.HddTileList} */
	public TileRenderOrder(Iterable<TileCoord> mortonOrdered) {
		this.mortonOrdered = mortonOrdered;
	}

	@Override
	public Iterator<TileCoord> iterator() {
		Iterator<TileCoord> source = mortonOrdered.iterator();
		return new Iterator<>() {
			private final List<TileCoord> block = new ArrayList<>();
			private int position;
			private TileCoord pending;

			@Override
			public boolean hasNext() {
				if (position < block.size())
					return true;
				fillNextBlock();
				return position < block.size();
			}

			@Override
			public TileCoord next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return block.get(position++);
			}

			private void fillNextBlock() {
				block.clear();
				position = 0;
				TileCoord first = pending != null ? pending : (source.hasNext() ? source.next() : null);
				pending = null;
				if (first == null)
					return;
				block.add(first);
				while (source.hasNext()) {
					TileCoord tile = source.next();
					if (!sameBlock(first, tile)) {
						pending = tile;
						break;
					}
					block.add(tile);
				}
				block.sort(COLUMN_ORDER);
			}
		};
	}

	static boolean sameBlock(TileCoord a, TileCoord b) {
		// Arithmetic shift is floor division, so negative coordinates group correctly.
		return (a.x >> BLOCK_SHIFT) == (b.x >> BLOCK_SHIFT) && (a.y >> BLOCK_SHIFT) == (b.y >> BLOCK_SHIFT);
	}
}
