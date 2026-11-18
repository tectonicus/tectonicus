/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import tectonicus.Version;

/**
 * @param sectionArrayOffset whether chunk-local y is shifted up so that blocks below Y 0 fit (1.18+ worlds)
 * @param minY               lowest block Y of the dimension; older chunks without a yPos are placed from here
 */
public record WorldInfo(Version version, boolean sectionArrayOffset, int minY) {
	public WorldInfo(Version version, boolean sectionArrayOffset) {
		this(version, sectionArrayOffset, sectionArrayOffset ? DimensionHeight.CAVES_AND_CLIFFS.minY() : 0);
	}

	public int minSectionY() {
		return Math.floorDiv(minY, 16);
	}
}
