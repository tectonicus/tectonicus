/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.Getter;
import lombok.Setter;
import tectonicus.raw.BlockProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
public class BlockStateWrapper {
	private final String blockName;
	private final List<BlockState> states = new ArrayList<>();
	private Map<String, String> defaultProperties = Collections.emptyMap();
	@Setter
	private boolean fullBlock = true;
	@Setter
	private boolean isTransparent = false;
	@Setter
	private boolean fullOpaqueBlock = true;

	public BlockStateWrapper(String blockName) {
		this.blockName = blockName;
	}

	public void addState(BlockState state) {
		states.add(state);
	}

	public void setDefaultProperties(Map<String, String> defaultProperties) {
		this.defaultProperties = Map.copyOf(defaultProperties);
	}

	public BlockProperties getPropertiesWithDefaults(BlockProperties properties) {
		boolean hasProperties = properties != null && !properties.getProperties().isEmpty();
		if (!hasProperties && !defaultProperties.isEmpty())
			return new BlockProperties(defaultProperties);

		return properties;
	}

	public List<BlockStateModel> getModels(BlockProperties properties, int x, int y, int z) {
		List<BlockStateModel> models = new ArrayList<>();
		Random random = BlockState.createPositionRandom(blockName, properties, x, y, z);
		boolean hasMultipart = states.stream().anyMatch(BlockStateCase.class::isInstance);
		long multipartSeed = hasMultipart ? random.nextLong() : 0;
		Random variantRandom = hasMultipart
				? BlockState.createPositionRandom(blockName, properties, x, y, z)
				: random;
		for (BlockState state : states) {
			Random stateRandom = state instanceof BlockStateCase ? new Random(multipartSeed) : variantRandom;
			state.addModels(models, properties, stateRandom);
		}
		return models;
	}

	public List<BlockStateModel> getAllModels() {
		List<BlockStateModel> models = new ArrayList<>();
		for (BlockState state : states) {
			models.addAll(state.getModelsAndWeight().getModels());
		}
		return models;
	}
}
