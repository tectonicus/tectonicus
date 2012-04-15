/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
