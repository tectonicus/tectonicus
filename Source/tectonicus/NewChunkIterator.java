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
