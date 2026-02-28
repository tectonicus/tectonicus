/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import tectonicus.Region;
import tectonicus.SaveFormat;
import tectonicus.raw.RawChunk;

import java.io.File;

public class RectangularRegionIterator implements RegionIterator
{
	private final File baseDir;
	private final SaveFormat saveFormat;
	
	private final long bufferedMinX, bufferedMaxX;
	private final long bufferedMinZ, bufferedMaxZ;
	
	// Region coordinate bounds
	private final long minRegionX, minRegionZ;
	private final long maxRegionX, maxRegionZ;
	
	// Iteration state
	private long currentRegionX, currentRegionZ;
	
	public RectangularRegionIterator(File baseDir, SaveFormat saveFormat,
									 long bufferedMinX, long bufferedMaxX, long bufferedMinZ, long bufferedMaxZ)
	{
		this.baseDir = baseDir;
		this.saveFormat = saveFormat;
		
		this.bufferedMinX = bufferedMinX;
		this.bufferedMaxX = bufferedMaxX;
		this.bufferedMinZ = bufferedMinZ;
		this.bufferedMaxZ = bufferedMaxZ;
		
		// Convert world (block) coordinates to region coordinates
		// Region files are identified by their region coordinate (rx, rz)
		// Each region contains 32x32 chunks, each chunk is 16x16 blocks
		// So each region is 512x512 blocks
		
		final long regionEdgeLength = Region.WIDTH_IN_CHUNKS * RawChunk.WIDTH; // divide by 512 to get the block length of one region edge
		
		// Calculate region bounds from buffered bounds
		minRegionX = (long)Math.floor((double)bufferedMinX / regionEdgeLength);
		minRegionZ = (long)Math.floor((double)bufferedMinZ / regionEdgeLength);
		
		maxRegionX = (long)Math.floor((double)bufferedMaxX / regionEdgeLength);
		maxRegionZ = (long)Math.floor((double)bufferedMaxZ / regionEdgeLength);
		
		// Initialize iteration pointers
		currentRegionX = minRegionX - 1;
		currentRegionZ = minRegionZ;
	}
	
	@Override
	public File getBaseDir()
	{
		return baseDir;
	}
	
	@Override
	public boolean hasNext()
	{
		return currentRegionZ <= maxRegionZ;
	}
	
	@Override
	public File next() {
		while (true) {
			currentRegionX++;
			if (currentRegionX > maxRegionX) {
				currentRegionX = minRegionX;
				currentRegionZ++;
				if (currentRegionZ > maxRegionZ)
					return null;
			}
			
			// Calculate region bounds in world coordinates
			final long regionEdgeLength = Region.WIDTH_IN_CHUNKS * RawChunk.WIDTH;
			
			final long regionMinX = currentRegionX * regionEdgeLength;
			final long regionMaxX = regionMinX + regionEdgeLength;
			
			final long regionMinZ = currentRegionZ * regionEdgeLength;
			final long regionMaxZ = regionMinZ + regionEdgeLength;
			
			// Check if region overlaps with buffered rectangle
			final boolean overlaps = !(regionMaxX < bufferedMinX || regionMinX > bufferedMaxX ||
					regionMaxZ < bufferedMinZ || regionMinZ > bufferedMaxZ);
			
			if (overlaps) {
				File regionFile = new File(baseDir, "region/r." + currentRegionX + "." + currentRegionZ + "." + saveFormat.extension);
				if (regionFile.exists())
					return regionFile;
			}
		}
	}
}

