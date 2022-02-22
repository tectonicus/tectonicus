/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;

@Getter
public class PaintingEntity extends BlockEntity
{
	private final String id;
	private final String motive;
	private final int direction;
	
	public PaintingEntity(int x, int y, int z, int localX, int localY, int localZ, String id, String motive, int direction)
	{
		super(x, y, z, localX, localY, localZ);
		this.id = id;
		this.motive = motive;
		this.direction = direction;
	}
}
