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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;

import tectonicus.TileCoord;

public class HddTileListIterator implements Iterator<TileCoord>
{
	private File[] xDirs;
	private File[] yDirs;
	private TileCoord[] tiles;
	
	private int xPosition, yPosition, tilePosition;
	
	private TileCoord next;
	
	public HddTileListIterator(File baseDir)
	{
		xDirs = baseDir.listFiles( new DirectoryFilter() );
		
		if (xDirs.length > 0)
			yDirs = xDirs[0].listFiles( new DirectoryFilter() );
		else
			yDirs = new File[0]	;
		
		if (yDirs.length > 0)
			tiles = fetchTiles(yDirs[0]);
		else
			tiles = new TileCoord[0];
		
		xPosition = 0;
		yPosition = 0;
		tilePosition = -1;
		
		advance();	
	}
	
	
	@Override
	public boolean hasNext()
	{
		return next != null;
	}
	
	@Override
	public TileCoord next()
	{
		if (!hasNext())
			return null;
		
		TileCoord actualNext = next;
		
		advance();
		
		return actualNext;
	}
	
	@Override
	public void remove()
	{
		throw new RuntimeException("not implemented");
	}
	
	private void advance()
	{	
		boolean finished = false;
		
		// First try and find the next file in tileFiles
		final boolean tileOk = advanceTile();
		
		if (!tileOk)
		{
			// Finished in this tile dir? Advance the y dir
			final boolean yOk = advanceYDir();
			
			if (!yOk)
			{
				// Finished in the y dir? Advance the x dir
				final boolean xOk = advanceXDir();
				
				if (!xOk)
				{
					// Finished?
					next = null;
					finished = true;
				}
			
				if (!finished)
					refreshYDir();
			}
			if (!finished)
			{
				refreshTiles();
				advance();
			}
		}
	}
	
	private boolean advanceTile()
	{
		tilePosition++;
		
		if (tiles == null || tiles.length <= tilePosition)
			return false;
		
		next = tiles[tilePosition];
		
		return true;
	}
	
	private boolean advanceYDir()
	{
		yPosition++;
		
		if (yDirs == null || yDirs.length <= yPosition)
			return false;
		
		return true;
	}
	
	private boolean advanceXDir()
	{
		xPosition++;
		
		if (xDirs == null || xDirs.length <= xPosition)
			return false;
		
		return true;
	}
	
	private void refreshTiles()
	{
		// Refresh the tiles array
		tiles = fetchTiles(yDirs[yPosition]);
		tilePosition = -1;
	}
	
	private void refreshYDir()
	{
		// Refresh the y dir
		yDirs = xDirs[xPosition].listFiles( new DirectoryFilter() );
		yPosition = 0;
	}
	
	private static TileCoord[] fetchTiles(File dir)
	{
		File[] files = dir.listFiles( new TileCoordFilter() );
		
		ArrayList<TileCoord> coords = new ArrayList<TileCoord>();
		
		for (File f : files)
		{
			TileCoord coord = HddTileList.fileToTileCoord(f);
			if (coord != null)
				coords.add(coord);
		}
		
		return coords.toArray(new TileCoord[0]);
	}
	
	private static class DirectoryFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			return pathname.isDirectory();
		}
	}
	private static class TileCoordFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			// TODO: More checking here - actually parse out tile coord
			
			return pathname.isFile() && pathname.getName().endsWith(".tile");
		}
		
	}
}
