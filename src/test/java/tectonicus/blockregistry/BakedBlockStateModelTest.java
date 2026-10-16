/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import org.junit.jupiter.api.Test;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.texture.SubTexture;
import tectonicus.util.Vector3f;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.not;

class BakedBlockStateModelTest {
	@Test
	void cachesByModelRotationAndUvLockSettings() {
		BlockRegistry registry = new BlockRegistry();
		BlockModel model = new BlockModel("minecraft:block/test", false, null, null, null);

		BakedBlockStateModel first = registry.getBakedModel(model, 0, 0, 0, false);
		BakedBlockStateModel repeated = registry.getBakedModel(model, 0, 0, 0, false);
		BakedBlockStateModel rotated = registry.getBakedModel(model, 0, 90, 0, false);
		BakedBlockStateModel uvLocked = registry.getBakedModel(model, 0, 0, 0, true);

		assertThat(repeated, sameInstance(first));
		assertThat(rotated, not(sameInstance(first)));
		assertThat(uvLocked, not(sameInstance(first)));
	}

	@Test
	void bakedCandidateRetainsItsBlockstateTransform() {
		BlockModel model = new BlockModel("minecraft:block/test", false, null, null, null);
		BakedBlockStateModel baked = new BakedBlockStateModel(model, 90, 180, 270, true);

		assertThat(baked.getSourceModel(), sameInstance(model));
		assertThat(baked.getXRotation(), org.hamcrest.Matchers.is(90));
		assertThat(baked.getYRotation(), org.hamcrest.Matchers.is(180));
		assertThat(baked.getZRotation(), org.hamcrest.Matchers.is(270));
		assertThat(baked.isUvlock(), org.hamcrest.Matchers.is(true));
		assertThat(baked.getQuads(), org.hamcrest.Matchers.empty());
	}

	@Test
	void bakesBlockstateRotationIntoFaceVerticesAndUvLock() {
		ElementFace up = new ElementFace(new SubTexture(null, 0.1f, 0.2f, 0.9f, 0.8f), "up", 0, false, false, false);
		BlockElement element = new BlockElement(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16),
				new org.joml.Vector3f(8, 8, 8), 0, 0, 0, false, true, Map.of("up", up));
		BlockModel model = new BlockModel("minecraft:block/test", false, List.of(element));

		BakedBlockStateModel rotated = new BakedBlockStateModel(model, 0, 90, 0, false);
		BakedBlockStateModel uvLocked = new BakedBlockStateModel(model, 0, 90, 0, true);

		assertThat(rotated.getQuads(), org.hamcrest.Matchers.hasSize(1));
		assertThat((double) rotated.getQuads().getFirst().getPositions()[0], org.hamcrest.Matchers.closeTo(1.0, 0.0001));
		assertThat((double) rotated.getQuads().getFirst().getPositions()[1], org.hamcrest.Matchers.closeTo(1.0, 0.0001));
		assertThat((double) rotated.getQuads().getFirst().getPositions()[2], org.hamcrest.Matchers.closeTo(0.0, 0.0001));
		assertThat(java.util.Arrays.equals(uvLocked.getQuads().getFirst().getUvs(), rotated.getQuads().getFirst().getUvs()),
				org.hamcrest.Matchers.is(false));
	}

}
