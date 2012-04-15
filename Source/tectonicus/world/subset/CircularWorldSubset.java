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
package tectonicus.world.subset;

import tectonicus.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.World;
import tectonicus.world.filter.BlockFilter;

public class CircularWorldSubset implements WorldSubset
{
	private World world;
	private Vector3l origin;
	private long radius;
	
	public CircularWorldSubset(World world, Vector3l origin, final long radius)
	{
		this.world = world;
		if (origin != null)
			this.origin = new Vector3l(origin);
		this.radius = radius;
	}
	
	@Override
	public String getDescription()
	{
		String originStr = "null";
		if (origin != null)
			originStr = "("+origin.x+", "+origin.z+")";
		
		return "CircularWorldSubset "+originStr+" "+ radius;
	}
	
	@Override
	public RegionIterator createRegionIterator()
	{
		Vector3l actualOrigin = origin != null ? origin : world.getLevelDat().getSpawnPosition();
		return new CircularRegionIterator(world.getDimensionDir(), actualOrigin, radius);
	}
	
	@Override
	public boolean contains(ChunkCoord coord)
	{
		Vector3l actualOrigin = origin != null ? origin : world.getLevelDat().getSpawnPosition();
		
		Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + RawChunk.WIDTH/2, 0, coord.z * RawChunk.DEPTH + RawChunk.DEPTH/2);
		final long dist = pos.separation(actualOrigin);
		
		final float bufferedRadius = radius + (RawChunk.WIDTH + RawChunk.DEPTH + RawChunk.WIDTH);
		
		return dist <= bufferedRadius;
	}
	
	@Override
	public BlockFilter getBlockFilter(ChunkCoord coord)
	{
		ArrayBlockFilter filter = new ArrayBlockFilter();
		
		Vector3l actualOrigin = origin != null ? origin : world.getLevelDat().getSpawnPosition();
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				final long dist = distance(coord, x, z, actualOrigin); 
				final boolean allow = dist <= radius;
				filter.set(x, z, allow);
			}
		}
		
		return filter;
	}
	
	public static long distance(ChunkCoord coord, final long offsetX, final long offsetZ, Vector3l origin)
	{
		final long x = coord.x * RawChunk.WIDTH + offsetX;
		final long z = coord.z * RawChunk.DEPTH + offsetZ;
		
		final long dx = x - origin.x;
		final long dz = z - origin.z;
		
		return (long)Math.sqrt(dx*dx + dz*dz);
	}
}
