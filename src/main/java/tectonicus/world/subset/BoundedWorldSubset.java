/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.world.filter.ArrayBlockFilter;

public interface BoundedWorldSubset extends WorldSubset {
	record Bounds(long minX, long minZ, long maxX, long maxZ) {}

	Bounds getBounds();

	String horizontalCacheKey();

	default boolean containsColumn(long x, long z) {
		return containsBlock(x, z);
	}

	@Override
	default RawChunk.BlockVisibility visibility(ChunkCoord coord) {
		long originX = coord.x * RawChunk.WIDTH;
		long originZ = coord.z * RawChunk.DEPTH;
		HeightRange range = getHeightRange();
		return (x, y, z) -> range.contains(y) && containsColumn(originX + x, originZ + z);
	}

	@Override
	ArrayBlockFilter getBlockFilter(ChunkCoord coord);
}
