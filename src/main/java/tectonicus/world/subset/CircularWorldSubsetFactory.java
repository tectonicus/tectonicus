/*
 * Copyright (c) 2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import tectonicus.util.Vector3l;
import tectonicus.world.World;

public class CircularWorldSubsetFactory implements WorldSubsetFactory
{
	private Vector3l origin;
	private long radius;
	
	public CircularWorldSubsetFactory()
	{
		
	}
	
	public void setOrigin(final Vector3l origin)
	{
		this.origin = origin;
	}
	
	public void setRadius(final long radius)
	{
		this.radius = radius;
	}
	
	public Vector3l getOrigin()
	{
		return origin;
	}
	
	public long getRadius()
	{
		return radius;
	}
	
	@Override
	public WorldSubset create(World world)
	{
		return new CircularWorldSubset(world, origin, radius);
	}
	
	@Override
	public String getDescription()
	{
		String originStr = "null";
		if (origin != null)
			originStr = "("+origin.x+", "+origin.z+")";
		
		return "CircularWorldSubset "+originStr+" "+ radius;
	}
}
