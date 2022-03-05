/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import tectonicus.ChunkCoord;
import tectonicus.SaveFormat;
import tectonicus.world.filter.BlockFilter;

import java.io.File;

public interface WorldSubset {
	RegionIterator createRegionIterator(SaveFormat format, File dimensionDir);

	boolean contains(ChunkCoord coord);

	boolean containsBlock(double x, double z);

	BlockFilter getBlockFilter(ChunkCoord coord);
}
