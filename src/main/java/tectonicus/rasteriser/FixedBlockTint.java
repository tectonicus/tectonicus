/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *  
 */

package tectonicus.rasteriser;

import tectonicus.util.Colour4f;

import java.awt.Color;

public enum FixedBlockTint {
	//These block colors are not affected by which biome they are in but for some reason the texture is grayscale so we have to use this hardcoded tint value to apply the correct color
	LILY_PAD("lily_pad", new Colour4f(new Color(32, 128, 48))),
	SPRUCE_LEAVES("spruce_leaves", new Colour4f(new Color(97, 153, 97))),
	BIRCH_LEAVES("birch_leaves", new Colour4f(new Color(128, 167, 85))),
	//For some reason cherry and pale oak leaves use the leaves parent block model which takes a tint even though they do not need a tint
	//other untinted leaves such as azalea and poplar leaves use cube_all which does not take a tint
	CHERRY_LEAVES("cherry_leaves", new Colour4f(Color.WHITE)),
	PALE_OAK_LEAVES("pale_oak_leaves", new Colour4f(Color.WHITE));

	private final String modelName;
	private final Colour4f color;

	FixedBlockTint(String modelName, Colour4f color) {
		this.modelName = modelName;
		this.color = color;
	}

	public static Colour4f find(String modelName) {
		for (FixedBlockTint tint : values()) {
			if (modelName.contains(tint.modelName)) {
				return tint.color;
			}
		}

		return null;
	}
}
