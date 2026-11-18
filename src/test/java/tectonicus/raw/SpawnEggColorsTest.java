/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpawnEggColorsTest {
	@Test
	void readsEggAndSpotColours() {
		assertArrayEquals(new int[] {0xF0A5A2, 0xDB635F}, SpawnEggColors.of("minecraft:pig_spawn_egg"));
		assertArrayEquals(new int[] {0x0DA70B, 0x000000}, SpawnEggColors.of("minecraft:creeper_spawn_egg"));
		assertArrayEquals(SpawnEggColors.of("minecraft:zombie_pigman_spawn_egg"), SpawnEggColors.of("minecraft:zombified_piglin_spawn_egg"));
		assertNull(SpawnEggColors.of("minecraft:diamond"));
	}

	@Test
	void coversTheEggsThatOldWorldsConvertTo() {
		assertNotNull(SpawnEggColors.of(LegacyItems.get().spawnEggItem("Villager")));
		assertNotNull(SpawnEggColors.of(LegacyItems.get().spawnEggItem("minecraft:evocation_illager")));
	}
}
