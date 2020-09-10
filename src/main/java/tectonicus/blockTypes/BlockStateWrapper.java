/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import tectonicus.raw.BlockProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
public class BlockStateWrapper {
	private final List<BlockStateCase> cases = new ArrayList<>();
	private final List<BlockVariant> variants = new ArrayList<>();
	private final Random random = new Random();
	@Setter
	private boolean fullBlock = true;

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
					models.addAll(bsc.getModels());
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
							models.add(getBlockStateModel(bsc));
							break;
						}
					}
				}
			}
		} else {
			for (BlockVariant variant : variants) {
				String variantProperties = variant.getName();
				if (variantProperties.equals(StringUtils.EMPTY) || properties.containsAll(variantProperties)) {
					models.add(getBlockStateModel(variant));
					break;
				}
			}
		}
		return models;
	}

	private BlockStateModel getBlockStateModel(BlockState state) {
		List<BlockStateModel> variantModels = state.getModels();
		int size = variantModels.size();
		if (size > 1) {
			return variantModels.get(random.nextInt(size));
		} else {
			return variantModels.get(0);
		}
	}
}
