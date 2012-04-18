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
