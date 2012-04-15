/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
