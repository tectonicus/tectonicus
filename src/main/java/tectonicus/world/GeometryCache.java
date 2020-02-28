/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.util.LinkedHashMap;

import tectonicus.Chunk;
import tectonicus.ChunkCoord;

class GeometryCache
{
	private int maxSize;
	
	private LinkedHashMap<ChunkCoord, Chunk> chunks;
	
	public GeometryCache(final int maxSize)
	{
		this.maxSize = maxSize;
		
		chunks = new LinkedHashMap<ChunkCoord, Chunk>(16, 0.75f, true); // access-order (most recently accessed last)
		
		/*
		ChunkCoord c00 = new ChunkCoord(0, 0);
		ChunkCoord c11 = new ChunkCoord(1, 1);
		ChunkCoord c22 = new ChunkCoord(2, 2);
		ChunkCoord c33 = new ChunkCoord(3, 3);
		ChunkCoord c44 = new ChunkCoord(4, 4);
		
		Chunk ch00 = null;
		Chunk ch11 = null;
		Chunk ch22 = null;
		Chunk ch33 = null;
		try
		{
			ch00 = new Chunk(c00, null);
			ch11 = new Chunk(c11, null);
			ch22 = new Chunk(c22, null);
			ch33 = new Chunk(c33, null);
		}
		catch (Exception e) { e.printStackTrace(); }
		
		put(c00, ch00);
		assert (contains(c00));
		assert (!contains(c11));
		assert (size() == 1);
		
		put(c11, ch11);
		
		assert(contains(new ChunkCoord(1, 1)));
		assert(size() == 2);
		
		put(c22, ch22);
		assert(contains(c00));
		assert(contains(c11));
		assert(contains(c22));
		assert(size() == 3);
		
		trimToMaxSize();
		assert(contains(c22));
		assert(contains(c11));
		assert(!contains(c00));
		
		// Contains c11, c22
		
		touch(c11);
		put(c33, null);
		trimToMaxSize();
		
		assert(contains(c11));
		assert(contains(c33));
		assert(size() == 2);
		
		
		put(c00, null);
		put(c22, null);
		trimToMaxSize();
		
		System.out.println("ok");
		
		chunks.clear();
		*/
	}
	
	public void unloadAll()
	{
		for (Chunk c : chunks.values())
		{
			c.unloadGeometry();
		}
		chunks.clear();
	}
	
	public boolean contains(ChunkCoord coord)
	{
		return chunks.containsKey(coord);
	}
	
	public void put(ChunkCoord coord, Chunk chunk)
	{
		assert (chunk != null);
		
		chunks.put(coord, chunk);
		chunks.get(coord); // touch the new entry to pull it to the end of the list
	}
	
	public Chunk get(ChunkCoord coord)
	{
		return chunks.get(coord);
	}
	
	public int size()
	{
		return chunks.size();
	}
	
	public long getGeometryMemorySize()
	{
		long total = 0;
		for (Chunk c : chunks.values())
		{
			total += c.getGeometryMemorySize();
		}
		return total;
	}
	
	public void trimToMaxSize()
	{
		while (size() > maxSize)
		{
			Chunk oldestChunk = chunks.values().iterator().next();
			oldestChunk.unloadGeometry();
			chunks.remove(oldestChunk.getCoord());
		}
	}
	
	public void touch(ChunkCoord coord)
	{
		chunks.get(coord);
	}
	
}
