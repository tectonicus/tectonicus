/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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

public interface WorldSubset
{
	public RegionIterator createRegionIterator(SaveFormat format);

	public boolean contains(ChunkCoord coord);

	public BlockFilter getBlockFilter(ChunkCoord coord);

	public String getDescription();
}
