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
