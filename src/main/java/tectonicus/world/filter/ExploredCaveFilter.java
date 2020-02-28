/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.filter;

import tectonicus.BlockIds;
import tectonicus.CaveMaskFactory;
import tectonicus.raw.RawChunk;

public class ExploredCaveFilter implements BlockFilter
{
	private static String NON_NATURAL_BLOCKS_METADATA = "nonNaturalBlocks";
	
	private static final int[] NON_NATURAL_BLOCK_IDS =
	{
		BlockIds.TORCH,
		BlockIds.BED,
		BlockIds.MINECART_TRACKS,
		BlockIds.REDSTONE_WIRE
	};
	
	@Override
	public void filter(RawChunk rawChunk)
	{
		boolean[][][] nonNaturalBlocks = new boolean[RawChunk.WIDTH][RawChunk.HEIGHT][RawChunk.DEPTH];
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int y=0; y<RawChunk.HEIGHT; y++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					nonNaturalBlocks[x][y][z] = isNonNatural(rawChunk.getBlockId(x, y, z));
				}
			}
		}
		
		rawChunk.setFilterMetadata(NON_NATURAL_BLOCKS_METADATA, nonNaturalBlocks);
	}
	
	@Override
	public void preGeometryFilter(RawChunk center, RawChunk north, RawChunk south, RawChunk east, RawChunk west)
	{
		// TODO Auto-generated method stub
		// Build an uber array of all the adjacent blocks
		
		int[][][] distanceGrid = new int[RawChunk.WIDTH*3][RawChunk.HEIGHT][RawChunk.DEPTH*3];
		copy(distanceGrid, center,	RawChunk.WIDTH, 		RawChunk.DEPTH);
		copy(distanceGrid, north,	0,						RawChunk.DEPTH);
		copy(distanceGrid, south,	RawChunk.WIDTH * 2, 	RawChunk.DEPTH);
		copy(distanceGrid, east,	RawChunk.WIDTH,			0);
		copy(distanceGrid, west,	RawChunk.WIDTH,			RawChunk.DEPTH * 2);
		
		// Now diffuse the distances over the natural blocks
		final int numIterations = 8;
		for (int i=0; i<numIterations; i++)
		{
			diffuse(distanceGrid);
		}
		
	//	boolean[][][] nonNatural = (boolean[][][])center.getFilterMetadata();
		
		int[][] heights = CaveMaskFactory.calcSmoothedSurfaceHeights(center);
		
		// Now use the distance grid to replace everything out of range with stone
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int y=0; y<RawChunk.HEIGHT; y++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					final int actualX = x + RawChunk.WIDTH;
					final int actualZ = z + RawChunk.DEPTH;
					
					final int dist = distanceGrid[actualX][y][actualZ];
					if (dist < 10) // arbitrary threshold
					{
						if (y <= heights[x][z])
						{
							center.setBlockId(x, y, z, (byte)BlockIds.STONE);
						//	center.setBlockId(x, y, z, (byte)BlockIds.AIR);
						}
					}
					
				//	if (y == 127 || y == 126)
				//		center.setBlockId(x, y, z, (byte)BlockIds.AIR);
				}
			}
		}
	}
	
	private static void copy(int[][][] dest, RawChunk chunk, final int xOffset, final int zOffset)
	{
		if (chunk == null)
			return;
		
		boolean[][][] nonNatural = (boolean[][][])chunk.getFilterMetadata(NON_NATURAL_BLOCKS_METADATA);
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int y=0; y<RawChunk.HEIGHT; y++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					final int value = nonNatural[x][y][z] ? 16 : 0;
					dest[x+xOffset][y][z+zOffset] = value;
				}
			}
		}
	}
	
	private static void diffuse(int[][][] distanceGrid)
	{
		// Diffuse over all the internal blocks (ie. skip the outer edges)
		for (int x=1; x<distanceGrid.length-1; x++)
		{
			for (int y=1; y<distanceGrid[0].length-1; y++)
			{
				for (int z=1; z<distanceGrid[0][0].length-1; z++)
				{
					final int current = distanceGrid[x][y][z];
					final int north = distanceGrid[x-1][y][z];
					final int south = distanceGrid[x+1][y][z];
					final int east = distanceGrid[x][y][z-1];
					final int west = distanceGrid[x][y][z+1];
					final int above = distanceGrid[x][y+1][z];
					final int below = distanceGrid[x][y-1][z];
					
					final int adjacent = max(north, south, east, west, above, below) - 1;
					
					if (adjacent > current)
						distanceGrid[x][y][z] = adjacent;
				}
			}
		}
		
		
		// TODO: Given that these will be open air or adamantium, do we need to do these?
		
		// Diffuse the top layer
		// ..
		
		// Diffuse the bottom layer
		// ..
	}
	
	private static int max(final int v0, final int v1, final int v2, final int v3, final int v4, final int v5)
	{
		return Math.max(v0, Math.max(v1, Math.max(v2, Math.max(v3, Math.max(v4, v5)))));
	}
	
	private static boolean isNonNatural(final int blockId)
	{
		for (int i=0; i<NON_NATURAL_BLOCK_IDS.length; i++)
		{
			if (blockId == NON_NATURAL_BLOCK_IDS[i])
				return true;
		}
		return false;
	}
}
