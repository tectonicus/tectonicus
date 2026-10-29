/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.Setter;

/** Effective rendering defaults, resolved by the XML parser before rendering. */
@Getter
@Setter
public class RenderOptions {
	public static final int DEFAULT_BIOME_BLEND_RADIUS = 2;
	public static final int MAX_BIOME_BLEND_RADIUS = 7;

	private boolean smoothLit = true;
	private boolean oldGrayscaleLighting;
	private boolean sortTranslucentFaces = true;
	private boolean legacyWater;
	@Setter(lombok.AccessLevel.NONE)
	private int biomeBlendRadius = DEFAULT_BIOME_BLEND_RADIUS;
	@Setter(lombok.AccessLevel.NONE)
	private boolean biomeBlendRadiusConfigured;

	public void setBiomeBlendRadius(int radius) {
		if (radius < 0 || radius > MAX_BIOME_BLEND_RADIUS)
			throw new IllegalArgumentException("biomeBlendRadius must be between 0 and " + MAX_BIOME_BLEND_RADIUS);
		biomeBlendRadius = radius;
		biomeBlendRadiusConfigured = true;
	}

	public boolean useOldGrayscaleLighting() {
		return oldGrayscaleLighting;
	}
}
