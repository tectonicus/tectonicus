/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpawnEggColorsTest {
	@Test
	void readsEggAndSpotColours() {
		assertArrayEquals(new int[] {0xF0A5A2, 0xDB635F}, SpawnEggColors.of("minecraft:pig_spawn_egg"));
		assertArrayEquals(new int[] {0x0DA70B, 0x000000}, SpawnEggColors.of("minecraft:creeper_spawn_egg"));
		assertNotNull(SpawnEggColors.of("minecraft:zombie_pigman_spawn_egg"));
		assertNotNull(SpawnEggColors.of("minecraft:zombified_piglin_spawn_egg"));
		assertNull(SpawnEggColors.of("minecraft:diamond"));
	}

	@Test
	void coversEggsAddedUpTo1_21_3() {
		assertArrayEquals(new int[] {0xEDC343, 0x43241B}, SpawnEggColors.of("minecraft:bee_spawn_egg"));
		assertNotNull(SpawnEggColors.of("minecraft:warden_spawn_egg"));
		assertNotNull(SpawnEggColors.of("minecraft:creaking_spawn_egg"));
	}

	@Test
	void tintsTwoLayerEggsWithoutChangingTheSourceLayers() {
		BufferedImage egg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		egg.setRGB(0, 0, 0xFFFFFFFF);
		BufferedImage spots = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		spots.setRGB(0, 0, 0xFFFFFFFF);
		List<BufferedImage> tinted = SpawnEggColors.tint("minecraft:pig_spawn_egg", List.of(egg, spots));
		assertEquals(0xFFF0A5A2, tinted.get(0).getRGB(0, 0));
		assertEquals(0xFFDB635F, tinted.get(1).getRGB(0, 0));
		assertEquals(0xFFFFFFFF, egg.getRGB(0, 0));

		List<BufferedImage> coloured = List.of(egg);
		assertSame(coloured, SpawnEggColors.tint("minecraft:pig_spawn_egg", coloured));
		List<BufferedImage> notAnEgg = List.of(egg, spots);
		assertSame(notAnEgg, SpawnEggColors.tint("minecraft:diamond", notAnEgg));
	}

	@Test
	void coversTheEggsThatOldWorldsConvertTo() {
		assertNotNull(SpawnEggColors.of(LegacyItems.get().spawnEggItem("Villager")));
		assertNotNull(SpawnEggColors.of(LegacyItems.get().spawnEggItem("minecraft:evocation_illager")));
	}
}
