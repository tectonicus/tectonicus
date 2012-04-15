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
