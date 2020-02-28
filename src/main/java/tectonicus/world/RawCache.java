/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.util.Collection;
import java.util.LinkedHashMap;

import tectonicus.Chunk;
import tectonicus.ChunkCoord;

class RawCache
{
	private int maxSize;
	
	private LinkedHashMap<ChunkCoord, Chunk> chunks;
	
	public RawCache(int maxSize)
	{
		this.maxSize = maxSize;
		
		chunks = new LinkedHashMap<ChunkCoord, Chunk>(16, 0.75f, true);
	}
	
	public void unloadAll()
	{
		for (Chunk c : chunks.values())
		{
			c.unloadRaw();
		}
		chunks.clear();
	}
	
	public long getRawMemorySize()
	{
		long rawMemTotal = 0;
		for (Chunk c : chunks.values())
		{
			rawMemTotal += c.getRawMemorySize();
		}
		return rawMemTotal;
	}
	
	public int size()
	{
		return chunks.size();
	}
	
	public boolean contains(ChunkCoord coord)
	{
		return chunks.containsKey(coord);
	}
	
	public void put(ChunkCoord coord, Chunk chunk)
	{
		assert (chunk != null);
		
		chunks.put(coord, chunk);
		chunks.get(coord);
	}
	
	public Chunk get(ChunkCoord coord)
	{
		return chunks.get(coord);
	}
	
	public void touch(ChunkCoord coord)
	{
		chunks.get(coord);
	}
	
	public void trimToMaxSize()
	{
		while (size() > maxSize)
		{
			Chunk oldestChunk = chunks.values().iterator().next();
			oldestChunk.unloadRaw();
			chunks.remove(oldestChunk.getCoord());
		}
	}
	
	public Collection<Chunk> values()
	{
		return chunks.values();
	}
}
