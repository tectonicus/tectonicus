/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import tectonicus.raw.BlockProperties;

@Getter
@ToString
public class BlockVariant extends BlockState
{
	private final String name;
	private final Map<String, String> states;
	private final BlockStateModelsWeight modelsAndWeight;
	// The states as parallel arrays, so matching a block allocates nothing.
	@Getter(AccessLevel.NONE)
	@ToString.Exclude
	private final String[] stateKeys;
	@Getter(AccessLevel.NONE)
	@ToString.Exclude
	private final String[] stateValues;
	
	public BlockVariant(String name, BlockStateModelsWeight models)
	{
		this.name = name;
		this.states = new HashMap<>();
		this.modelsAndWeight = models;

		String[] statesArray = name.split(",");
		
		for(String s : statesArray)
		{
			String[] state = s.split("=");
			if(state.length > 1)
				states.put(state[0], state[1]);
			else
				states.put(state[0], "");
		}
		stateKeys = states.keySet().toArray(String[]::new);
		stateValues = new String[stateKeys.length];
		for (int i = 0; i < stateKeys.length; i++)
			stateValues[i] = states.get(stateKeys[i]);
	}
        
        @Override
        void addModels(List<BlockStateModel> models, BlockProperties properties, Random random) {
		if (name.equals(StringUtils.EMPTY) || matches(properties)) {
			models.add(getRandomWeightedModel(modelsAndWeight, random));
                }
        }

	/** Whether the block has every state of this variant, like {@code properties.containsAll(states)}. */
	private boolean matches(BlockProperties properties) {
		for (int i = 0; i < stateKeys.length; i++) {
			if (!stateValues[i].equals(properties.get(stateKeys[i])))
				return false;
		}
		return true;
	}
}
