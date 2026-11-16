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
import org.apache.commons.lang3.StringUtils;
import tectonicus.Minecraft;
import tectonicus.util.Colour4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public enum Colors {
	//These are the numeric ids for Minecraft 1.13 and newer. For older versions you need to subtract the id from 15
	WHITE(0, "white", new Color(249, 255, 254), new Color(255, 255, 255), 0xffffff),
	ORANGE(1, "orange", new Color(249, 128, 29), new Color(216, 127, 51), 0xff681f),
	MAGENTA(2, "magenta", new Color(199, 78, 189), new Color(178, 76, 216), 0xff00ff),
	LIGHT_BLUE(3, "light_blue", new Color(58, 179, 218), new Color(102, 153, 216), 0x9ac0cd),
	YELLOW(4, "yellow", new Color(254, 216, 61), new Color(229, 229, 51), 0xffff00),
	LIME(5, "lime", new Color(128, 199, 31), new Color(127, 204, 25), 0xbfff00),
	PINK(6, "pink", new Color(243, 139, 170), new Color(242, 127, 165), 0xff69b4),
	GRAY(7, "gray", new Color(71, 79, 82), new Color(76, 76, 76), 0x808080),
	SILVER(8, "silver", "light_gray", new Color(157, 157, 151), new Color(153, 153, 153), 0xd3d3d3),
	CYAN(9, "cyan", new Color(22, 156, 156), new Color(76, 127, 153), 0x00ffff),
	PURPLE(10, "purple", new Color(137, 50, 184), new Color(127, 63, 178), 0xa020f0),
	BLUE(11, "blue", new Color(60, 68, 170), new Color(51, 76, 178), 0x0000ff),
	BROWN(12, "brown", new Color(131, 84, 50), new Color(102, 76, 51), 0x8b4513),
	GREEN(13, "green", new Color(94, 124, 22), new Color(102, 127, 51), 0x00ff00),
	RED(14, "red", new Color(176, 46, 38), new Color(153, 51, 51), 0xff0000),
	BLACK(15, "black", new Color(29, 29, 33), new Color(25, 25, 25), 0x000000);
	
	private static final Colors[] ID_LOOKUP = new Colors[values().length];
	private static final Map<String, Colors> NAME_LOOKUP = HashMap.newHashMap(values().length);
	@Getter
	private final int id;
	@Getter
	private final String name;
	private final String altName;
	private final Color newColor, oldColor;
	@Getter
	private final int textColor;
	private final Colour4f newColorNormalized, oldColorNormalized;
	
	Colors(int id, String name, Color newColor, Color oldColor, int textColor) {
		this(id, name, StringUtils.EMPTY, newColor, oldColor, textColor);
	}
	
	Colors(int id, String name, String altName, Color newColor, Color oldColor, int textColor) {
		this.id = id;
		this.name = name;
		this.altName = altName;
		this.newColor = newColor;
		this.oldColor = oldColor;
		this.textColor = textColor;
		
		this.newColorNormalized = new Colour4f(newColor);
		this.oldColorNormalized = new Colour4f(oldColor);
	}
	
	static {
		for (Colors color : values()) {
			ID_LOOKUP[color.getId()] = color;
			NAME_LOOKUP.put(color.name, color);
			if (StringUtils.isNotEmpty(color.altName)) {
				NAME_LOOKUP.put(color.altName, color);
			}
		}
	}
	
	public Color getColor() {
		if (Minecraft.useOldColorPalette())
			return oldColor;
		else
			return newColor;
	}
	
	public Colour4f getColorNormalized() {
		if (Minecraft.useOldColorPalette())
			return oldColorNormalized;
		else
			return newColorNormalized;
	}

	public int getDarkTextColor() {
		int red = (int) ((textColor >> 16) * 0.4f);
		int green = (int) (((textColor >> 8) & 0xff) * 0.4f);
		int blue = (int) ((textColor & 0xff) * 0.4f);
		return (red << 16) | (green << 8) | blue;
	}

	public int getGlowingTextOutline() {
		return this == BLACK ? 0xf0ebcc : getDarkTextColor();
	}
	
	public static Colors byId(int id) {
		return ID_LOOKUP[id];
	}
	
	public static Colors byName(String name) {
		return NAME_LOOKUP.get(name.toLowerCase());
	}
}
