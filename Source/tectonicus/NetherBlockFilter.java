/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.raw.RawChunk;
import tectonicus.world.filter.BlockFilter;

public class NetherBlockFilter implements BlockFilter
{
	@Override
	public void filter(RawChunk rawChunk)
	{
		final int NETHER_ROOF = 127;
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				final int roofId = rawChunk.getBlockId(x, NETHER_ROOF, z);
				if (roofId == BlockIds.ADAMANTIUM)
				{
					rawChunk.setBlockId(x, NETHER_ROOF, z, BlockIds.AIR);
					// remove the mushrooms
					final int roof1Id = rawChunk.getBlockId(x, NETHER_ROOF+1, z);
					if (roof1Id == BlockIds.RED_MUSHROOM || roof1Id == BlockIds.BROWN_MUSHROOM)
					{
						rawChunk.setBlockId(x, NETHER_ROOF+1, z, BlockIds.AIR);
					}
				}
				
				for (int y=NETHER_ROOF-1; y>=0; y--)
				{
					final int id = rawChunk.getBlockId(x, y, z);
					if (id == BlockIds.ADAMANTIUM || id == BlockIds.NETHERSTONE)
					{
						rawChunk.setBlockId(x, y, z, BlockIds.AIR);
					}
					else
					{
						break;
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
