/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.raw.RawChunk;

public class CaveMaskFactory implements BlockMaskFactory
{
	
	@Override
	public BlockMask createMask(ChunkCoord coord, RawChunk rawChunk)
	{
		int[][] heights = calcSmoothedSurfaceHeights(rawChunk);
		
		BlockMask mask = new BlockMask();
		mask.setAllVisible();
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				final int surfaceHeight = heights[x][z];
				
				for (int y = surfaceHeight; y< Minecraft.getChunkHeight(); y++)
				{
					mask.setVisible(x, y, z, false);
				}
			}
		}
		
		return mask;
	}
	
	public static int[][] calcSmoothedSurfaceHeights(RawChunk rawChunk)
	{
		int[][] heights = calcSurfaceHeights(rawChunk);
		return smoothSurfaceHeights(heights);
	}
	
	private static int[][] calcSurfaceHeights(RawChunk rawChunk)
	{
		int[][] heights = new int[RawChunk.WIDTH][RawChunk.DEPTH];
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				final int surfaceHeight = calcSurfaceHeight(rawChunk, x, z);
				heights[x][z] = surfaceHeight;
			}
		}
		
		return heights;
	}
	
	private static int[][] smoothSurfaceHeights(int[][] heights)
	{
		int[][] smoothedHeights = new int[RawChunk.WIDTH][RawChunk.DEPTH];
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				final int N = getHeight(x-1, z, heights);
				final int S = getHeight(x+1, z, heights);
				final int E = getHeight(x, z-1, heights);
				final int W = getHeight(1, z+1, heights);
				
				final int minHeight = Math.min(N, Math.min(E, Math.min(S, W)));
				
				smoothedHeights[x][z] = minHeight;
			}
		}
		
		return smoothedHeights;
	}
	
	// Calculate by casting a ray down vertically
	private static int calcSurfaceHeight(RawChunk rawChunk, final int x, final int z)
	{
		int penetration = 0;
		
		int y;
		for (y=Minecraft.getChunkHeight()-1; y>=0; y--)
		{
			final int blockId = rawChunk.getBlockId(x, y, z);
			if (blockId == BlockIds.AIR
				|| blockId == BlockIds.WATER
				|| blockId == BlockIds.STATIONARY_WATER
				|| blockId == BlockIds.LEAVES
				|| blockId == BlockIds.WOOD
				|| blockId == BlockIds.LOG
				|| blockId == BlockIds.SAPLING
				|| blockId == BlockIds.RED_FLOWER
				|| blockId == BlockIds.YELLOW_FLOWER
				|| blockId == BlockIds.RED_MUSHROOM
				|| blockId == BlockIds.BROWN_MUSHROOM
				)
			{
				// Probably an above surface block
				
				// Idea: reset penetration? Or penetration = Math.max(penetration-1, 0) ?
				
				// Decay penetration
				penetration = Math.max(penetration-2, 0);
			}
			else
			{
				penetration++;
			}
			
			if (penetration >= 5)
			{
				break;
			}
		}
		
		y = Math.max(y-2, 0);
		return y;
	}
	
	private static int getHeight(int x, int z, int[][] heights)
	{
		x = Math.max(x, 0);
		x = Math.min(x, RawChunk.WIDTH-1);
		
		z = Math.max(z, 0);
		z = Math.min(z, RawChunk.DEPTH-1);
		
		return heights[x][z];
	}
}
