/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tectonicus.util.Colour4f;

import java.awt.Color;

@Getter
@RequiredArgsConstructor
public enum ColorsWater {
	// Java Edition water tints are assigned per biome, not by climate.
	FROZEN(new Colour4f(new Color(57, 56, 201))),
	COLD(new Colour4f(new Color(61, 87, 214))),
	WARM(new Colour4f(new Color(67, 213, 238))),
	DEFAULT(new Colour4f(new Color(63, 118, 228))),
	LUKEWARM(new Colour4f(new Color(69, 173, 242))),
	MEADOW(new Colour4f(new Color(14, 78, 207))),
	ICE_CAVES(new Colour4f(new Color(20, 85, 155))),
	SWAMP(new Colour4f(new Color(97,123,100))),
	MANGROVE_SWAMP(new Colour4f(new Color(58, 122, 106))),
	CHERRY_GROVE(new Colour4f(new Color(93, 183, 239))),
	PALE_GARDEN(new Colour4f(new Color(118, 136, 157))),
	SULFUR_CAVES(new Colour4f(new Color(52, 191, 137))),
	DAPPLED_FOREST(new Colour4f(new Color(55, 81, 84)));

	private final Colour4f waterColor;
}
