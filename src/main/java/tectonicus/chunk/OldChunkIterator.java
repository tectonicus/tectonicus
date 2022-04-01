/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.chunk;

public class OldChunkIterator
{
	/*
	private File[] xDirs;
	private File[] yDirs;
	private File[] chunkFiles;
	
	private int xPosition, yPosition, chunkPosition;
	
	public OldChunkIterator(File worldDir)
	{
		xDirs = worldDir.listFiles( new DirectoryFilter() );
		
		xPosition = 0;
		yPosition = 0;
		chunkPosition = -1;
		
		advance();	
	}
	
	
	public boolean hasNext()
	{
		if (xDirs == null || yDirs == null)
			return false;
		
		if (xPosition >= xDirs.length)
			return false;
		
		return true;
	}
	
	public ChunkCoord next()
	{
		if (!hasNext())
			return null;
		
		File datFile = chunkFiles[chunkPosition];
		advance();
		
		ChunkCoord coord = World.getTileCoord(datFile);
		
		return coord;
	}
	
	private void advance()
	{	
		while (true)
		{
			final boolean chunkOk = advanceChunkFile();
			if (!chunkOk)
			{
				final boolean yOk = advanceYDir();
				if (!yOk)
				{
					final boolean xOk = advanceXDir();
					if (!xOk)
					{
						// finished!
						assert (!hasNext());
						break;
					}
					refreshY();
				}
				refreshChunks();
			}
			
			if (chunkPosition < chunkFiles.length)
				break;
		}
	}
	
	private void refreshY()
	{
		yPosition = 0;
		yDirs = xDirs[xPosition].listFiles( new DirectoryFilter() );
	}
	
	private void refreshChunks()
	{
		chunkPosition = 0;
		if (yDirs != null && yPosition < yDirs.length)
			chunkFiles = yDirs[yPosition].listFiles( new ChunkFilter() );
		else
			chunkFiles = new File[0];
	}
	
	private boolean advanceChunkFile()
	{
		if (chunkFiles == null)
			return false;
		
		boolean ok = false;
		
		while (true)
		{
			chunkPosition++;
			
			if (chunkPosition >= chunkFiles.length)
			{
				ok = false;
				break;
			}
			
			if (chunkFiles[chunkPosition].isFile())
			{
				ok = true;
				break;
			}
		}
		
		return ok;
	}
	
	private boolean advanceYDir()
	{
		if (yDirs == null)
			return false;
		
		boolean ok = false;
		
		while (true)
		{
			yPosition++;
			
			if (yPosition >= yDirs.length)
			{
				ok = false;
				break;
			}
			
			if (yDirs[yPosition].isDirectory())
			{
				ok = true;
				break;
			}
		}
		
		return ok;
	}

	private boolean advanceXDir()
	{
		if (xDirs == null)
			return false;
		
		boolean ok = false;
		
		while (true)
		{
			xPosition++;
			
			if (xPosition >= xDirs.length)
			{
				ok = false;
				break;
			}
			
			if (xDirs[xPosition].isDirectory())
			{
				ok = true;
				break;
			}
		}
		
		return ok;
	}
	
	private static class DirectoryFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			return pathname.isDirectory();
		}
	}
	private static class ChunkFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			return pathname.isFile() && pathname.getName().endsWith(".dat");
		}
		
	}
	*/
}
