/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

public enum Color
{
	WHITE(0, "white"),
    ORANGE(1, "orange"),
    MAGENTA(2,"magenta"),
    LIGHT_BLUE(3, "light_blue"),
    YELLOW(4, "yellow"),
    LIME(5, "lime"),
    PINK(6, "pink"),
    GRAY(7, "gray"),
    SILVER(8, "silver"),
    CYAN(9, "cyan"),
    PURPLE(10, "purple"),
    BLUE(11, "blue"),
    BROWN(12, "brown"),
    GREEN(13, "green"),
    RED(14, "red"),
    BLACK(15, "black");
	
	private static final Color[] ID_LOOKUP = new Color[values().length];
	private final int id;
	private final String name;
	
	private Color(int id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	static
	{
        for (Color color : values())
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
	
	public static Color byId(int id)
    {
        return ID_LOOKUP[id];
    }
}
