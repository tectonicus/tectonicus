/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tectonicus.util.Colour4f;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import static tectonicus.world.ColorsWater.COLD;
import static tectonicus.world.ColorsWater.END;
import static tectonicus.world.ColorsWater.LUSH;
import static tectonicus.world.ColorsWater.SNOWY;
import static tectonicus.world.ColorsWater.SWAMPY;
import static tectonicus.world.ColorsWater.TEMPERATE;
import static tectonicus.world.ColorsWater.WARM;

@Getter
@AllArgsConstructor
public enum BiomesOld implements Biome {
	OCEAN(0, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	PLAINS(1, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	DESERT(2, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	MOUNTAINS(3, 0.2f, 0.3f, COLD.getWaterColor()),
	FOREST(4, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	TAIGA(5, 0.25f, 0.8f, COLD.getWaterColor()),
	SWAMP(6, 0.8f, 0.9f, SWAMPY.getWaterColor(), HardcodedColors.SWAMP, HardcodedColors.SWAMP),
	RIVER(7, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	NETHER_WASTES(8, 2.0f, 0.0f, null),
	THE_END(9, 0.5f, 0.5f, END.getWaterColor()),
	FROZEN_OCEAN(10, 0.0f, 0.5f, SNOWY.getWaterColor()),
	FROZEN_RIVER(11, 0.0f, 0.5f, SNOWY.getWaterColor()),
	SNOWY_TUNDRA(12, 0.0f, 0.5f, SNOWY.getWaterColor()),
	SNOWY_MOUNTAINS(13, 0.0f, 0.5f, SNOWY.getWaterColor()),
	MUSHROOM_FIELDS(14, 0.9f, 1.0f, TEMPERATE.getWaterColor()),
	MUSHROOM_FIELDS_SHORE(15, 0.9f, 1.0f, TEMPERATE.getWaterColor()),
	BEACH(16, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	DESERT_HILLS(17, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	WOODED_HILLS(18, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	TAIGA_HILLS(19, 0.25f, 0.8f, COLD.getWaterColor()),
	MOUNTAIN_EDGE(20, 0.2f, 0.3f, COLD.getWaterColor()),
	JUNGLE(21, 0.95f, 0.9f, LUSH.getWaterColor()),
	JUNGLE_HILLS(22, 0.95f, 0.9f, LUSH.getWaterColor()),
	JUNGLE_EDGE(23, 0.95f, 0.8f, LUSH.getWaterColor()),
	DEEP_OCEAN(24, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	STONE_SHORE(25, 0.2f, 0.3f, TEMPERATE.getWaterColor()),
	SNOWY_BEACH(26, 0.05f, 0.3f, SNOWY.getWaterColor()),
	BIRCH_FOREST(27, 0.6f, 0.6f, TEMPERATE.getWaterColor()),
	BIRCH_FOREST_HILLS(28, 0.6f, 0.6f, TEMPERATE.getWaterColor()),
	DARK_FOREST(29, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	SNOWY_TAIGA(30, -0.5f, 0.4f, SNOWY.getWaterColor()),
	SNOWY_TAIGA_HILLS(31, -0.5f, 0.4f, SNOWY.getWaterColor()),
	GIANT_TREE_TAIGA(32, 0.3f, 0.8f, COLD.getWaterColor()),
	GIANT_TREE_TAIGA_HILLS(33, 0.3f, 0.8f, COLD.getWaterColor()),
	WOODED_MOUNTAINS(34, 0.2f, 0.3f, COLD.getWaterColor()),
	SAVANNA(35, 1.2f, 0.0f, TEMPERATE.getWaterColor()),
	SAVANNA_PLATEAU(36, 1.0f, 0.0f, TEMPERATE.getWaterColor()),
	BADLANDS(37, 2.0f, 0.0f, TEMPERATE.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	WOODED_BADLANDS_PLATEAU(38, 2.0f, 0.0f, TEMPERATE.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	BADLANDS_PLATEAU(39, 2.0f, 0.0f, TEMPERATE.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	SMALL_END_ISLANDS(40, 0.5f, 0.5f, END.getWaterColor()),
	END_MIDLANDS(41, 0.5f, 0.5f, END.getWaterColor()),
	END_HIGHLANDS(42, 0.5f, 0.5f, END.getWaterColor()),
	END_BARRENS(43, 0.5f, 0.5f, END.getWaterColor()),
	WARM_OCEAN(44, 0.5f, 0.5f, WARM.getWaterColor()),
	LUKEWARM_OCEAN(45, 0.5f, 0.5f, LUSH.getWaterColor()),
	COLD_OCEAN(46, 0.5f, 0.5f, COLD.getWaterColor()),
	DEEP_WARM_OCEAN(47, 0.5f, 0.5f, WARM.getWaterColor()),
	DEEP_LUKEWARM_OCEAN(48, 0.5f, 0.5f, LUSH.getWaterColor()),
	DEEP_COLD_OCEAN(49, 0.5f, 0.5f, COLD.getWaterColor()),
	DEEP_FROZEN_OCEAN(50, 0.5f, 0.5f, SNOWY.getWaterColor()),
	THE_VOID(127, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	SUNFLOWER_PLAINS(129, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	DESERT_LAKES(130, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	GRAVELLY_MOUNTAINS(131, 0.2f, 0.3f, COLD.getWaterColor()),
	FLOWER_FOREST(132, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	TAIGA_MOUNTAINS(133, 0.25f, 0.8f, COLD.getWaterColor()),
	SWAMP_HILLS(134, 0.8f, 0.9f, SWAMPY.getWaterColor()),
	ICE_SPIKES(140, 0.0f, 0.5f, SNOWY.getWaterColor()),
	MODIFIED_JUNGLE(149, 0.95f, 0.9f, LUSH.getWaterColor()),
	MODIFIED_JUNGLE_EDGE(151, 0.95f, 0.8f, LUSH.getWaterColor()),
	TALL_BIRCH_FOREST(155, 0.6f, 0.6f, TEMPERATE.getWaterColor()),
	TALL_BIRCH_HILLS(156, 0.6f, 0.6f, TEMPERATE.getWaterColor()),
	DARK_FOREST_HILLS(157, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	SNOWY_TAIGA_MOUNTAINS(158, -0.5f, 0.4f, SNOWY.getWaterColor()),
	GIANT_SPRUCE_TAIGA(160, 0.25f, 0.8f, COLD.getWaterColor()),
	GIANT_SPRUCE_TAIGA_HILLS(161, 0.25f, 0.8f, COLD.getWaterColor()),
	MODIFIED_GRAVELLY_MOUNTAINS(162, 0.2f, 0.3f, COLD.getWaterColor()),
	SHATTERED_SAVANNA(163, 1.1f, 0.0f, TEMPERATE.getWaterColor()),
	SHATTERED_SAVANNA_PLATEAU(164, 1.0f, 0.0f, TEMPERATE.getWaterColor()),
	ERODED_BADLANDS(165, 2.0f, 0.0f, TEMPERATE.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	MODIFIED_WOODED_BADLANDS_PLATEAU(166, 2.0f, 0.0f, TEMPERATE.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	MODIFIED_BADLANDS_PLATEAU(167, 2.0f, 0.0f, TEMPERATE.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	BAMBOO_JUNGLE(168, 0.95f, 0.9f, LUSH.getWaterColor()),
	BAMBOO_JUNGLE_HILLS(169, 0.95f, 0.9f, LUSH.getWaterColor()),
	SOUL_SAND_VALLEY(170, 2.0f, 0.0f, null),
	CRIMSON_FOREST(171, 2.0f, 0.0f, null),
	WARPED_FOREST(172, 2.0f, 0.0f, null),
	BASALT_DELTAS(173, 2.0f, 0.0f, null);
	
	BiomesOld(int numericId, float temperature, float rainfall, Colour4f waterColor, Colour4f grassColor, Colour4f foliageColor) {
		this(numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor, grassColor, foliageColor);
	}
	
	BiomesOld(int numericId, float temperature, float rainfall, Colour4f waterColor) {
		this(numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor, null, null);
	}

	private static final Map<Integer, BiomesOld> ID_LOOKUP = new HashMap<>(values().length);

	private final int numericId;
	private final float temperature;
	private final float rainfall;
	private final Point colorCoords;
	private final Colour4f waterColor;
	//Some biomes have hard-coded grass and foliage colors
	private final Colour4f grassColor;
	private final Colour4f foliageColor;

	static
	{
		for (BiomesOld biome : values()) {
			ID_LOOKUP.put(biome.numericId, biome);
		}
	}

	public static BiomesOld byId(int id) {
		return ID_LOOKUP.getOrDefault(id, OCEAN);
	}

	public String getName() {
		return this.toString().toLowerCase();
	}
	
	private static class HardcodedColors {
		public static final Colour4f BADLANDS_GRASS = new Colour4f(144, 129, 77);
		public static final Colour4f BADLANDS_FOLIAGE = new Colour4f(158, 129, 77);
		public static final Colour4f SWAMP = new Colour4f(106, 112, 57);
	}
}
