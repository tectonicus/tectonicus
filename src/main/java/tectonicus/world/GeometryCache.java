/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.util.function.Function;

import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

class GeometryCache extends ChunkCache
{	
	public GeometryCache(int maxSize, Function<ChunkCoord, Chunk> getChunk)
        {		
                super(maxSize, getChunk);
	}
	
	public long getGeometryMemorySize()
	{
		long total = 0;
		for (Chunk c : chunks.asMap().values())
		{
			total += c.getGeometryMemorySize();
		}
		return total;
	}
        
        @Override
        protected void unloadInvalidatedChunk(Chunk chunk)
        {
                chunk.unloadGeometry();
        }	
}
