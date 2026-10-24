/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.texture.SubTexture;
import tectonicus.util.Vector3f;

import java.util.List;
import java.util.Map;

public final class LightingModelFixture {
	private LightingModelFixture() {}

	public static BakedBlockStateModel.BakedQuad face(String direction, Vector3f from, Vector3f to) {
		return face(direction, from, to, true, null);
	}

	public static BakedBlockStateModel.BakedQuad face(String direction, Vector3f from, Vector3f to, boolean shaded, String shadeOverride) {
		ElementFace face = new ElementFace(new SubTexture(null, 0, 0, 1, 1), null, 0, false, false, false);
		BlockElement element = new BlockElement(from, to, new org.joml.Vector3f(8, 8, 8),
				0, 0, 0, false, shaded, shadeOverride, Map.of(direction, face));
		BlockModel model = new BlockModel("minecraft:block/lighting_test", true, List.of(element));
		return new BakedBlockStateModel(model, 0, 0, 0, false).getQuads().getFirst();
	}
}
