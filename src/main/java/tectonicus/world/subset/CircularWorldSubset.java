/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import lombok.Data;
import tectonicus.chunk.ChunkCoord;
import tectonicus.SaveFormat;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.ArrayBlockFilter;
import tectonicus.world.filter.BlockFilter;

import java.io.File;

@Data
public class CircularWorldSubset implements WorldSubset
{
	private final Vector3l origin;
	private final long radius;

	@Override
	public RegionIterator createRegionIterator(SaveFormat saveFormat, File dimensionDir)
	{
		return new CircularRegionIterator(dimensionDir, saveFormat, origin, radius);
	}
	
	@Override
	public boolean contains(ChunkCoord coord)
	{
		Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + RawChunk.WIDTH/2, 0, coord.z * RawChunk.DEPTH + RawChunk.DEPTH/2);
		final long dist = pos.separation(origin);
		
		final float bufferedRadius = radius + (float)(RawChunk.WIDTH + RawChunk.DEPTH + RawChunk.WIDTH);
		
		return dist <= bufferedRadius;
	}

	@Override
	public boolean containsBlock(double x, double z) {
		return Math.pow((x - origin.x), 2) + Math.pow((z - origin.z), 2) < Math.pow(radius,2);
	}
	
	@Override
	public BlockFilter getBlockFilter(ChunkCoord coord)
	{
		ArrayBlockFilter filter = new ArrayBlockFilter();
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				final long dist = distance(coord, x, z, origin);
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
