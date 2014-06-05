/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

public class Vector3l
{
	public long x, y, z;
	
	public Vector3l()
	{
		
	}
	
	public Vector3l(Vector3l other)
	{
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	public Vector3l(final long x, final long y, final long z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public long separation(Vector3l other)
	{
		final long dx = this.x - other.x;
		final long dy = this.y - other.y;
		final long dz = this.z - other.z;
		
		return (long)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
	
	public String toString()
	{
		return "["+super.toString()+" ("+x+", "+y+") ]";
	}
}
