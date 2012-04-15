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