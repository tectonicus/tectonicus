/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

public class Vector3d
{
	public double x, y, z;
	
	public Vector3d() {}
	
	public Vector3d(final double x, final double y, final double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3d(Vector3d other)
	{
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	public void set(final double x, final double y, final double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
