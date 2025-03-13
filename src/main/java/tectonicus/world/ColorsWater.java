/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
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
	SNOWY(new Colour4f(new Color(57, 56, 201))),
	COLD(new Colour4f(new Color(61, 87, 214))),
	WARM(new Colour4f(new Color(67, 213, 238))),
	TEMPERATE(new Colour4f(new Color(63, 118, 228))),
	LUSH(new Colour4f(new Color(69, 173, 242))),
	END(new Colour4f(new Color(98, 82, 158))),
	SWAMPY(new Colour4f(new Color(97,123,100))),
	MANGROVE(new Colour4f(new Color(58, 122, 106))),
	CHERRY(new Colour4f(new Color(93, 183, 239))),
	PALE(new Colour4f(new Color(118, 136, 157)));

	private final Colour4f waterColor;
}
