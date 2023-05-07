/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.util.Collection;
import java.util.function.Function;

import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

class RawCache
{
        private class RawCacheRemovalListener implements RemovalListener<ChunkCoord, Chunk> {

                @Override
                public void onRemoval(ChunkCoord coord, Chunk chunk, RemovalCause rc) {
                        chunk.unloadRaw();
                }
        
        }
    
	private final Cache<ChunkCoord, Chunk> chunks;
        private final Function<ChunkCoord, Chunk> getChunk;
	
	public RawCache(int maxSize, Function<ChunkCoord, Chunk> getChunk)
	{
		chunks = Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .evictionListener(new RawCacheRemovalListener())
                        .build();
                this.getChunk = getChunk;
	}
	
	public void unloadAll()
	{
		chunks.invalidateAll();
	}
	
	public long getRawMemorySize()
	{
		long rawMemTotal = 0;
		for (Chunk c : chunks.asMap().values())
		{
			rawMemTotal += c.getRawMemorySize();
		}
		return rawMemTotal;
	}
	
	public int size()
	{
		return chunks.asMap().size();
	}
	
	public Chunk get(ChunkCoord coord)
	{
		return chunks.get(coord, getChunk);
	}
	
	public Collection<Chunk> values()
	{
		return chunks.asMap().values();
	}
}
