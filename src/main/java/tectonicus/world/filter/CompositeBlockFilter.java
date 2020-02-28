/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.filter;

import java.util.ArrayList;

import tectonicus.raw.RawChunk;

public class CompositeBlockFilter implements BlockFilter
{
	private ArrayList<BlockFilter> filters;
	
	public CompositeBlockFilter()
	{
		this.filters = new ArrayList<BlockFilter>();
	}
	
	public void add(BlockFilter newFilter)
	{
		if (newFilter != null)
			filters.add(newFilter);
	}
	
	@Override
	public void filter(RawChunk rawChunk)
	{
		for (BlockFilter f : filters)
		{
			f.filter(rawChunk);
		}
	}
	
	public void preGeometryFilter(RawChunk center, RawChunk north, RawChunk south, RawChunk east, RawChunk west)
	{
		for (BlockFilter f : filters)
		{
			f.preGeometryFilter(center, north, south, east, west);
		}
	}
	
}
