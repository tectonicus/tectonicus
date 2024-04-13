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
import tectonicus.raw.BlockProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BlockStateWrapper {
	private final String blockName;
	private final List<BlockState> states = new ArrayList<>();
	@Setter
	private boolean fullBlock = true;
	@Setter
	private boolean isTransparent = false;

	public BlockStateWrapper(String blockName) {
		this.blockName = blockName;
	}

	public void addState(BlockState state) {
		states.add(state);
	}

	public List<BlockStateModel> getModels(BlockProperties properties) {
		List<BlockStateModel> models = new ArrayList<>();
                for (BlockState state : states) {
                        state.addModels(models, properties);
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
