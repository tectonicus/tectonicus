/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.raw;

import java.util.Map;

/** A text component's color and its UTF-16 range within a sign line. */
public record SignTextColor(int start, int end, int rgb) {
	private static final Map<String, Integer> NAMED_COLORS = Map.ofEntries(
			Map.entry("black", 0x000000), Map.entry("dark_blue", 0x0000aa),
			Map.entry("dark_green", 0x00aa00), Map.entry("dark_aqua", 0x00aaaa),
			Map.entry("dark_red", 0xaa0000), Map.entry("dark_purple", 0xaa00aa),
			Map.entry("gold", 0xffaa00), Map.entry("gray", 0xaaaaaa),
			Map.entry("dark_gray", 0x555555), Map.entry("blue", 0x5555ff),
			Map.entry("green", 0x55ff55), Map.entry("aqua", 0x55ffff),
			Map.entry("red", 0xff5555), Map.entry("light_purple", 0xff55ff),
			Map.entry("yellow", 0xffff55), Map.entry("white", 0xffffff));

	static int parse(String color, int inherited) {
		if (color.startsWith("#")) {
			try {
				int rgb = Integer.parseInt(color.substring(1), 16);
				if (rgb >= 0 && rgb <= 0xffffff) return rgb;
			} catch (NumberFormatException ignored) {
				// An invalid color leaves the inherited style in place.
			}
		}
		return NAMED_COLORS.getOrDefault(color, inherited);
	}
}
