/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import tectonicus.ChunkCoord;
import tectonicus.world.filter.NullBlockFilter;
import tectonicus.SaveFormat;
import tectonicus.world.World;
import tectonicus.world.filter.BlockFilter;

public class FullWorldSubset implements WorldSubset
{
	private final World world;
	
	public FullWorldSubset(World world)
	{
		this.world = world;
	}
	
	@Override
	public RegionIterator createRegionIterator(SaveFormat saveFormat)
	{
		return new AllRegionsIterator(world.getDimensionDir(), saveFormat);
	}
	
	@Override
	public boolean contains(ChunkCoord coord)
	{
		return true;
	}
	
	@Override
	public BlockFilter getBlockFilter(ChunkCoord coord)
	{
		return new NullBlockFilter();
	}
	
	@Override
	public String getDescription()
	{
		return "FullWorldSubset";
	}
}
