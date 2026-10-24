/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.raw.BlockProperties;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRegistryAttributesTest {
	@Test
	void beaconKeepsFullCollisionShadeWithoutBecomingRenderOpaque() {
		BlockStateWrapper block = new BlockStateWrapper("minecraft:beacon");
		block.addState(new BlockVariant("", new BlockStateModelsWeight(List.of(model("beacon", false, false)))));
		block.setEmissiveBlock(EmissiveBlock.forBlockName(block.getBlockName()));

		BlockRegistry.setBlockAttributes(block);

		assertFalse(block.isFullBlock(), "The model's inset parts still need partial-face lighting");
		assertFalse(block.isFullOpaqueBlock(), "Beacon rendering remains transparent");
		assertEquals(15, block.getLightEmission(new BlockProperties()));
		assertEquals(0.2f, block.getShadeBrightness());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void seaPickleLightEmissionRequiresWaterlogging(int pickles) {
		BlockStateWrapper block = new BlockStateWrapper("minecraft:sea_pickle");
		block.setDefaultProperties(Map.of("pickles", "1", "waterlogged", "true"));
		block.setEmissiveBlock(EmissiveBlock.forBlockName(block.getBlockName()));
		assertEquals(3 + 3 * pickles, block.getLightEmission(new BlockProperties(Map.of("pickles", Integer.toString(pickles)))));
		assertEquals(0, block.getLightEmission(new BlockProperties(Map.of("pickles", Integer.toString(pickles), "waterlogged", "false"))));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void bookshelfBodyStillOccludesWhenBookOverlaysArePartial(boolean overlayFirst) {
		BlockStateWrapper block = new BlockStateWrapper("minecraft:chiseled_bookshelf");
		BlockStateModel body = model("body", true, true);
		BlockStateModel books = model("books", false, false);
		BlockStateCase bodyCase = part(body, Map.of("facing", "north"));
		BlockStateCase bookCase = part(books, Map.of("facing", "north", "slot_0_occupied", "true"));
		block.addState(overlayFirst ? bookCase : bodyCase);
		block.addState(overlayFirst ? bodyCase : bookCase);

		BlockRegistry.setBlockAttributes(block);

		assertFalse(block.isFullBlock(), "Overlay geometry should still use partial-face lighting");
		assertFalse(books.getBlockModel().isFullBlock());
		assertTrue(block.isFullOpaqueBlock(), "The opaque cube body still covers neighboring faces");
		assertEquals(0.2f, block.getShadeBrightness());
	}

	@Test
	void alternativesDoNotCombineSingleAndDoubleSlabsIntoAnOpaqueCube() {
		BlockStateWrapper block = new BlockStateWrapper("minecraft:oak_slab");
		block.addState(new BlockVariant("type=double", new BlockStateModelsWeight(List.of(model("double", true, true)))));
		block.addState(new BlockVariant("type=bottom", new BlockStateModelsWeight(List.of(model("bottom", false, true)))));

		BlockRegistry.setBlockAttributes(block);

		assertFalse(block.isFullBlock());
		assertFalse(block.isFullOpaqueBlock());
		assertEquals(1.0f, block.getShadeBrightness());
	}

	@Test
	void partialMultipartPiecesDoNotBecomeFullOccluders() {
		BlockStateWrapper block = new BlockStateWrapper("minecraft:oak_fence");
		block.addState(part(model("post", false, true), Map.of()));
		block.addState(part(model("side", false, true), Map.of("north", "true")));

		BlockRegistry.setBlockAttributes(block);

		assertFalse(block.isFullOpaqueBlock());
		assertEquals(1.0f, block.getShadeBrightness());
	}

	@Test
	void transparentFullModelDoesNotCreateAnOpaqueMultipartBody() {
		BlockStateWrapper block = new BlockStateWrapper("minecraft:glass");
		block.addState(part(model("glass", true, false), Map.of()));
		block.addState(part(model("overlay", false, false), Map.of()));

		BlockRegistry.setBlockAttributes(block);

		assertFalse(block.isFullOpaqueBlock());
		assertEquals(1.0f, block.getShadeBrightness());
	}

	private BlockStateCase part(BlockStateModel model, Map<String, String> condition) {
		return BlockStateCase.builder().whenClauses(condition.isEmpty() ? List.of() : List.of(condition))
				.modelsAndWeight(new BlockStateModelsWeight(List.of(model))).build();
	}

	private BlockStateModel model(String name, boolean fullBlock, boolean solid) {
		BlockModel model = new BlockModel("minecraft:block/" + name, true, List.of());
		model.setFullBlock(fullBlock);
		model.setSolid(solid);
		BlockStateModel stateModel = new BlockStateModel();
		stateModel.setBlockModel(model);
		return stateModel;
	}
}
