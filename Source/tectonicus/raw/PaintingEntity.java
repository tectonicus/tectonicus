/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class PaintingEntity extends BlockEntity
{
	private final String motive;
	private final int direction;
	
	public PaintingEntity(int x, int y, int z, int localX, int localY, int localZ, String motive, int direction)
	{
		super(x, y, z, localX, localY, localZ);
		this.motive = motive;
		this.direction = direction;
	}
	
	public String getMotive() { return motive; }
	public int getDirection() { return direction; }
}
