/*
 * Copyright (c) 2023 Tectonicus contributors.  All rights reserved.
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

class ChunkCache
{
        private class RawCacheRemovalListener implements RemovalListener<ChunkCoord, Chunk> {

                @Override
                public void onRemoval(ChunkCoord coord, Chunk chunk, RemovalCause rc) {
                        invalidatedChunks.add(chunk);
                }
        
        }
    
	protected final Cache<ChunkCoord, Chunk> chunks;
        private final Function<ChunkCoord, Chunk> getChunk;
        private final ConcurrentLinkedQueue<Chunk> invalidatedChunks;
	
	public ChunkCache(int maxSize, Function<ChunkCoord, Chunk> getChunk)
	{
		chunks = Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .evictionListener(new RawCacheRemovalListener())
                        .build();
                this.getChunk = getChunk;
                invalidatedChunks = new ConcurrentLinkedQueue<>();
	}
	
	public void unloadAll()
	{
		chunks.invalidateAll();
                unloadInvalidatedChunks();
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
        
        protected void unloadInvalidatedChunk(Chunk chunk) {        
        }
        
        public void unloadInvalidatedChunks() {
                chunks.cleanUp();
            
                Chunk chunk;
                while ((chunk = invalidatedChunks.poll()) != null) {
                        
                        // Check whether the chunk is not currently in cache. It could have been re-added
                        // in which case we can not unload it.
                        if (chunks.getIfPresent(chunk.getCoord()) != null)
                        {
                            continue;
                        }
                        
                        unloadInvalidatedChunk(chunk);
                        
                }
        }
}
