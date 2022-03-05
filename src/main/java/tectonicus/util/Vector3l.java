/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class Vector3l
{
	public long x, y, z;
	
	public Vector3l(Vector3l other)
	{
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	public long separation(Vector3l other)
	{
		final long dx = this.x - other.x;
		final long dy = this.y - other.y;
		final long dz = this.z - other.z;
		
		return (long)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	@Override
	public String toString()
	{
		return "(" + x + ", " + y + ", " + z +")";
	}
}
