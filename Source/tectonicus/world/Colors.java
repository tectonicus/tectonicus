/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.awt.Color;

import tectonicus.Minecraft;

public enum Colors
{
	WHITE(0, "white", new Color(249,255,254), new Color(255,255,255)),
    ORANGE(1, "orange", new Color(249,128,29), new Color(216,127,51)),
    MAGENTA(2,"magenta", new Color(199,78,189), new Color(178,76,216)),
    LIGHT_BLUE(3, "light_blue", new Color(58,179,218), new Color(102,153,216)),
    YELLOW(4, "yellow", new Color(254,216,61), new Color(229,229,51)),
    LIME(5, "lime", new Color(128,199,31), new Color(127,204,25)),
    PINK(6, "pink", new Color(243,139,170), new Color(242,127,165)),
    GRAY(7, "gray", new Color(71,79,82), new Color(76,76,76)),
    SILVER(8, "silver", new Color(157,157,151), new Color(153,153,153)),
    CYAN(9, "cyan", new Color(22,156,156), new Color(76,127,153)),
    PURPLE(10, "purple", new Color(137,50,184), new Color(127,63,178)),
    BLUE(11, "blue", new Color(60,68,170), new Color(51,76,178)),
    BROWN(12, "brown", new Color(131,84,50), new Color(102,76,51)),
    GREEN(13, "green", new Color(94,124,22), new Color(102,127,51)),
    RED(14, "red", new Color(176,46,38), new Color(153,51,51)),
    BLACK(15, "black", new Color(29,29,33), new Color(25,25,25));
	
	private static final Colors[] ID_LOOKUP = new Colors[values().length];
	private final int id;
	private final String name;
	private final Color newColor, oldColor;
	
	private Colors(int id, String name, Color newColor, Color oldColor)
	{
		this.id = id;
		this.name = name;
		this.newColor = newColor;
		this.oldColor = oldColor;
	}
	
	static
	{
        for (Colors color : values())
            ID_LOOKUP[color.getId()] = color;
    }
	
	public int getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Color getColor()
	{
		if (Minecraft.useOldColorPalette())
			return oldColor;
		else
			return newColor;
	}
	
	public static Colors byId(int id)
    {
        return ID_LOOKUP[id];
    }
}
