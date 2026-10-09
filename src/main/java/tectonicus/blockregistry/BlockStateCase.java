/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Random;
import tectonicus.raw.BlockProperties;

@Builder
@Getter
@ToString
public class BlockStateCase extends BlockState {
	List<Map<String, String>> whenClauses;
	BlockStateModelsWeight modelsAndWeight;
        
        @Override
        void addModels(List<BlockStateModel> models, BlockProperties properties, Random random) {
                if (whenClauses.isEmpty()) {  // If no when clauses then always apply models
			models.add(getRandomWeightedModel(modelsAndWeight, random));
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
					models.add(getRandomWeightedModel(modelsAndWeight, random));
                                        break;
                                }
                        }
                }
        }
}
