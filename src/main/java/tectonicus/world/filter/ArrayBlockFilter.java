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
import tectonicus.raw.PaintingEntity;
import tectonicus.raw.RawChunk;

import java.util.List;

public class ArrayBlockFilter implements BlockFilter
{

	private final boolean[][] columns;
	
	public ArrayBlockFilter()
	{
		columns = new boolean[RawChunk.WIDTH][RawChunk.DEPTH];
	}
	
	public void set(final int x, final int z, final boolean allow)
	{
		columns[x][z] = allow;
	}
	
	@Override
	public void filter(RawChunk rawChunk)
	{
		for (int x = 0; x< columns.length; x++)
		{
			for (int z = 0; z< columns[0].length; z++)
			{
				if (!columns[x][z])
				{
					for (int y = 0; y< Minecraft.getChunkHeight(); y++)
					{
						rawChunk.setBlockId(x, y, z, (byte)BlockIds.AIR);
						rawChunk.setSkyLight(x, y, z, (byte)16);
					}
				}
			}
		}

		List<PaintingEntity> paintings = rawChunk.getPaintings();
		paintings.removeIf(p -> !columns[p.getLocalX()][p.getLocalZ()]);
		List<PaintingEntity> itemFrames = rawChunk.getItemFrames();
		itemFrames.removeIf(i -> !columns[i.getLocalX()][i.getLocalZ()]);
	}
	
	@Override
	public void preGeometryFilter(RawChunk center, RawChunk north, RawChunk south, RawChunk east, RawChunk west)
	{
		
	}
	
}
