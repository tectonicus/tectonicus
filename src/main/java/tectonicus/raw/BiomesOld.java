/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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
import tectonicus.world.ColorsWater;

import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum BiomesOld implements Biome {
	OCEAN(0, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	PLAINS(1, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	DESERT(2, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	MOUNTAINS(3, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	FOREST(4, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	TAIGA(5, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	SWAMP(6, 0.8f, 0.9f, ColorsWater.SWAMP.getWaterColor(), HardcodedColors.SWAMP, HardcodedColors.SWAMP),
	RIVER(7, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	NETHER_WASTES(8, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	THE_END(9, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	FROZEN_OCEAN(10, 0.0f, 0.5f, ColorsWater.FROZEN.getWaterColor()),
	FROZEN_RIVER(11, 0.0f, 0.5f, ColorsWater.FROZEN.getWaterColor()),
	SNOWY_TUNDRA(12, 0.0f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_MOUNTAINS(13, 0.0f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	MUSHROOM_FIELDS(14, 0.9f, 1.0f, ColorsWater.DEFAULT.getWaterColor()),
	MUSHROOM_FIELDS_SHORE(15, 0.9f, 1.0f, ColorsWater.DEFAULT.getWaterColor()),
	BEACH(16, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	DESERT_HILLS(17, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	WOODED_HILLS(18, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	TAIGA_HILLS(19, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	MOUNTAIN_EDGE(20, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	JUNGLE(21, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	JUNGLE_HILLS(22, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	JUNGLE_EDGE(23, 0.95f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	DEEP_OCEAN(24, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	STONE_SHORE(25, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_BEACH(26, 0.05f, 0.3f, ColorsWater.COLD.getWaterColor()),
	BIRCH_FOREST(27, 0.6f, 0.6f, ColorsWater.DEFAULT.getWaterColor()),
	BIRCH_FOREST_HILLS(28, 0.6f, 0.6f, ColorsWater.DEFAULT.getWaterColor()),
	DARK_FOREST(29, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_TAIGA(30, -0.5f, 0.4f, ColorsWater.COLD.getWaterColor()),
	SNOWY_TAIGA_HILLS(31, -0.5f, 0.4f, ColorsWater.COLD.getWaterColor()),
	GIANT_TREE_TAIGA(32, 0.3f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	GIANT_TREE_TAIGA_HILLS(33, 0.3f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	WOODED_MOUNTAINS(34, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	SAVANNA(35, 1.2f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	SAVANNA_PLATEAU(36, 1.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	BADLANDS(37, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	WOODED_BADLANDS_PLATEAU(38, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	BADLANDS_PLATEAU(39, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	SMALL_END_ISLANDS(40, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	END_MIDLANDS(41, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	END_HIGHLANDS(42, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	END_BARRENS(43, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	WARM_OCEAN(44, 0.5f, 0.5f, ColorsWater.WARM.getWaterColor()),
	LUKEWARM_OCEAN(45, 0.5f, 0.5f, ColorsWater.LUKEWARM.getWaterColor()),
	COLD_OCEAN(46, 0.5f, 0.5f, ColorsWater.COLD.getWaterColor()),
	DEEP_WARM_OCEAN(47, 0.5f, 0.5f, ColorsWater.WARM.getWaterColor()),
	DEEP_LUKEWARM_OCEAN(48, 0.5f, 0.5f, ColorsWater.LUKEWARM.getWaterColor()),
	DEEP_COLD_OCEAN(49, 0.5f, 0.5f, ColorsWater.COLD.getWaterColor()),
	DEEP_FROZEN_OCEAN(50, 0.5f, 0.5f, ColorsWater.FROZEN.getWaterColor()),
	THE_VOID(127, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	SUNFLOWER_PLAINS(129, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	DESERT_LAKES(130, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	GRAVELLY_MOUNTAINS(131, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	FLOWER_FOREST(132, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	TAIGA_MOUNTAINS(133, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	SWAMP_HILLS(134, 0.8f, 0.9f, ColorsWater.SWAMP.getWaterColor(), HardcodedColors.SWAMP, HardcodedColors.SWAMP),
	ICE_SPIKES(140, 0.0f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	MODIFIED_JUNGLE(149, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	MODIFIED_JUNGLE_EDGE(151, 0.95f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	TALL_BIRCH_FOREST(155, 0.6f, 0.6f, ColorsWater.DEFAULT.getWaterColor()),
	TALL_BIRCH_HILLS(156, 0.6f, 0.6f, ColorsWater.DEFAULT.getWaterColor()),
	DARK_FOREST_HILLS(157, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_TAIGA_MOUNTAINS(158, -0.5f, 0.4f, ColorsWater.COLD.getWaterColor()),
	GIANT_SPRUCE_TAIGA(160, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	GIANT_SPRUCE_TAIGA_HILLS(161, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	MODIFIED_GRAVELLY_MOUNTAINS(162, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	SHATTERED_SAVANNA(163, 1.1f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	SHATTERED_SAVANNA_PLATEAU(164, 1.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	ERODED_BADLANDS(165, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	MODIFIED_WOODED_BADLANDS_PLATEAU(166, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	MODIFIED_BADLANDS_PLATEAU(167, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE),
	BAMBOO_JUNGLE(168, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	BAMBOO_JUNGLE_HILLS(169, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	SOUL_SAND_VALLEY(170, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	CRIMSON_FOREST(171, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	WARPED_FOREST(172, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	BASALT_DELTAS(173, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor());
	
	BiomesOld(int numericId, float temperature, float rainfall, Colour4f waterColor, Colour4f grassColor, Colour4f foliageColor) {
		this(numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor, grassColor, foliageColor);
	}
	
	BiomesOld(int numericId, float temperature, float rainfall, Colour4f waterColor) {
		this(numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor, null, null);
	}

	private static final Map<Integer, BiomesOld> ID_LOOKUP = HashMap.newHashMap(values().length);

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
		public static final Colour4f BADLANDS_GRASS = new Colour4f(new Color(144, 129, 77));
		public static final Colour4f BADLANDS_FOLIAGE = new Colour4f(new Color(158, 129, 77));
		public static final Colour4f SWAMP = new Colour4f(new Color(106, 112, 57));
	}
}
