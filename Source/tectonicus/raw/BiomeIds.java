/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class BiomeIds
{
	public static final int UNKNOWN					= -1;
	
	public static final int OCEAN					= 0;
	public static final int PLAINS					= 1;
	public static final int DESERT					= 2;
	public static final int EXTREME_HILLS			= 3;
	public static final int FOREST					= 4;
	public static final int TAIGA					= 5;
	public static final int SWAMPLAND				= 6;
	public static final int RIVER					= 7;
	public static final int HELL					= 8;
	public static final int SKY						= 9;
	public static final int FROZEN_OCEAN			= 10;
	public static final int FROZEN_RIVER			= 11;
	public static final int ICE_PLAINS				= 12;
	public static final int ICE_MOUNTAINS			= 13;
	public static final int MUSHROOM_ISLAND			= 14;
	public static final int MUSHROOM_ISLAND_SHORE	= 15;
	public static final int BEACH					= 16;
	public static final int DESERT_HILLS			= 17;
	public static final int FOREST_HILLS			= 18;
	public static final int TAIGA_HILLS				= 19;
	public static final int EXTREME_HILLS_EDGE		= 20;
	public static final int JUNGLE					= 21;
	public static final int JUNGLE_HILLS			= 22;
	
	public static final Point DefaultColourCoord = new Point(127, 127);
	public static final Point OceanColourCoord = new Point(127, 191);
	public static final Point JungleColourCoord = new Point(0, 25);
	public static final Point MushroomColourCoord = new Point(25, 25);
	public static final Point SwampColourCoord = new Point(50, 71);
	public static final Point ForestColourCoord = new Point(76, 112);
	public static final Point PlainsColourCoord = new Point(50, 173);
	public static final Point DesertColourCoord = new Point(0, 255);
	public static final Point MountainsColourCoord = new Point(203, 239);
	public static final Point TaigaColourCoord = new Point(242, 244);
	public static final Point SnowColourCoord = new Point(255, 255);
	
	private static Map<Integer, Point> biomeColourCoords = new HashMap<Integer, Point>();
	
	static
	{
		biomeColourCoords.put(OCEAN, OceanColourCoord);
		biomeColourCoords.put(PLAINS, PlainsColourCoord);
		biomeColourCoords.put(DESERT, DesertColourCoord);
		biomeColourCoords.put(EXTREME_HILLS, DefaultColourCoord);
		biomeColourCoords.put(FOREST, ForestColourCoord);
		biomeColourCoords.put(TAIGA, TaigaColourCoord);
		biomeColourCoords.put(SWAMPLAND, SwampColourCoord);
		biomeColourCoords.put(RIVER, DefaultColourCoord);
		biomeColourCoords.put(HELL, DefaultColourCoord);
		biomeColourCoords.put(SKY, DefaultColourCoord);
		biomeColourCoords.put(FROZEN_OCEAN, TaigaColourCoord);
		biomeColourCoords.put(FROZEN_RIVER, TaigaColourCoord);
		biomeColourCoords.put(ICE_PLAINS, TaigaColourCoord);
		biomeColourCoords.put(ICE_MOUNTAINS, TaigaColourCoord);
		biomeColourCoords.put(MUSHROOM_ISLAND, MushroomColourCoord);
		biomeColourCoords.put(MUSHROOM_ISLAND_SHORE, MushroomColourCoord);
		biomeColourCoords.put(BEACH, DefaultColourCoord);
		biomeColourCoords.put(DESERT_HILLS, DesertColourCoord);
		biomeColourCoords.put(FOREST_HILLS, ForestColourCoord);
		biomeColourCoords.put(TAIGA_HILLS, TaigaColourCoord);
		biomeColourCoords.put(EXTREME_HILLS_EDGE, DefaultColourCoord);
		biomeColourCoords.put(JUNGLE, JungleColourCoord);
		biomeColourCoords.put(JUNGLE_HILLS, JungleColourCoord);
	}
	
	public static Point getColourCoord(final int biomeId)
	{
		Point result = biomeColourCoords.get(biomeId);
		if (result != null)
			return result;
		else
			return DefaultColourCoord;
	}
}
