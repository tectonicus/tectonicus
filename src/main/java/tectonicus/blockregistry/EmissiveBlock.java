/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/** Vanilla block light emission rules, resolved from the block's actual state properties. */
public enum EmissiveBlock {
	CAMPFIRE(names("campfire"), state -> bool(state, "lit") ? 15 : 0),
	SOUL_CAMPFIRE(names("soul_campfire"), state -> bool(state, "lit") ? 10 : 0),
	FURNACE(names("furnace", "blast_furnace", "smoker"), state -> bool(state, "lit") ? 13 : 0),
	REDSTONE_LAMP(names("redstone_lamp"), state -> bool(state, "lit") ? 15 : 0),
	REDSTONE_ORE(names("redstone_ore", "deepslate_redstone_ore"), state -> bool(state, "lit") ? 9 : 0),
	REDSTONE_TORCH(names("redstone_torch", "redstone_wall_torch"), state -> bool(state, "lit") ? 7 : 0),
	COPPER_BULB(copperBulbs(), state -> bool(state, "lit") ? 15 : 0),
	CANDLE(candles(), state -> bool(state, "lit") ? 3 * integer(state, "candles", 1) : 0),
	CANDLE_CAKE(candleCakes(), state -> bool(state, "lit") ? 3 : 0),
	SEA_PICKLE(names("sea_pickle"), state -> {
		int pickles = integer(state, "pickles", 0);
		return !bool(state, "waterlogged") || pickles == 0 ? 0 : 3 + 3 * pickles;
	}),
	RESPAWN_ANCHOR(names("respawn_anchor"), state -> switch (integer(state, "charges", 0)) {
		case 1 -> 3;
		case 2 -> 7;
		case 3 -> 11;
		case 4 -> 15;
		default -> 0;
	}),
	END_PORTAL_FRAME(names("end_portal_frame"), state -> bool(state, "eye") ? 1 : 0),
	LIGHT(names("light"), state -> integer(state, "level", 0)),
	CAVE_VINES(names("cave_vines", "cave_vines_plant"), state -> bool(state, "berries") ? 14 : 0),
	SCULK_SENSOR(names("sculk_sensor", "calibrated_sculk_sensor"), state -> "active".equals(state.get("sculk_sensor_phase")) ? 1 : 0),
	SCULK_CATALYST(names("sculk_catalyst"), state -> bool(state, "bloom") ? 6 : 0),
	AMETHYST_CLUSTER(names("amethyst_cluster"), state -> 5),
	ICE_CRYSTAL(names("ice_crystal"), state -> 4),
	LARGE_AMETHYST_BUD(names("large_amethyst_bud"), state -> 4),
	MEDIUM_AMETHYST_BUD(names("medium_amethyst_bud"), state -> 2),
	SMALL_AMETHYST_BUD(names("small_amethyst_bud"), state -> 1),
	ALWAYS_LIT_15(names("lava", "fire", "glowstone", "sea_lantern", "shroomlight", "ochre_froglight",
			"verdant_froglight", "pearlescent_froglight", "jack_o_lantern", "beacon", "conduit",
			"end_portal", "end_gateway", "lava_cauldron", "lantern", "copper_lantern", "exposed_copper_lantern",
			"weathered_copper_lantern", "oxidized_copper_lantern", "waxed_copper_lantern", "waxed_exposed_copper_lantern",
			"waxed_weathered_copper_lantern", "waxed_oxidized_copper_lantern"), state -> 15),
	ALWAYS_LIT_14(names("torch", "wall_torch", "copper_torch", "copper_wall_torch", "end_rod"), state -> 14),
	ALWAYS_LIT_ENDER_CHEST(names("ender_chest"), state -> 7),
	ALWAYS_LIT_11(names("nether_portal"), state -> 11),
	ALWAYS_LIT_10(names("soul_fire", "soul_torch", "soul_wall_torch", "crying_obsidian", "soul_lantern"), state -> 10),
	ALWAYS_LIT_7(names("glow_lichen"), state -> 7),
	ALWAYS_LIT_3(names("magma_block"), state -> 3);

	private static final Map<String, EmissiveBlock> BY_BLOCK_NAME = buildLookup();

	private final List<String> blockNames;
	private final ToIntFunction<Map<String, String>> emission;

	EmissiveBlock(List<String> blockNames, ToIntFunction<Map<String, String>> emission) {
		this.blockNames = blockNames;
		this.emission = emission;
	}

	public int getLightEmission(Map<String, String> state) {
		return Math.max(0, Math.min(15, emission.applyAsInt(state)));
	}

	public static EmissiveBlock forBlockName(String blockName) {
		return BY_BLOCK_NAME.get(blockName);
	}

	private static Map<String, EmissiveBlock> buildLookup() {
		Map<String, EmissiveBlock> result = new HashMap<>();
		for (EmissiveBlock emissiveBlock : values()) {
			for (String blockName : emissiveBlock.blockNames) {
				EmissiveBlock previous = result.put(blockName, emissiveBlock);
				if (previous != null)
					throw new IllegalStateException("Duplicate emissive block registration: " + blockName);
			}
		}
		return Map.copyOf(result);
	}

	private static List<String> names(String... names) {
		return java.util.Arrays.stream(names).map(name -> "minecraft:" + name).toList();
	}

	private static List<String> candles() {
		List<String> colors = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
				"gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
		java.util.ArrayList<String> names = new java.util.ArrayList<>(names("candle"));
		for (String color : colors)
			names.add("minecraft:" + color + "_candle");
		return List.copyOf(names);
	}

	private static List<String> candleCakes() {
		java.util.ArrayList<String> names = new java.util.ArrayList<>(names("candle_cake"));
		for (String candle : candles()) {
			if (!candle.equals("minecraft:candle"))
				names.add(candle + "_cake");
		}
		return List.copyOf(names);
	}

	private static List<String> copperBulbs() {
		return names("copper_bulb", "exposed_copper_bulb", "weathered_copper_bulb", "oxidized_copper_bulb",
				"waxed_copper_bulb", "waxed_exposed_copper_bulb", "waxed_weathered_copper_bulb", "waxed_oxidized_copper_bulb");
	}

	private static boolean bool(Map<String, String> state, String property) {
		return "true".equals(state.get(property));
	}

	private static int integer(Map<String, String> state, String property, int fallback) {
		try {
			return Integer.parseInt(state.getOrDefault(property, Integer.toString(fallback)));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
