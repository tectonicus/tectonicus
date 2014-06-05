/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

public class Vector3i
{
	public int x, y, z;
	
	public Vector3i()
	{
		
	}
	
	public Vector3i(Vector3i other)
	{
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	public Vector3i(final int x, final int y, final int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
