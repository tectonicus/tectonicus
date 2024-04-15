/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import java.util.List;
import tectonicus.raw.BlockProperties;

public abstract class BlockState {
	abstract BlockStateModelsWeight getModelsAndWeight();
        abstract void addModels(List<BlockStateModel> models, BlockProperties properties);
        
	//TODO: We need to do similar to whatever Minecraft does so that the same model is always chosen for specific block coordinates
	public static BlockStateModel getRandomWeightedModel(BlockStateModelsWeight modelsAndWeight) {
		int totalWeight = modelsAndWeight.getTextureWeightSum();
		List<BlockStateModel> models = modelsAndWeight.getModels();

		int idx = 0;
		for (double r = Math.random() * totalWeight; idx < models.size() - 1; ++idx) {
			r -= models.get(idx).getWeight();
			if (r <= 0.0) break;
		}

		return models.get(idx);
	}
}
