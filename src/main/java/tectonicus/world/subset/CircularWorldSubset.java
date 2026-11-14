/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import lombok.AllArgsConstructor;
import lombok.Data;
import tectonicus.chunk.ChunkCoord;
import tectonicus.SaveFormat;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.ArrayBlockFilter;

import java.io.File;

@Data
@AllArgsConstructor
public class CircularWorldSubset implements BoundedWorldSubset
{
	private static final long CHUNK_BUFFER = RawChunk.WIDTH + RawChunk.DEPTH + RawChunk.WIDTH;

	private Vector3l origin;
	private final long radius;

	@Override
	public RegionIterator createRegionIterator(SaveFormat saveFormat, File dimensionDir)
	{
		// Rendering includes neighboring chunks for geometry at the circle's edge.
		// Preprocessing must visit their regions too, so every rendered chunk has a hash.
		return new CircularRegionIterator(dimensionDir, saveFormat, origin, radius + CHUNK_BUFFER);
	}
	
	@Override
	public boolean contains(ChunkCoord coord)
	{
		final long dist = distance(coord, RawChunk.WIDTH / 2, RawChunk.DEPTH / 2, origin);
		
		final long bufferedRadius = radius + CHUNK_BUFFER;
		
		return dist <= bufferedRadius;
	}

	@Override
	public boolean containsBlock(double x, double z) {
		return Double.isFinite(x) && Double.isFinite(z)
				&& containsColumn((long) Math.floor(x), (long) Math.floor(z));
	}

	@Override
	public boolean containsColumn(long x, long z) {
		return distance(x, z, origin) <= radius;
	}
	
	@Override
	public Bounds getBounds() {
		return new Bounds(origin.x - radius, origin.z - radius, origin.x + radius, origin.z + radius);
	}

	@Override
	public ArrayBlockFilter getBlockFilter(ChunkCoord coord)
	{
		ArrayBlockFilter filter = new ArrayBlockFilter(coord, this::getBlockFilter);
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				filter.set(x, z, containsColumn(coord.x * RawChunk.WIDTH + x, coord.z * RawChunk.DEPTH + z));
			}
		}
		
		return filter;
	}
        
        @Override
        public String toString() {
                return String.format("CircularWorldSubset@[%d,%d,%d],r%d", origin.x, origin.y, origin.z, radius);
        }
	
	public static long distance(ChunkCoord coord, final long offsetX, final long offsetZ, Vector3l origin)
	{
		final long x = coord.x * RawChunk.WIDTH + offsetX;
		final long z = coord.z * RawChunk.DEPTH + offsetZ;
		return distance(x, z, origin);
	}

	private static long distance(long x, long z, Vector3l origin) {
		final long dx = x - origin.x;
		final long dz = z - origin.z;
		
		return (long)Math.sqrt(dx*dx + dz*dz);
	}
}
