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
import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.ArrayBlockFilter;
import tectonicus.world.filter.BlockFilter;

import java.io.File;

@Data
@AllArgsConstructor
public class RectangularWorldSubset implements WorldSubset
{
	private Vector3l origin;
	private final long width;
	private final long depth;

	// Calculated bounds (in blocks)
	private long minX;
	private long maxX;
	private long minZ;
	private long maxZ;
	
	// Buffered bounds (for chunk filtering)
	private long bufferedMinX;
	private long bufferedMaxX;
	private long bufferedMinZ;
	private long bufferedMaxZ;

	/**
	 * Constructor that calculates bounds from origin, width, and depth
	 */
	public RectangularWorldSubset(Vector3l origin, long width, long depth)
	{
		this.origin = origin;
		this.width = width;
		this.depth = depth;

		if (origin != null) {
			calculateMinMaxCoords(origin, width, depth);
		}
	}

	@Override
	public RegionIterator createRegionIterator(SaveFormat saveFormat, File dimensionDir)
	{
		return new RectangularRegionIterator(dimensionDir, saveFormat, bufferedMinX, bufferedMaxX, bufferedMinZ, bufferedMaxZ);
	}
	
	@Override
	public boolean contains(ChunkCoord coord)
	{
		// Calculate chunk center in world coordinates
		final long chunkCenterX = coord.x * RawChunk.WIDTH + RawChunk.WIDTH / 2;
		final long chunkCenterZ = coord.z * RawChunk.DEPTH + RawChunk.DEPTH / 2;
		
		// Check if chunk center is within buffered bounds
		return chunkCenterX >= bufferedMinX && chunkCenterX <= bufferedMaxX &&
		       chunkCenterZ >= bufferedMinZ && chunkCenterZ <= bufferedMaxZ;
	}

	@Override
	public boolean containsBlock(double x, double z)
	{
		// Check if block is within exact rectangle bounds (no buffer)
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
	}
	
	@Override
	public BlockFilter getBlockFilter(ChunkCoord coord)
	{
		ArrayBlockFilter filter = new ArrayBlockFilter();
		
		// Calculate world coordinates for each block in the chunk
		for (int x = 0; x < RawChunk.WIDTH; x++)
		{
			for (int z = 0; z < RawChunk.DEPTH; z++)
			{
				final long worldX = coord.x * RawChunk.WIDTH + x;
				final long worldZ = coord.z * RawChunk.DEPTH + z;
				
				// Check if block is within exact rectangle bounds
				final boolean allow = (worldX >= minX && worldX <= maxX &&
				                      worldZ >= minZ && worldZ <= maxZ);
				filter.set(x, z, allow);
			}
		}
		
		return filter;
	}

	@Override
	public String toString()
	{
		return String.format("RectangularWorldSubset@[%d,%d],w%d,d%d",
			origin.x, origin.z, width, depth);
	}
	
	@Override
	public void setOrigin(Vector3l origin) {
		this.origin = origin;
		calculateMinMaxCoords(origin, width, depth);
	}
	
	public void calculateMinMaxCoords(Vector3l origin, long width, long depth) {
		this.minX = origin.x - (width / 2);
		this.minZ = origin.z - (depth / 2);
		this.maxX = origin.x + (width / 2);
		this.maxZ = origin.z + (depth / 2);

		final long buffer = RawChunk.WIDTH + RawChunk.DEPTH;
		this.bufferedMinX = minX - buffer;
		this.bufferedMaxX = maxX + buffer;
		this.bufferedMinZ = minZ - buffer;
		this.bufferedMaxZ = maxZ + buffer;
	}
}

