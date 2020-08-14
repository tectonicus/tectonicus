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
import java.util.Collections;
import java.util.List;

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
		if (!cases.isEmpty()) {
			//get models from cases based on properties
		} else {
			//get models from variants based on properties
		}
		return Collections.emptyList();
	}
}
