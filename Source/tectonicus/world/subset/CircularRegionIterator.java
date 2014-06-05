/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import java.io.File;

import tectonicus.Region;
import tectonicus.SaveFormat;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;

public class CircularRegionIterator implements RegionIterator
{
	private final File baseDir;
	private final SaveFormat saveFormat;
	
	private final Vector3l origin;
	private final long radius; // In world units
	
	private final long minX, minZ;
	private final long maxX, maxZ;
	
	private long currentX, currentZ;
	
	public CircularRegionIterator(File baseDir, SaveFormat saveFormat, Vector3l origin, final long radius)
	{
		this.baseDir = baseDir;
		this.saveFormat = saveFormat;
		this.origin = new Vector3l(origin.x, 0, origin.z);
		this.radius = radius;
		
		final long radiusInRegions = (long)Math.ceil(radius / Region.WIDTH_IN_CHUNKS / RawChunk.WIDTH) + 1;
		
		final long regionOriginX = (long)Math.floor( (double)origin.x / Region.WIDTH_IN_CHUNKS / RawChunk.WIDTH );
		final long regionOriginZ = (long)Math.floor( (double)origin.z / Region.WIDTH_IN_CHUNKS / RawChunk.WIDTH );
		
		minX = regionOriginX - radiusInRegions;
		minZ = regionOriginZ - radiusInRegions;
		
		maxX = regionOriginX + radiusInRegions;
		maxZ = regionOriginZ + radiusInRegions;
		
		currentX = minX - 1;
		currentZ = minZ;
	}
	
	@Override
	public File getBaseDir()
	{
		return baseDir;
	}
	
	@Override
	public boolean hasNext()
	{
		return currentZ <= maxZ;
	}
	
	@Override
	public File next()
	{
		while (true)
		{
			currentX++;
			if (currentX > maxX)
			{
				currentX = minX;
				currentZ++;
				if (currentZ > maxZ)
					return null;
			}
			
			final long chunkX = currentX * Region.WIDTH_IN_CHUNKS + Region.WIDTH_IN_CHUNKS/2;
			final long chunkZ = currentZ * Region.WIDTH_IN_CHUNKS + Region.WIDTH_IN_CHUNKS/2;
			
			Vector3l pos = new Vector3l(chunkX * RawChunk.WIDTH,
										0,
										chunkZ * RawChunk.DEPTH);
			
			final long regionEdgeLength = Region.WIDTH_IN_CHUNKS * RawChunk.WIDTH;
			final long regionDiagonal = (long)Math.sqrt(regionEdgeLength*regionEdgeLength + regionEdgeLength*regionEdgeLength);
			
			final long sep = origin.separation(pos);
			final long bufferedRadius = radius + regionDiagonal / 2;
			if (sep <= bufferedRadius)
			{
				File regionFile = new File(baseDir, "region/r."+currentX+"."+currentZ+"."+saveFormat.extension);
				if (regionFile.exists())
					return regionFile;
			}
		}
	}
	
}
