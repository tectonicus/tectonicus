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
import tectonicus.raw.BlockProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class BlockStateWrapper {
	private List<BlockStateCase> cases = new ArrayList<>();
	private List<BlockVariant> variants = new ArrayList<>();

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
							models.addAll(bsc.getModels());
							break;
						}
					}
				}
			}
		} else {
			for (BlockVariant variant : variants) {
				if (properties.containsAll(variant.getName())) {
					models.addAll(variant.getModels());
					break;
				}
			}
		}
		return models;
	}
}
