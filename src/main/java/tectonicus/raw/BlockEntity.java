/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockEntity {
	private int x, y, z;
	private final int localX, localY, localZ;
	
	public BlockEntity(int x, int y, int z, int localX, int localY, int localZ) {
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.localX = localX;
		this.localY = localY;
		this.localZ = localZ;
	}
}
