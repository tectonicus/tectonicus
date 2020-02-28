/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.raw.RawChunk;

public class RegionCoord implements Comparable<RegionCoord>
{
	public static int REGION_WIDTH = 32;
	public static int REGION_HEIGHT = 32;
	
	public final long x, z;
	
	public RegionCoord()
	{
		this.x = 0;
		this.z = 0;
	}
	
	public RegionCoord(final long x, final long z)
	{
		this.x = x;
		this.z = z;
	}
	
	public RegionCoord(RegionCoord other)
	{
		this.x = other.x;
		this.z = other.z;
	}
	
	public static RegionCoord fromWorldCoord(final float worldX, final float worldZ)
	{
		final long chunkX = (long)Math.floor(worldX / (float)RawChunk.WIDTH);
		final long chunkZ = (long)Math.floor(worldZ / (float)RawChunk.DEPTH);
		
		final long regionX = (long)Math.floor(chunkX / (float)REGION_WIDTH);
		final long regionZ = (long)Math.floor(chunkZ / (float)REGION_HEIGHT);
		
		return new RegionCoord(regionX, regionZ);
	}
	
	public static RegionCoord fromChunkCoord(final ChunkCoord chunkCoord)
	{
		final long regionX = (long)Math.floor(chunkCoord.x / (float)REGION_WIDTH);
		final long regionZ = (long)Math.floor(chunkCoord.z / (float)REGION_HEIGHT);
		
		return new RegionCoord(regionX, regionZ);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		assert (obj instanceof RegionCoord);
		
		RegionCoord other = (RegionCoord)obj;
		
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
	public int compareTo(RegionCoord other)
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
		return "[RegionCoord ("+x+", "+z+") / <"+Util.toBase36(x)+", "+Util.toBase36(z)+"> ]";
	}
}
