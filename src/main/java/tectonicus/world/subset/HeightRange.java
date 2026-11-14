/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

/** Inclusive bounds in Minecraft world block coordinates. */
public record HeightRange(Integer minY, Integer maxY) {
	public static final HeightRange UNBOUNDED = new HeightRange(null, null);

	public HeightRange {
		if (minY != null && maxY != null && minY > maxY)
			throw new IllegalArgumentException("minY must be less than or equal to maxY");
	}

	public boolean isBounded() {
		return minY != null || maxY != null;
	}

	public boolean contains(double blockY) {
		return Double.isFinite(blockY) && (minY == null || blockY >= minY) && (maxY == null || blockY <= maxY);
	}

	String cacheSuffix() {
		return isBounded() ? "[minY=" + minY + ",maxY=" + maxY + "]" : "";
	}
}
