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
