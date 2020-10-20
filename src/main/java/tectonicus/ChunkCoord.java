/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.raw.RawChunk;

public class ChunkCoord implements Comparable<ChunkCoord>
{
	public final long x, z;
	
	public ChunkCoord()
	{
		this.x = 0;
		this.z = 0;
	}
	
	public ChunkCoord(final long x, final long z)
	{
		this.x = x;
		this.z = z;
	}
	
	public ChunkCoord(ChunkCoord other)
	{
		this.x = other.x;
		this.z = other.z;
	}
	
	public static ChunkCoord fromWorldCoord(final float worldX, final float worldZ)
	{
		final long chunkX = (long)Math.floor(worldX / (float)RawChunk.WIDTH);
		final long chunkZ = (long)Math.floor(worldZ / (float)RawChunk.DEPTH);
		return new ChunkCoord(chunkX, chunkZ);
	}

	public static ChunkCoord fromWorldCoord(final int worldX, final int worldZ) {
		return new ChunkCoord(worldX >> 4, worldZ >> 4);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		assert (obj instanceof ChunkCoord);
		
		ChunkCoord other = (ChunkCoord)obj;
		
		return this.x == other.x && this.z == other.z;
	}
	
	@Override
	public int hashCode()
	{
		//		unused		x		z
		//	|	12		|	10	| 	10	|
	
		final int mask10 = 0x3ff; // lowest ten bits set
		
		return (int)( ((x & mask10) << 10) | (z & mask10) );
	}
	
	@Override
	public int compareTo(ChunkCoord other)
	{
		if (this.x == other.x)
		{
			return (int)(other.z - this.z);
		}
		else
		{
			return (int)(other.x - this.x);
		}
	}
	
	@Override
	public String toString()
	{
		return "[ChunkCoord ("+x+", "+z+") / <"+Util.toBase36(x)+", "+Util.toBase36(z)+"> ]";
	}
}
