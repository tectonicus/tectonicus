/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum Block {
	AIR("minecraft:air"),
	ANCIENT_DEBRIS("minecraft:ancient_debris"),
	BASALT("minecraft:basalt"),
	BEDROCK("minecraft:bedrock"),
	BLACKSTONE("minecraft:blackstone"),
	CHEST("minecraft:chest"),
	LAVA("minecraft:lava"),
	NETHER_GOLD_ORE("minecraft:nether_gold_ore"),
	NETHER_PORTAL("minecraft:nether_portal"),
	NETHER_QUARTZ_ORE("minecraft:nether_quartz_ore"),
	NETHERRACK("minecraft:netherrack"),
	RAIL("minecraft:rail"),
	RESPAWN_ANCHOR("minecraft:respawn_anchor"),
	SOUL_SAND("minecraft:soul_sand"),
	SOUL_SOIL("minecraft:soul_soil"),
	STONE("minecraft:stone"),
	TORCH("minecraft:torch"),
	WALL_TORCH("minecraft:wall_torch"),
	WATER("minecraft:water");

	private static final Map<String, Block> NAME_LOOKUP = new HashMap<>(values().length);
	@Getter
	private final String name;

	Block(String name) {
		this.name = name;
	}

	static
	{
		for (Block block : values()) {
			NAME_LOOKUP.put(block.name, block);
		}
	}

	public static Block byName(String name) {
		return NAME_LOOKUP.get(name.toLowerCase());
	}
}
