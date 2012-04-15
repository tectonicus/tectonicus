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
package tectonicus.cache.swap;

import java.io.File;
import java.io.FileOutputStream;

import tectonicus.TileCoord;
import tectonicus.util.FileUtils;

public class HddTileList implements Iterable<TileCoord>
{
	private final File baseDir;
	
	private int size;
	
	private int minTileX, maxTileX;
	private int minTileY, maxTileY;
	
	public HddTileList(File baseDir)
	{
		this.baseDir = baseDir;
		
		if (baseDir.exists())
			FileUtils.deleteDirectory(baseDir);
		baseDir.mkdirs();
		
		minTileX = minTileY = Integer.MAX_VALUE;
		maxTileX = maxTileY = Integer.MIN_VALUE;
	}
	
	@Override
	public java.util.Iterator<TileCoord> iterator()
	{
		return new HddTileListIterator(baseDir);
	}
	
	public void add(TileCoord coord)
	{
		File coordFile = tileCoordToFile(coord, baseDir);
		
		if (!coordFile.exists())
		{
			coordFile.getParentFile().mkdirs();
			
			FileOutputStream fOut = null;
			try
			{
				fOut = new FileOutputStream(coordFile);
				
				minTileX = Math.min(coord.x, minTileX);
				maxTileX = Math.max(coord.x, maxTileX);
				
				minTileY = Math.min(coord.y, minTileY);
				maxTileY = Math.max(coord.y, maxTileY);
				
				size++;
			}
			catch (Exception e) {}
			finally
			{
				try
				{
					if (fOut != null)
						fOut.close();
				}
				catch (Exception e) {}
			}
		}
	}
	
	public int size()
	{
		return size;
	}
	
	/** Returns a tile coord with the smallest x coord and the smallest y coord inserted.
	 *  Note that this exact coord may not actually be present itself.
	 */
	public TileCoord getAbsoluteMinCoord()
	{
		return new TileCoord(minTileX, minTileY);
	}

	/** Returns a tile coord with the largest x coord and the largest y coord inserted.
	 *  Note that this exact coord may not actually be present itself.
	 */
	public TileCoord getAbsoluteMaxCoord()
	{
		return new TileCoord(maxTileX, maxTileY);
	}
	
	public static File tileCoordToFile(TileCoord coord, File baseDir)
	{
		final int xBucket  = coord.x / 4;
		final int yBucket = coord.y / 4;
		File first = new File(baseDir, ""+xBucket);
		File second = new File(first, ""+yBucket);
		File path = new File(second, "t_"+coord.x+"_"+coord.y+".tile");
		
		return path;
	}
	
	public static TileCoord fileToTileCoord(File file)
	{
		// t_1_2.tile
		try
		{
			String working = file.getName();
			
			final int firstDelim = working.indexOf('_');
			final int secondDelim = working.indexOf('_', firstDelim+1);
			final int thirdDelim = working.indexOf('.');
			
			if (firstDelim != -1 && secondDelim != -1 && thirdDelim != -1)
			{
				String xStr = working.substring(firstDelim+1, secondDelim);
				String yStr = working.substring(secondDelim+1, thirdDelim);
				
				final int x = Integer.parseInt(xStr);
				final int y = Integer.parseInt(yStr);
				
				return new TileCoord(x, y);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
}
