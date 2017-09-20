/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class BedEntity extends BlockEntity
{
	private final int color;
	
	public BedEntity(int x, int y, int z, int localX, int localY, int localZ, int color)
	{
		super(x, y, z, localX, localY, localZ);
		this.color = color;
	}
	
	public int getColor() { return color; }
}