/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.world.World;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockStateModel {
	private String model = "";  //TODO: can we make this be the actual BlockModel?
	@JsonProperty("x")
	private int xRotation = 0;
	@JsonProperty("y")
	private int yRotation = 0;
	private int weight = 1;
	private boolean uvlock = false;

	public void createGeometry(int x, int y, int z, World world, BlockRegistry registry, RawChunk rawChunk, Geometry geometry) {
		BlockModel blockModel = registry.getModel(model);
		blockModel.createGeometry(x, y, z, world, rawChunk, geometry, xRotation, yRotation);  //TODO: pass in weight and uvlock
	}
}
