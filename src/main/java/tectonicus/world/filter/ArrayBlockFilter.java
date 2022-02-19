/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.filter;

import tectonicus.BlockIds;
import tectonicus.Minecraft;
import tectonicus.raw.RawChunk;

public class ArrayBlockFilter implements BlockFilter
{

	private boolean[][] collumns;
	
	public ArrayBlockFilter()
	{
		collumns = new boolean[RawChunk.WIDTH][RawChunk.DEPTH];
	}
	
	public void set(final int x, final int z, final boolean allow)
	{
		collumns[x][z] = allow;
	}
	
	@Override
	public void filter(RawChunk rawChunk)
	{
		for (int x=0; x<collumns.length; x++)
		{
			for (int z=0; z<collumns[0].length; z++)
			{
				if (!collumns[x][z])
				{
					for (int y = 0; y< Minecraft.getChunkHeight(); y++)
					{
						rawChunk.setBlockId(x, y, z, (byte)BlockIds.AIR);
						rawChunk.setSkyLight(x, y, z, (byte)16);
					}
				}
			}
		}
	}
	
	@Override
	public void preGeometryFilter(RawChunk center, RawChunk north, RawChunk south, RawChunk east, RawChunk west)
	{
		
	}
	
}
