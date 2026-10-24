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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.texture.SubTexture;
import tectonicus.util.Vector3f;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
	@ParameterizedTest
	@CsvSource({
			"0, 0, 0, down",
			"90, 0, 0, north",
			"-90, 0, 0, south",
			"180, 0, 0, up",
			"0, 0, 115, east",
			"0, 0, -115, west",
			"0, 90, 0, down",
			"30, 0, 60, east"
	})
	void bottomFaceLightingFollowsElementRotation(float xRotation, float yRotation, float zRotation, String expected) {
		BlockModel model = bottomFaceModel(xRotation, yRotation, zRotation);
		BakedBlockStateModel.BakedQuad quad = new BakedBlockStateModel(model, 0, 0, 0, false).getQuads().getFirst();

		assertEquals("down", quad.getSourceFace());
		assertEquals(expected, quad.getDirection());
	}

	@ParameterizedTest
	@CsvSource({"0, north", "90, east", "180, south", "270, west"})
	void bottomFaceLightingCombinesElementAndBlockstateRotation(int yRotation, String expected) {
		BlockModel model = bottomFaceModel(90, 0, 0);
		BakedBlockStateModel.BakedQuad quad = new BakedBlockStateModel(model, 0, yRotation, 0, false).getQuads().getFirst();

		assertEquals(expected, quad.getDirection());
		// Cull directions are declared separately and follow only blockstate rotation.
		assertEquals("down", quad.getCullDirection());
	}

	private BlockModel bottomFaceModel(float xRotation, float yRotation, float zRotation) {
		ElementFace down = new ElementFace(new SubTexture(null, 0, 0, 1, 1), "down", 0, false, false, false);
		BlockElement element = new BlockElement(new Vector3f(4, 0, 4), new Vector3f(12, 8, 12),
				new org.joml.Vector3f(8, 8, 8), xRotation, yRotation, zRotation, false, true, Map.of("down", down));
		return new BlockModel("minecraft:block/test", false, List.of(element));
	}
}
