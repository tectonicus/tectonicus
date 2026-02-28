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
import tectonicus.util.Vector3l;
import tectonicus.world.filter.BlockFilter;
import tectonicus.world.filter.NullBlockFilter;

import java.io.File;

public class FullWorldSubset implements WorldSubset {
	@Override
	public RegionIterator createRegionIterator(SaveFormat saveFormat, File dimensionDir) {
		return new AllRegionsIterator(dimensionDir, saveFormat);
	}

	@Override
	public boolean contains(ChunkCoord coord) {
		return true;
	}

	@Override
	public boolean containsBlock(double x, double z) {
		return true;
	}

	@Override
	public BlockFilter getBlockFilter(ChunkCoord coord) {
		return new NullBlockFilter();
	}
	
	@Override
	public Vector3l getOrigin() {
		return null;
	}
	
	@Override
	public void setOrigin(Vector3l origin) {
		// No-op since it isn't needed for a full world subset
	}
	
	@Override
	public String toString() {
		return "FullWorldSubset";
	}
}