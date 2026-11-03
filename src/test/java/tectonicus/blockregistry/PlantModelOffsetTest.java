/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlantModelOffsetTest {
	@ParameterizedTest
	@CsvSource({
			"grass, XYZ", "short_grass, XYZ", "fern, XYZ", "short_dry_grass, XYZ", "tall_dry_grass, XYZ",
			"small_dripleaf, SMALL_DRIPLEAF", "dandelion, XZ", "tall_grass, XZ", "large_fern, XZ",
			"sunflower, XZ", "bamboo, XZ", "bamboo_sapling, XZ", "tall_seagrass, XZ",
			"mangrove_propagule, XZ", "hanging_roots, XZ", "golden_dandelion, XZ", "open_eyeblossom, XZ"
	})
	void recognizesVanillaPlantRules(String name, PlantModelOffset expected) {
		assertEquals(expected, new BlockStateWrapper("minecraft:" + name).getModelOffset());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"minecraft:grass_block", "minecraft:potted_dandelion", "minecraft:oak_sapling",
			"minecraft:dead_bush", "minecraft:seagrass", "minecraft:pitcher_crop", "minecraft:wheat",
			"minecraft:pointed_dripstone", "minecraft:future_flower", "example:dandelion"})
	void leavesOtherModelsInPlace(String name) {
		PlantModelOffset rule = PlantModelOffset.forBlock(name);
		assertEquals(PlantModelOffset.NONE, rule);
		assertEquals(0, rule.getOffset(-17, 32).x());
		assertEquals(0, rule.getOffset(-17, 32).y());
		assertEquals(0, rule.getOffset(-17, 32).z());
	}

	@ParameterizedTest
	@CsvSource({
			"0, 0, -0.25, -0.2, -0.25",
			"1, 1, -0.0833333284, -0.0533333309, -0.0833333284",
			"15, 16, -0.116666660, -0.146666661, -0.183333337",
			"-1, -1, -0.0833333284, -0.0533333309, -0.0833333284",
			"-17, 32, 0.150000006, -0.146666661, -0.216666669",
			"1000, -2000, 0.25, -0.119999997, -0.0166666657",
			"29999999, -29999999, -0.150000006, -0.119999997, -0.0833333284"
	})
	void matchesVanillaPositionHashAndOffsetRanges(int x, int z, float dx, float dy, float dz) {
		// Reference values for Mth.getSeed(x, 0, z) and Properties.offsetType (26.3).
		// Far coordinates exercise the intentional 32-bit overflow in x * 3129871.
		var grass = PlantModelOffset.XYZ.getOffset(x, z);
		var flower = PlantModelOffset.XZ.getOffset(x, z);
		var dripleaf = PlantModelOffset.SMALL_DRIPLEAF.getOffset(x, z);
		assertEquals(dx, grass.x(), 0.00000001f);
		assertEquals(dy, grass.y(), 0.00000001f);
		assertEquals(dz, grass.z(), 0.00000001f);
		assertEquals(dx, flower.x(), 0.00000001f);
		assertEquals(0, flower.y(), 0.00000001f);
		assertEquals(dz, flower.z(), 0.00000001f);
		assertEquals(dx, dripleaf.x(), 0.00000001f);
		assertEquals(dy / 2, dripleaf.y(), 0.00000001f);
		assertEquals(dz, dripleaf.z(), 0.00000001f);
	}
}
