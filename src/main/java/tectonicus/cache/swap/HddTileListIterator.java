/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
