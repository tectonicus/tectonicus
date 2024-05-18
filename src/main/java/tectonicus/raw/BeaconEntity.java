/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;
import tectonicus.world.Effect;

@Getter
public class BeaconEntity extends BlockEntity {
	private final int levels;
	private final Effect primaryEffect;
	private final Effect secondaryEffect;
	
	public BeaconEntity(int x, int y, int z, int localX, int localY, int localZ, int levels, Effect primaryEffect, Effect secondaryEffect) {
		super(x, y, z, localX, localY, localZ);
		this.levels = levels;
		this.primaryEffect = primaryEffect;
		this.secondaryEffect = secondaryEffect;
	}
}
