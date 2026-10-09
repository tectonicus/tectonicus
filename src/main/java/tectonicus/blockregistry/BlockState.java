/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import java.util.List;
import java.util.Random;
import java.util.Set;
import tectonicus.raw.BlockProperties;

public abstract class BlockState {
	private static final Set<String> DOUBLE_BLOCK_PLANTS = Set.of(
			"large_fern", "lilac", "peony", "pitcher_plant", "rose_bush",
			"pitcher_crop", "small_dripleaf", "sunflower", "tall_grass", "tall_seagrass");

	abstract BlockStateModelsWeight getModelsAndWeight();
	abstract void addModels(List<BlockStateModel> models, BlockProperties properties, Random random);

	public static Random createPositionRandom(int x, int y, int z) {
		return new Random(getPositionSeed(x, y, z));
	}

	public static Random createPositionRandom(String blockName, BlockProperties properties, int x, int y, int z) {
		String name = blockName.substring(blockName.indexOf(':') + 1);
		if (properties != null) {
			if ((name.endsWith("_door") || DOUBLE_BLOCK_PLANTS.contains(name))
					&& "upper".equals(properties.get("half"))) {
				y--;
			} else if (name.endsWith("_bed") && "foot".equals(properties.get("part"))) {
				String facing = properties.get("facing");
				switch (facing == null ? "" : facing) {
					case "north" -> z--;
					case "south" -> z++;
					case "west" -> x--;
					case "east" -> x++;
					default -> {
					}
				}
			}
		}
		return createPositionRandom(x, y, z);
	}

	private static long getPositionSeed(int x, int y, int z) {
		long seed = (long)(x * 3129871) ^ z * 116129781L ^ y;
		seed = seed * seed * 42317861L + seed * 11L;
		return seed >> 16;
	}

	public static BlockStateModel getRandomWeightedModel(BlockStateModelsWeight modelsAndWeight, Random random) {
		int totalWeight = modelsAndWeight.getTextureWeightSum();
		List<BlockStateModel> models = modelsAndWeight.getModels();

		int selection = random.nextInt(totalWeight);
		for (BlockStateModel model : models) {
			selection -= model.getWeight();
			if (selection < 0) {
				return model;
			}
		}

		throw new IllegalStateException("No model selected from weighted model list");
	}
}
