/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.BlockFilter;

import java.io.File;

public interface WorldSubset {
	RegionIterator createRegionIterator(SaveFormat format, File dimensionDir);

	boolean contains(ChunkCoord coord);

	boolean containsBlock(double x, double z);

	default HeightRange getHeightRange() {
		return HeightRange.UNBOUNDED;
	}

	default boolean hasHeightBounds() {
		return getHeightRange().isBounded();
	}

	default boolean containsBlock(double x, double y, double z) {
		if (!hasHeightBounds())
			return containsBlock(x, z);
		return getHeightRange().contains(Math.floor(y)) && containsBlock(Math.floor(x), Math.floor(z));
	}

	/** Horizontal filtering for saved views and configurations without height bounds. */
	BlockFilter getBlockFilter(ChunkCoord coord);

	default BlockFilter getRenderBlockFilter(ChunkCoord coord) {
		if (!hasHeightBounds())
			return getBlockFilter(coord);
		return new BlockFilter() {
			@Override
			public void filter(RawChunk chunk) {
				chunk.filterBlocks(visibility(coord));
			}

			@Override
			public void preGeometryFilter(RawChunk center, RawChunk north, RawChunk south, RawChunk east, RawChunk west) {}
		};
	}

	default RawChunk.BlockVisibility visibility(ChunkCoord coord) {
		long originX = coord.x * RawChunk.WIDTH;
		long originZ = coord.z * RawChunk.DEPTH;
		return (x, y, z) -> containsBlock(originX + x, y, originZ + z);
	}
	
	Vector3l getOrigin();

	default boolean needsOrigin() {
		return getOrigin() == null;
	}
	
	void setOrigin(Vector3l origin);
}
