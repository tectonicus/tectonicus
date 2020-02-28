/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class ChunkIterator
{
	/*
	private OldChunkIterator oldIterator;
	private NewChunkIterator newIterator;
	
	public ChunkIterator(File worldDir, RegionCache regionCache)
	{
		oldIterator = new OldChunkIterator(worldDir);
		newIterator = new NewChunkIterator(worldDir, regionCache);
	}
	
	public boolean hasNext()
	{
		if (newIterator.hasNext())
			return true;
		
		return oldIterator.hasNext();
	}
	
	public ChunkCoord next()
	{
		if (newIterator.hasNext())
			return newIterator.next();
		
		return oldIterator.next();
	}
	*/
}
