/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class NewChunkIterator
{
	/*
	private final RegionCache regionCache;
	
	// TODO: This may be a memory issue for large maps
	private File[] regionFiles;
	private int currentFileIndex;
	
	private Region currentRegion;
	private ChunkCoord[] chunkCoords;
	private int currentChunkIndex;
	
	public NewChunkIterator(File worldDir, RegionCache regionCache)
	{
		this.regionCache = regionCache;
		
		File regionDir = new File(worldDir, "region");
		regionFiles = regionDir.listFiles(new RegionFileFilter());
		if (regionFiles == null)
			regionFiles = new File[0];
		
		currentFileIndex = -1;
		chunkCoords = new ChunkCoord[0];
		
		advance();
	}
	
	public boolean hasNext()
	{
		return currentChunkIndex < chunkCoords.length || currentFileIndex < regionFiles.length;
	}
	
	public ChunkCoord next()
	{
		assert (hasNext());
		
		ChunkCoord coord = chunkCoords[currentChunkIndex];
		assert (coord != null);
		
		advance();
		
		return coord;
	}
	
	private void advance()
	{
		currentChunkIndex++;
		if (currentChunkIndex >= chunkCoords.length)
		{
			do
			{
				currentFileIndex++;
				
				if (currentRegion != null)
				{
					currentRegion.close();
					currentRegion = null;
				}
				
				if (currentFileIndex < regionFiles.length)
				{
					try
					{
						File regionFile = regionFiles[currentFileIndex];
						RegionCoord regionCoord = Region.extractRegionCoord(regionFile);
						
						currentRegion = regionCache.getRegion(regionCoord);
						if (currentRegion != null)
						{
							chunkCoords = currentRegion.getContainedChunks();
							if (chunkCoords.length > 0)
							{
								currentChunkIndex = 0;
								break;
							}
							else
							{
								chunkCoords = null;
							}
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						currentRegion = null;
					}
				}
			}
			while (currentFileIndex < regionFiles.length);
		}
	}
	
	private static class RegionFileFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			return pathname.isFile() && pathname.getName().startsWith("r.") && pathname.getName().endsWith(".mcr");
		}
	}
	*/
}
