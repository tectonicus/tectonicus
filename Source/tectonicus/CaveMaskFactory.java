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
				
				for (int y=surfaceHeight; y<RawChunk.HEIGHT; y++)
				{
					mask.setVisible(x, y, z, false);
				}
			}
		}
		
		return mask;
	}
	
	public static int[][] calcSmoothedSurfaceHeights(RawChunk rawChunk)
	{
		int heights[][] = calcSurfaceHeights(rawChunk);
		int smoothedHeights[][] = smoothSurfaceHeights(heights);
		return smoothedHeights;
	}
	
	private static int[][] calcSurfaceHeights(RawChunk rawChunk)
	{
		int heights[][] = new int[RawChunk.WIDTH][RawChunk.DEPTH];
		
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
		int smoothedHeights[][] = new int[RawChunk.WIDTH][RawChunk.DEPTH];
		
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
		for (y=RawChunk.HEIGHT-1; y>=0; y--)
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
