/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.raw.RawChunk;
import tectonicus.world.filter.BlockFilter;

public class NetherBlockFilter113 implements BlockFilter
{
	@Override
	public void filter(RawChunk rawChunk)
	{
		final int NETHER_ROOF = 127;
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				for (int y=NETHER_ROOF; y>=0; y--)
				{
					final Block block = Block.byName(rawChunk.getBlockName(x, y, z));
					if (block == null) {
						break;
					}
					if (block == Block.BEDROCK || block == Block.NETHERRACK || block == Block.NETHER_QUARTZ_ORE
							|| block == Block.NETHER_GOLD_ORE || block == Block.BLACKSTONE || block == Block.SOUL_SAND
							|| block == Block.SOUL_SOIL || block == Block.ANCIENT_DEBRIS)
					{
						rawChunk.setBlockName(x, y, z, Block.AIR.getName());
					} else if (block == Block.LAVA) {
						Block belowBlock = Block.byName(rawChunk.getBlockName(x, y-1, z));
						if (belowBlock != Block.LAVA) {
							rawChunk.setBlockName(x, y, z, Block.AIR.getName());
						} else {
							break;
						}
					} else if (block == Block.BASALT && y > 65) {
						rawChunk.setBlockName(x, y, z, Block.AIR.getName());
					} else {
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
