/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import java.util.Map;

/** Liquid colours of potions, matching potionColors in the map viewer's containers.js. */
public final class PotionColors {
	public static final int WATER = 0x385DC6;

	private static final Map<String, Integer> COLORS = Map.ofEntries(
			Map.entry("water", WATER),
			Map.entry("mundane", WATER),
			Map.entry("thick", WATER),
			Map.entry("awkward", WATER),
			Map.entry("night_vision", 0xC2FF66),
			Map.entry("invisibility", 0xF6F6F6),
			Map.entry("leaping", 0xFDFF84),
			Map.entry("fire_resistance", 0xFF9900),
			Map.entry("swiftness", 0x33EBFF),
			Map.entry("slowness", 0x8BAFE0),
			Map.entry("water_breathing", 0x98DAC0),
			Map.entry("healing", 0xF82423),
			Map.entry("harming", 0xA9656A),
			Map.entry("poison", 0x87A363),
			Map.entry("regeneration", 0xCD5CAB),
			Map.entry("strength", 0xFFC700),
			Map.entry("weakness", 0x484D48),
			Map.entry("luck", 0xF7F8E0),
			Map.entry("turtle_master", 0x8E7BE8),
			Map.entry("slow_falling", 0xF7F8E0));

	private PotionColors() {}

	/**
	 * @param potion a potion ID such as "minecraft:long_swiftness", or null
	 * @return its RGB colour, or null if Tectonicus does not know it
	 */
	public static Integer of(String potion) {
		if (potion == null)
			return null;
		String name = potion.substring(potion.indexOf(':') + 1);
		if (name.startsWith("long_"))
			name = name.substring("long_".length());
		else if (name.startsWith("strong_"))
			name = name.substring("strong_".length());
		return COLORS.get(name);
	}
}
