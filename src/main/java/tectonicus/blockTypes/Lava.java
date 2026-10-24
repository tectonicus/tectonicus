/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import tectonicus.BlockContext;
import tectonicus.raw.RawChunk;
import tectonicus.texture.SubTexture;

public class Lava extends Fluid {
	public Lava(String name, SubTexture stillTexture, SubTexture flowTexture, int frame) {
		super(name, stillTexture, flowTexture, frame);
	}

	@Override
	public boolean isWater() { return false; }

	@Override
	protected boolean containsFluid(BlockContext world, RawChunk chunk, int x, int y, int z) {
		return world.getBlockType(chunk, x, y, z) instanceof Lava;
	}
}
