/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import tectonicus.ChunkCoord;
import tectonicus.SaveFormat;
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
	public RegionIterator createRegionIterator(SaveFormat saveFormat)
	{
		Vector3l actualOrigin = origin != null ? origin : world.getLevelDat().getSpawnPosition();
		return new CircularRegionIterator(world.getDimensionDir(), saveFormat, actualOrigin, radius);
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
