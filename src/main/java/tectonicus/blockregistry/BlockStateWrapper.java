/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import tectonicus.raw.BlockProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class BlockStateWrapper {
	private final String blockName;
	private final List<BlockStateCase> cases = new ArrayList<>();
	private final List<BlockVariant> variants = new ArrayList<>();
	@Setter
	private boolean fullBlock = true;
	@Setter
	private boolean isTransparent = false;

	public BlockStateWrapper(String blockName) {
		this.blockName = blockName;
	}

	public void addCase(BlockStateCase bsc) {
		cases.add(bsc);
	}

	public void addVariant(BlockVariant variant) {
		variants.add(variant);
	}

	public List<BlockStateModel> getModels(BlockProperties properties) {
		List<BlockStateModel> models = new ArrayList<>();
		if (!cases.isEmpty()) {
			for (BlockStateCase bsc : cases) {
				List<Map<String, String>> whenClauses = bsc.getWhenClauses();
				if (whenClauses.isEmpty()) {  // If no when clauses then always apply models
					models.addAll(bsc.getModelsAndWeight().getModels());
				} else {
					for (Map<String, String> clause : whenClauses) {
						boolean addModel = true;
						for (Map.Entry<String, String> entry : clause.entrySet()) {
							String key = entry.getKey();
							if (!(properties.containsKey(key) && entry.getValue().contains(properties.get(key)))) {
								addModel = false;
								break;
							}
						}
						if (addModel) {
							models.add(getRandomWeightedModel(bsc.getModelsAndWeight()));
							break;
						}
					}
				}
			}
		} else {
			for (BlockVariant variant : variants) {
				String variantProperties = variant.getName();
				if (variantProperties.equals(StringUtils.EMPTY) || properties.contains(variantProperties)
						|| properties.containsAll(variant.getStates())) {
					models.add(getRandomWeightedModel(variant.getModelsAndWeight()));
					break;
				}
			}
		}
		return models;
	}

	public List<BlockStateModel> getAllModels() {
		List<BlockStateModel> models = new ArrayList<>();
		if (!cases.isEmpty()) {
			for (BlockStateCase bsc : cases) {
				models.addAll(bsc.getModelsAndWeight().getModels());
			}
		} else {
			for (BlockVariant variant : variants) {
				models.addAll(variant.getModelsAndWeight().getModels());
			}
		}
		return models;
	}

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
