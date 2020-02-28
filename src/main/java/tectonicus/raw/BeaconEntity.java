/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class BeaconEntity extends BlockEntity
{
	private final int levels;
	
	public BeaconEntity(int x, int y, int z, int localX, int localY, int localZ, int levels)
	{
		super(x, y, z, localX, localY, localZ);
		this.levels = levels;
	}
	
	public int getLevels() { return levels; }
}
