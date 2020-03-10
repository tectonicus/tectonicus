/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.AccessLevel;
import lombok.Getter;
import tectonicus.util.Colour4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@Getter
public enum BiomeWater {
	OCEAN(0, new Color(63, 118, 228)),
	SWAMP(6, new Color(97,123,100)),
	RIVER(7, new Color(63, 118, 228)),
	FROZEN_OCEAN(10, new Color(57, 56, 201)),
	FROZEN_RIVER(11, new Color(57, 56, 201)),
	WARM_OCEAN(44, new Color(67, 213, 238)),
	LUKEWARM_OCEAN(45, new Color(69, 173, 242)),
	COLD_OCEAN(46, new Color(61, 87, 214));

	private static final Map<Integer, BiomeWater> ID_LOOKUP = new HashMap<>(values().length);
	private final int id;
	@Getter(AccessLevel.NONE)
	private final Color wColor;
	private final Colour4f waterColor;

	BiomeWater(int id, Color wColor) {
		this.id = id;
		this.wColor = wColor;
		waterColor = new Colour4f(wColor.getRed() / 255f, wColor.getGreen() / 255f, wColor.getBlue() / 255f);
	}

	static
	{
		for (BiomeWater biome : values()) {
			ID_LOOKUP.put(biome.id, biome);
		}
	}

	public static BiomeWater byId(int id) {
		return ID_LOOKUP.getOrDefault(id, OCEAN);
	}

	public String getName() {
		return this.toString().toLowerCase();
	}
}
