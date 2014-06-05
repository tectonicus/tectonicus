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

public class BlockMask
{
	private boolean[][][] mask;
	
	public BlockMask()
	{
		mask = new boolean[RawChunk.WIDTH][RawChunk.HEIGHT][RawChunk.DEPTH];
		setAllVisible();
	}
	
	public void setAllVisible()
	{
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int y=0; y<RawChunk.HEIGHT; y++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					mask[x][y][z] = true;
				}
			}
		}
	}

	public void setAllInvisible()
	{
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int y=0; y<RawChunk.HEIGHT; y++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					mask[x][y][z] = false;
				}
			}
		}
	}
	
	public void setVisible(final int x, final int y, final int z, final boolean isVisible)
	{
		assert (x >= 0 && x < RawChunk.WIDTH);
		assert (y >= 0 && y < RawChunk.HEIGHT);
		assert (z >= 0 && z < RawChunk.DEPTH);
		
		mask[x][y][z] = isVisible;
	}
	
	public boolean isVisible(final int x, final int y, final int z)
	{
		assert (x >= 0 && x < RawChunk.WIDTH);
		assert (y >= 0 && y < RawChunk.HEIGHT);
		assert (z >= 0 && z < RawChunk.DEPTH);
		
		return mask[x][y][z];
	}
}
