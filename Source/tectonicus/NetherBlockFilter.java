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
