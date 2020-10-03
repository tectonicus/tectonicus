/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
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
	BEDROCK("minecraft:bedrock"),
	NETHERRACK("minecraft:netherrack"),
	NETHER_QUARTZ_ORE("minecraft:nether_quartz_ore"),
	BLACKSTONE("minecraft:blackstone"),
	NETHER_GOLD_ORE("minecraft:nether_gold_ore"),
	SOUL_SAND("minecraft:soul_sand"),
	SOUL_SOIL("minecraft:soul_soil"),
	ANCIENT_DEBRIS("minecraft:ancient_debris"),
	LAVA("minecraft:lava"),
	BASALT("minecraft:basalt");

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
