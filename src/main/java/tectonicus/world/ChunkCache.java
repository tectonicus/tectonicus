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
import com.github.benmanes.caffeine.cache.Weigher;

import java.util.Collection;
import java.util.OptionalLong;
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
		this.getChunk = getChunk;
		invalidatedChunks = new ConcurrentLinkedQueue<>();
		chunks = Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .evictionListener(new RawCacheRemovalListener())
                        .build();
	}

	/**
	 * Creates a cache limited by total weight rather than entry count. Each chunk is weighed once, when it is
	 * added, so the weigher must only depend on state that is fixed by then.
	 */
	public ChunkCache(long maxWeight, Weigher<ChunkCoord, Chunk> weigher, Function<ChunkCoord, Chunk> getChunk)
	{
		this.getChunk = getChunk;
		invalidatedChunks = new ConcurrentLinkedQueue<>();
		chunks = Caffeine.newBuilder()
                        .maximumWeight(maxWeight)
                        .weigher(weigher)
                        .evictionListener(new RawCacheRemovalListener())
                        .build();
	}

	/** Total weight of the cached chunks, or empty when the cache is limited by entry count. */
	public OptionalLong weightedSize()
	{
		return chunks.policy().eviction()
				.map(eviction -> eviction.weightedSize())
				.orElse(OptionalLong.empty());
	}
	
	public void unloadAll()
	{
		unloadInvalidatedChunks();
		chunks.asMap().values().forEach(this::unloadInvalidatedChunk);
		chunks.invalidateAll();
	}
	
	public int size()
	{
		return chunks.asMap().size();
	}
	
	public Chunk get(ChunkCoord coord)
	{
		return chunks.get(coord, getChunk);
	}

	public Chunk getIfPresent(ChunkCoord coord)
	{
		return chunks.getIfPresent(coord);
	}

	/** Number of evicted chunks waiting for {@link #unloadInvalidatedChunks}. */
	public int pendingUnloadCount()
	{
		return invalidatedChunks.size();
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
                        if (chunks.getIfPresent(chunk.getCoord()) == chunk)
                        {
                            continue;
                        }
                        
                        unloadInvalidatedChunk(chunk);
                        
                }
        }
}
