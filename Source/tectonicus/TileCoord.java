/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class TileCoord
{
	public final int x, y;
	
	public TileCoord(final int x, final int y)
	{
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		TileCoord other = (TileCoord)obj;
		return this.x == other.x && this.y == other.y;
	}
	
	@Override
	public int hashCode()
	{
		return ((x & 0xFFFF) << 16) | (y & 0xFFFF);
	}
	
	@Override
	public String toString()
	{
		return "TileCoord("+x+", "+y+")";
	}

	// http://en.wikipedia.org/wiki/Z-order_(curve)
	public long zOrder()
	{
		long zOrder = 0;
		
		// Interleave the x bits
		for (int i=0; i<32; i++)
		{
			int bit = x & (1 << i);
			
			zOrder |= bit << (i * 2);
		}
		
		// Interleave the y bits
		for (int i=0; i<32; i++)
		{
			int bit = y & (1 << i);
			
			zOrder |= bit << ((i * 2) + 1);
		}
		
		return zOrder;
	}
}
