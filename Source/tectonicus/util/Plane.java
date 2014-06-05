/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import org.lwjgl.util.vector.Vector3f;

public class Plane
{
	private Vector3f point, normal;
	
	public Plane()
	{
		point = new Vector3f();
		normal = new Vector3f();
	}

	public void set(Vector3f point, Vector3f normal)
	{
		this.point.set(point);
		this.normal.set(normal);
		this.normal.normalise();
	}
	
	public boolean isInside(Vector3f testPoint)
	{
		return isInside(testPoint.x, testPoint.y, testPoint.z);
	}
	
	public boolean isInside(final float testX, final float testY, final float testZ)
	{
		final float dirX = testX - point.x;
		final float dirY = testY - point.y;
		final float dirZ = testZ - point.z;
		
		final float dot = dirX*normal.x + dirY*normal.y + dirZ*normal.z;
		
		return dot >= 0.0f;
	}
}
