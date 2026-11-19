/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import tectonicus.raw.BlockProperties;

/** How much sky light a Java block takes away when light passes down through it, judged from its name. */
final class SkyLight {
	private static final String[] CLEAR = {
			"glass", "_pane", "iron_bars", "chain", "torch", "lantern", "candle", "_sign", "rail", "carpet", "button",
			"lever", "pressure_plate", "fence", "_wall", "door", "ladder", "vine", "sapling", "flower", "tulip", "orchid",
			"allium", "bluet", "daisy", "dandelion", "poppy", "lilac", "peony", "rose_bush", "sunflower", "fern",
			"short_grass", "tall_grass", "dead_bush", "bush", "sugar_cane", "mushroom", "fungus", "roots", "sprouts",
			"wheat", "carrots", "potatoes", "beetroots", "melon_stem", "pumpkin_stem", "sweet_berry", "bamboo",
			"scaffolding", "banner", "head", "skull", "flower_pot", "pointed_dripstone", "amethyst_cluster", "_bud",
			"lily_pad", "structure_void", "barrier", "snow", "_slab", "_stairs", "redstone_wire", "redstone_torch", "repeater",
			"comparator", "tripwire", "end_rod", "lightning_rod", "cocoa", "nether_wart", "azalea", "dripleaf",
			"spore_blossom", "glow_lichen", "sculk_vein", "hanging_roots", "moss_carpet", "pink_petals", "leaf_litter",
			"wildflowers", "cactus", "frame", "bell", "conduit", "brewing_stand", "enchanting_table", "anvil",
			"campfire", "chest", "_bed", "trapdoor", "farmland", "dirt_path", "eyeblossom", "pale_hanging_moss",
			"firefly_bush", "cactus_flower", "web",
	};
	private static final String[] DIMMING = {"water", "leaves", "ice", "slime_block", "honey_block", "seagrass", "kelp", "coral",
			"bubble_column", "cobweb"};

	private SkyLight() {}

	static int opacity(String name, BlockProperties state) {
		if (state != null && "true".equals(state.get("waterlogged")))
			return 1;
		if (name.equals("minecraft:light"))
			return 0;
		if (name.endsWith("packed_ice") || name.endsWith("blue_ice") || name.endsWith("snow_block"))
			return 15;
		for (String part : DIMMING) {
			if (name.contains(part))
				return 1;
		}
		for (String part : CLEAR) {
			if (name.contains(part))
				return 0;
		}
		return 15;
	}
}
