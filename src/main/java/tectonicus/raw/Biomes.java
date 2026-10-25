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
public enum Biomes implements Biome {
	THE_VOID("the_void", 0, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	PLAINS("plains", 1, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	SUNFLOWER_PLAINS("sunflower_plains", 2, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_PLAINS("snowy_plains", 3, 0.0f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	ICE_SPIKES("ice_spikes", 4, 0.0f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	DESERT("desert", 5, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	SWAMP("swamp", 6, 0.8f, 0.9f, ColorsWater.SWAMP.getWaterColor(), HardcodedColors.SWAMP, HardcodedColors.SWAMP, HardcodedColors.DRY_FOLIAGE),
	MANGROVE_SWAMP("mangrove_swamp", 7, 0.8f, 0.9f, ColorsWater.MANGROVE_SWAMP.getWaterColor(), HardcodedColors.SWAMP, HardcodedColors.MANGROVE_FOLIAGE, HardcodedColors.DRY_FOLIAGE),
	FOREST("forest", 8, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	FLOWER_FOREST("flower_forest", 9, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	BIRCH_FOREST("birch_forest", 10, 0.6f, 0.6f, ColorsWater.DEFAULT.getWaterColor()),
	DARK_FOREST("dark_forest", 11, 0.7f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	OLD_GROWTH_BIRCH_FOREST("old_growth_birch_forest", 12, 0.6f, 0.6f, ColorsWater.DEFAULT.getWaterColor()),
	OLD_GROWTH_PINE_TAIGA("old_growth_pine_taiga", 13, 0.3f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	OLD_GROWTH_SPRUCE_TAIGA("old_growth_spruce_taiga", 14, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	TAIGA("taiga", 15, 0.25f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_TAIGA("snowy_taiga", 16, -0.5f, 0.4f, ColorsWater.COLD.getWaterColor()),
	SAVANNA("savanna", 17, 1.2f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	SAVANNA_PLATEAU("savanna_plateau", 18, 1.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	WINDSWEPT_HILLS("windswept_hills", 19, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	WINDSWEPT_GRAVELLY_HILLS("windswept_gravelly_hills", 20, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	WINDSWEPT_FOREST("windswept_forest", 21, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	WINDSWEPT_SAVANNA("windswept_savanna", 22, 1.1f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	JUNGLE("jungle", 23, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	SPARSE_JUNGLE("sparse_jungle", 24, 0.95f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	BAMBOO_JUNGLE("bamboo_jungle", 25, 0.95f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	BADLANDS("badlands", 26, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE, HardcodedColors.BADLANDS_FOLIAGE),
	ERODED_BADLANDS("eroded_badlands", 27, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE, HardcodedColors.BADLANDS_FOLIAGE),
	WOODED_BADLANDS("wooded_badlands", 28, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor(), HardcodedColors.BADLANDS_GRASS, HardcodedColors.BADLANDS_FOLIAGE, HardcodedColors.BADLANDS_FOLIAGE),
	MEADOW("meadow", 29, 0.5f, 0.8f, ColorsWater.MEADOW.getWaterColor()),
	CHERRY_GROVE("cherry_grove", 30, 0.5f, 0.8f, ColorsWater.CHERRY_GROVE.getWaterColor(), HardcodedColors.CHERRY_GROVE, HardcodedColors.CHERRY_GROVE, HardcodedColors.DRY_FOLIAGE),
	GROVE("grove", 31, -0.2f, 0.8f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_SLOPES("snowy_slopes", 32, -0.3f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	FROZEN_PEAKS("frozen_peaks", 33, -0.7f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	JAGGED_PEAKS("jagged_peaks", 34, -0.7f, 0.9f, ColorsWater.DEFAULT.getWaterColor()),
	STONY_PEAKS("stony_peaks", 35, 1.0f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	RIVER("river", 36, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	FROZEN_RIVER("frozen_river", 37, 0.0f, 0.5f, ColorsWater.FROZEN.getWaterColor()),
	BEACH("beach", 38, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	SNOWY_BEACH("snowy_beach", 39, 0.05f, 0.3f, ColorsWater.COLD.getWaterColor()),
	STONY_SHORE("stony_shore", 40, 0.2f, 0.3f, ColorsWater.DEFAULT.getWaterColor()),
	WARM_OCEAN("warm_ocean", 41, 0.5f, 0.5f, ColorsWater.WARM.getWaterColor()),
	LUKEWARM_OCEAN("lukewarm_ocean", 42, 0.5f, 0.5f, ColorsWater.LUKEWARM.getWaterColor()),
	DEEP_LUKEWARM_OCEAN("deep_lukewarm_ocean",43, 0.5f, 0.5f, ColorsWater.LUKEWARM.getWaterColor()),
	OCEAN("ocean", 44, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	DEEP_OCEAN("deep_ocean", 45, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	COLD_OCEAN("cold_ocean", 46, 0.5f, 0.5f, ColorsWater.COLD.getWaterColor()),
	DEEP_COLD_OCEAN("deep_cold_ocean", 47, 0.5f, 0.5f, ColorsWater.COLD.getWaterColor()),
	FROZEN_OCEAN("frozen_ocean", 48, 0.0f, 0.5f, ColorsWater.FROZEN.getWaterColor()),
	DEEP_FROZEN_OCEAN("deep_frozen_ocean", 49, 0.5f, 0.5f, ColorsWater.FROZEN.getWaterColor()),
	MUSHROOM_FIELDS("mushroom_fields", 50, 0.9f, 1.0f, ColorsWater.DEFAULT.getWaterColor()),
	DRIPSTONE_CAVES("dripstone_caves", 51, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	LUSH_CAVES("lush_caves", 52, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	DEEP_DARK("deep_dark", 53, 0.8f, 0.4f, ColorsWater.DEFAULT.getWaterColor()),
	NETHER_WASTES("nether_wastes", 54, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	WARPED_FOREST("warped_forest", 55, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	CRIMSON_FOREST("crimson_forest", 56, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	SOUL_SAND_VALLEY("soul_sand_valley", 57, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	BASALT_DELTAS("basalt_deltas", 58, 2.0f, 0.0f, ColorsWater.DEFAULT.getWaterColor()),
	THE_END("the_end", 59, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	END_HIGHLANDS("end_highlands", 60, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	END_MIDLANDS("end_midlands", 61, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	SMALL_END_ISLANDS("small_end_islands", 62, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	END_BARRENS("end_barrens", 63, 0.5f, 0.5f, ColorsWater.DEFAULT.getWaterColor()),
	PALE_GARDEN("pale_garden", 64, 0.7f, 0.8f, ColorsWater.PALE_GARDEN.getWaterColor(), HardcodedColors.PALE_GARDEN_GRASS, HardcodedColors.PALE_GARDEN_FOLIAGE, HardcodedColors.PALE_GARDEN_DRY_FOLIAGE),
	SULFUR_CAVES("sulfur_caves", 65, 0.8f, 0.4f, ColorsWater.SULFUR_CAVES.getWaterColor(), HardcodedColors.SULFUR_CAVES_GRASS, null, null),
	DAPPLED_FOREST("dappled_forest", 66, 0.6f, 0.6f, ColorsWater.DAPPLED_FOREST.getWaterColor(), HardcodedColors.DAPPLED_FOREST_GRASS, HardcodedColors.DAPPLED_FOREST_FOLIAGE, HardcodedColors.DAPPLED_FOREST_DRY_FOLIAGE),
	ICE_CAVES("ice_caves", 67, 0.0f, 0.0f, ColorsWater.ICE_CAVES.getWaterColor());
	
	Biomes(String id, int numericId, float temperature, float rainfall, Colour4f waterColor, Colour4f grassColor, Colour4f foliageColor, Colour4f dryFoliageColor) {
		this(id, numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor, grassColor, foliageColor, dryFoliageColor);
	}
	
	Biomes(String id, int numericId, float temperature, float rainfall, Colour4f waterColor) {
		this(id, numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor, null, null, null);
	}

	private static final Map<String, Biomes> ID_LOOKUP = HashMap.newHashMap(values().length);

	private final String id;
	private final int numericId;
	private final float temperature;
	private final float rainfall;
	private final Point colorCoords;
	private final Colour4f waterColor;
	//Some biomes have hard-coded grass and foliage colors
	private final Colour4f grassColor;
	private final Colour4f foliageColor;
	private final Colour4f dryFoliageColor;

	static {
		for (Biomes biome : values()) {
			ID_LOOKUP.put(biome.id, biome);
		}
	}

	public static Biomes byId(String id) {
		return ID_LOOKUP.getOrDefault(id, OCEAN);
	}
	
	private static class HardcodedColors {
		public static final Colour4f BADLANDS_GRASS = new Colour4f(new Color(144, 129, 77));
		public static final Colour4f BADLANDS_FOLIAGE = new Colour4f(new Color(158, 129, 77));
		public static final Colour4f SWAMP = new Colour4f(new Color(106, 112, 57));
		public static final Colour4f MANGROVE_FOLIAGE = new Colour4f(new Color(141, 177, 39));
		public static final Colour4f CHERRY_GROVE = new Colour4f(new Color(182, 219, 97));
		public static final Colour4f DRY_FOLIAGE = new Colour4f(new Color(123, 83, 52));
		public static final Colour4f PALE_GARDEN_GRASS = new Colour4f(new Color(119, 130, 114));
		public static final Colour4f PALE_GARDEN_FOLIAGE = new Colour4f(new Color(135, 141, 118));
		public static final Colour4f PALE_GARDEN_DRY_FOLIAGE = new Colour4f(new Color(160, 166, 156));
		public static final Colour4f SULFUR_CAVES_GRASS = new Colour4f(new Color(171, 166, 79));
		public static final Colour4f DAPPLED_FOREST_GRASS = new Colour4f(new Color(223, 104, 39));
		public static final Colour4f DAPPLED_FOREST_FOLIAGE = new Colour4f(new Color(230, 142, 48));
		public static final Colour4f DAPPLED_FOREST_DRY_FOLIAGE = new Colour4f(new Color(140, 58, 4));
	}
}
