/*
 * Copyright (c) 2023 Tectonicus contributors.  All rights reserved.
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
import static tectonicus.world.ColorsWater.MANGROVE;
import static tectonicus.world.ColorsWater.CHERRY;

@Getter
@AllArgsConstructor
public enum Biomes implements Biome {
	THE_VOID("the_void", 0, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	PLAINS("plains", 1, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	SUNFLOWER_PLAINS("sunflower_plains", 2, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	SNOWY_PLAINS("snowy_plains", 3, 0.0f, 0.5f, SNOWY.getWaterColor()),
	ICE_SPIKES("ice_spikes", 4, 0.0f, 0.5f, SNOWY.getWaterColor()),
	DESERT("desert", 5, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	SWAMP("swamp", 6, 0.8f, 0.9f, SWAMPY.getWaterColor()),
	MANGROVE_SWAMP("mangrove_swamp", 7, 0.8f, 0.9f, MANGROVE.getWaterColor()),
	FOREST("forest", 8, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	FLOWER_FOREST("flower_forest", 9, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	BIRCH_FOREST("birch_forest", 10, 0.6f, 0.6f, TEMPERATE.getWaterColor()),
	DARK_FOREST("dark_forest", 11, 0.7f, 0.8f, TEMPERATE.getWaterColor()),
	OLD_GROWTH_BIRCH_FOREST("old_growth_birch_forest", 12, 0.6f, 0.6f, TEMPERATE.getWaterColor()),
	OLD_GROWTH_PINE_TAIGA("old_growth_pine_taiga", 13, 0.3f, 0.8f, COLD.getWaterColor()),
	OLD_GROWTH_SPRUCE_TAIGA("old_growth_spruce_taiga", 14, 0.25f, 0.8f, COLD.getWaterColor()),
	TAIGA("taiga", 15, 0.25f, 0.8f, COLD.getWaterColor()),
	SNOWY_TAIGA("snowy_taiga", 16, -0.5f, 0.4f, SNOWY.getWaterColor()),
	SAVANNA("savanna", 17, 1.2f, 0.0f, TEMPERATE.getWaterColor()),
	SAVANNA_PLATEAU("savanna_plateau", 18, 1.0f, 0.0f, TEMPERATE.getWaterColor()),
	WINDSWEPT_HILLS("windswept_hills", 19, 0.2f, 0.3f, COLD.getWaterColor()),
	WINDSWEPT_GRAVELLY_HILLS("windswept_gravelly_hills", 20, 0.2f, 0.3f, COLD.getWaterColor()),
	WINDSWEPT_FOREST("windswept_forest", 21, 0.2f, 0.3f, COLD.getWaterColor()),
	WINDSWEPT_SAVANNA("windswept_savanna", 22, 1.1f, 0.0f, TEMPERATE.getWaterColor()),
	JUNGLE("jungle", 23, 0.95f, 0.9f, LUSH.getWaterColor()),
	SPARSE_JUNGLE("sparse_jungle", 24, 0.95f, 0.8f, LUSH.getWaterColor()),
	BAMBOO_JUNGLE("bamboo_jungle", 25, 0.95f, 0.9f, LUSH.getWaterColor()),
	BADLANDS("badlands", 26, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	ERODED_BADLANDS("eroded_badlands", 27, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	WOODED_BADLANDS("wooded_badlands", 28, 2.0f, 0.0f, TEMPERATE.getWaterColor()),
	MEADOW("meadow", 29, 0.5f, 0.8f, TEMPERATE.getWaterColor()),
	CHERRY_GROVE("cherry_grove", 30, 0.5f, 0.8f, CHERRY.getWaterColor()),
	GROVE("grove", 31, -0.2f, 0.8f, SNOWY.getWaterColor()),
	SNOWY_SLOPES("snowy_slopes", 32, -0.3f, 0.9f, SNOWY.getWaterColor()),
	FROZEN_PEAKS("frozen_peaks", 33, -0.7f, 0.9f, SNOWY.getWaterColor()),
	JAGGED_PEAKS("jagged_peaks", 34, -0.7f, 0.9f, SNOWY.getWaterColor()),
	STONY_PEAKS("stony_peaks", 35, 1.0f, 0.3f, TEMPERATE.getWaterColor()),
	RIVER("river", 36, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	FROZEN_RIVER("frozen_river", 37, 0.0f, 0.5f, SNOWY.getWaterColor()),
	BEACH("beach", 38, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	SNOWY_BEACH("snowy_beach", 39, 0.05f, 0.3f, SNOWY.getWaterColor()),
	STONY_SHORE("stony_shore", 40, 0.2f, 0.3f, TEMPERATE.getWaterColor()),
	WARM_OCEAN("warm_ocean", 41, 0.5f, 0.5f, WARM.getWaterColor()),
	LUKEWARM_OCEAN("lukewarm_ocean", 42, 0.5f, 0.5f, LUSH.getWaterColor()),
	DEEP_LUKEWARM_OCEAN("deep_lukewarm_ocean",43, 0.5f, 0.5f, LUSH.getWaterColor()),
	OCEAN("ocean", 44, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	DEEP_OCEAN("deep_ocean", 45, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	COLD_OCEAN("cold_ocean", 46, 0.5f, 0.5f, COLD.getWaterColor()),
	DEEP_COLD_OCEAN("deep_cold_ocean", 47, 0.5f, 0.5f, COLD.getWaterColor()),
	FROZEN_OCEAN("frozen_ocean", 48, 0.0f, 0.5f, SNOWY.getWaterColor()),
	DEEP_FROZEN_OCEAN("deep_frozen_ocean", 49, 0.5f, 0.5f, SNOWY.getWaterColor()),
	MUSHROOM_FIELDS("mushroom_fields", 50, 0.9f, 1.0f, TEMPERATE.getWaterColor()),
	DRIPSTONE_CAVES("dripstone_caves", 51, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	LUSH_CAVES("lush_caves", 52, 0.5f, 0.5f, TEMPERATE.getWaterColor()),
	DEEP_DARK("deep_dark", 53, 0.8f, 0.4f, TEMPERATE.getWaterColor()),
	NETHER_WASTES("nether_wastes", 54, 2.0f, 0.0f, null),
	WARPED_FOREST("warped_forest", 55, 2.0f, 0.0f, null),
	CRIMSON_FOREST("crimson_forest", 56, 2.0f, 0.0f, null),
	SOUL_SAND_VALLEY("soul_sand_valley", 57, 2.0f, 0.0f, null),
	BASALT_DELTAS("basalt_deltas", 58, 2.0f, 0.0f, null),
	THE_END("the_end", 59, 0.5f, 0.5f, END.getWaterColor()),
	END_HIGHLANDS("end_highlands", 60, 0.5f, 0.5f, END.getWaterColor()),
	END_MIDLANDS("end_midlands", 61, 0.5f, 0.5f, END.getWaterColor()),
	SMALL_END_ISLANDS("small_end_islands", 62, 0.5f, 0.5f, END.getWaterColor()),
	END_BARRENS("end_barrens", 63, 0.5f, 0.5f, END.getWaterColor());

	Biomes(String id, int numericId, float temperature, float rainfall, Colour4f waterColor) {
		this(id, numericId, temperature, rainfall, BiomeUtils.getColorCoords(temperature, rainfall), waterColor);
	}

	private static final Map<String, Biomes> ID_LOOKUP = new HashMap<>(values().length);

	private final String id;
	private final int numericId;
	private final float temperature;
	private final float rainfall;
	private final Point colorCoords;
	private final Colour4f waterColor;

	static {
		for (Biomes biome : values()) {
			ID_LOOKUP.put(biome.id, biome);
		}
	}

	public static Biomes byId(String id) {
		return ID_LOOKUP.getOrDefault(id, OCEAN);
	}
}
