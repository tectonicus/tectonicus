/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import tectonicus.configuration.Configuration;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@ToString
public class BlockStateCase {
	List<Map<String, String>> whenClauses;
	List<BlockStateModel> models;

	public static void main(String[] args) {
		int width = 800;
		int height = 800;

		Rasteriser rasteriser = RasteriserFactory.createRasteriser(Configuration.RasteriserType.LWJGL, RasteriserFactory.DisplayType.Window, width, height, 24, 8, 24, 4);

		BlockRegistry br = new BlockRegistry(rasteriser);
		//br.deserializeMultipartBlockStates();
	}
}
