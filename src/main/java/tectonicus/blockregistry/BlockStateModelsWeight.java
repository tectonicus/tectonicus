/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.Data;

import java.util.List;

@Data
public class BlockStateModelsWeight {
	private final List<BlockStateModel> models;
	private final int textureWeightSum;

	public BlockStateModelsWeight(List<BlockStateModel> models) {
		this.models = models;
		this.textureWeightSum = models.stream().mapToInt(BlockStateModel::getWeight).sum();
	}
}
