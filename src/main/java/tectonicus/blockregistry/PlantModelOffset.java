/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import lombok.RequiredArgsConstructor;
import tectonicus.util.Vector3f;

/** Vanilla plant displacement, which is defined in Java rather than resource-pack models. */
@RequiredArgsConstructor
public enum PlantModelOffset {
	NONE(0),
	XZ(0),
	XYZ(0.2f),
	SMALL_DRIPLEAF(0.1f);

	private static final Vector3f ZERO = new Vector3f();
	private final float maxVerticalOffset;

	/** Explicit vanilla table; unknown and modded blocks retain their model's placement. */
	public static PlantModelOffset forBlock(String blockName) {
		if (blockName == null)
			return NONE;
		return switch (blockName) {
			case "minecraft:grass", "minecraft:short_grass", "minecraft:fern",
					"minecraft:short_dry_grass", "minecraft:tall_dry_grass" -> XYZ;
			case "minecraft:small_dripleaf" -> SMALL_DRIPLEAF;
			case "minecraft:dandelion", "minecraft:golden_dandelion", "minecraft:poppy",
					"minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet",
					"minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip",
					"minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley",
					"minecraft:wither_rose", "minecraft:torchflower", "minecraft:open_eyeblossom", "minecraft:closed_eyeblossom",
					"minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush", "minecraft:peony",
					"minecraft:tall_grass", "minecraft:large_fern", "minecraft:pitcher_plant", "minecraft:tall_seagrass",
					"minecraft:bamboo", "minecraft:bamboo_sapling", "minecraft:mangrove_propagule",
					"minecraft:crimson_roots", "minecraft:warped_roots", "minecraft:nether_sprouts", "minecraft:hanging_roots" -> XZ;
			default -> NONE;
		};
	}

	public Vector3f getOffset(int worldX, int worldZ) {
		if (this == NONE)
			return ZERO;
		// Both offset types ignore Y, so every part of a vertical plant stays aligned.
		long seed = BlockState.getPositionSeed(worldX, 0, worldZ);
		// Keep Minecraft's float division before promotion to double.
		float x = (float) (((seed & 15L) / 15.0f - 0.5) * 0.5);
		float y = (float) ((((seed >> 4) & 15L) / 15.0f - 1.0) * maxVerticalOffset);
		float z = (float) ((((seed >> 8) & 15L) / 15.0f - 0.5) * 0.5);
		return new Vector3f(x, y, z);
	}
}
