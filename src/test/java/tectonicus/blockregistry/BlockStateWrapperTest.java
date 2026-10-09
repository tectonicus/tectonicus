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
import tools.jackson.databind.ObjectMapper;
import tectonicus.raw.BlockProperties;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

class BlockStateWrapperTest {

	@Test
	void deserializesZRotationAndDefaultsItToZero() {
		ObjectMapper mapper = new ObjectMapper();

		BlockStateModel defaultRotation = mapper.readValue("{\"model\":\"default\"}", BlockStateModel.class);
		BlockStateModel zRotation = mapper.readValue("{\"model\":\"rotated\",\"z\":270}", BlockStateModel.class);

		assertThat(defaultRotation.getZRotation(), is(0));
		assertThat(zRotation.getZRotation(), is(270));
	}

	@Test
	void usesDefaultPropertiesWhenPropertiesAreEmpty() {
		BlockStateWrapper wrapper = new BlockStateWrapper("minecraft:test");
		BlockStateModel northModel = new BlockStateModel();
		northModel.setModel("north_model");
		BlockStateModel southModel = new BlockStateModel();
		southModel.setModel("south_model");

		wrapper.addState(new BlockVariant("facing=north", new BlockStateModelsWeight(List.of(northModel))));
		wrapper.addState(new BlockVariant("facing=south", new BlockStateModelsWeight(List.of(southModel))));
		wrapper.setDefaultProperties(Map.of("facing", "north"));

		BlockProperties properties = wrapper.getPropertiesWithDefaults(new BlockProperties());
		List<BlockStateModel> models = wrapper.getModels(properties, 0, 0, 0);

		assertThat(properties.get("facing"), is("north"));
		assertThat(models, hasSize(1));
		assertThat(models.getFirst().getModel(), is("north_model"));
	}

	@Test
	void selectsWeightedModelsDeterministicallyFromBlockPosition() {
		BlockStateWrapper wrapper = new BlockStateWrapper("minecraft:test");
		BlockStateModel firstModel = new BlockStateModel();
		firstModel.setModel("first_model");
		firstModel.setWeight(1);
		BlockStateModel secondModel = new BlockStateModel();
		secondModel.setModel("second_model");
		secondModel.setWeight(3);
		wrapper.addState(new BlockVariant("", new BlockStateModelsWeight(List.of(firstModel, secondModel))));

		List<BlockStateModel> firstSelection = wrapper.getModels(new BlockProperties(), 0, 0, 0);
		List<BlockStateModel> repeatedSelection = wrapper.getModels(new BlockProperties(), 0, 0, 0);

		assertThat(firstSelection, hasSize(1));
		assertThat(firstSelection.getFirst().getModel(), is("second_model"));
		assertThat(repeatedSelection.getFirst().getModel(), is(firstSelection.getFirst().getModel()));
	}

	@Test
	void selectsOneWeightedModelForUnconditionalMultipartCases() {
		BlockStateWrapper wrapper = new BlockStateWrapper("minecraft:test");
		BlockStateModel firstModel = new BlockStateModel();
		firstModel.setModel("first_model");
		firstModel.setWeight(1);
		BlockStateModel secondModel = new BlockStateModel();
		secondModel.setModel("second_model");
		secondModel.setWeight(3);
		wrapper.addState(BlockStateCase.builder()
				.whenClauses(List.of())
				.modelsAndWeight(new BlockStateModelsWeight(List.of(firstModel, secondModel)))
				.build());

		List<BlockStateModel> models = wrapper.getModels(new BlockProperties(), 0, 0, 0);

		assertThat(models, hasSize(1));
		assertThat(models.getFirst().getModel(), is("second_model"));
	}

	@Test
	void resetsPositionRandomForEachMatchingMultipartCase() {
		BlockStateModel firstModel = new BlockStateModel();
		firstModel.setModel("first_model");
		firstModel.setWeight(1);
		BlockStateModel secondModel = new BlockStateModel();
		secondModel.setModel("second_model");
		secondModel.setWeight(3);
		BlockStateModelsWeight weightedModels = new BlockStateModelsWeight(List.of(firstModel, secondModel));
		BlockStateWrapper wrapper = new BlockStateWrapper("minecraft:test");
		wrapper.addState(BlockStateCase.builder().whenClauses(List.of()).modelsAndWeight(weightedModels).build());
		wrapper.addState(BlockStateCase.builder().whenClauses(List.of()).modelsAndWeight(weightedModels).build());

		Random positionRandom = BlockState.createPositionRandom(10, 20, 30);
		String expectedModel = BlockState.getRandomWeightedModel(weightedModels, new Random(positionRandom.nextLong())).getModel();
		List<BlockStateModel> models = wrapper.getModels(new BlockProperties(), 10, 20, 30);

		assertThat(models, hasSize(2));
		assertThat(models.get(0).getModel(), is(expectedModel));
		assertThat(models.get(1).getModel(), is(expectedModel));
	}

	@Test
	void usesMinecraftSeedAnchorsForMultiBlockParts() {
		BlockProperties upperDoor = new BlockProperties(Map.of("half", "upper"));
		BlockProperties lowerDoor = new BlockProperties(Map.of("half", "lower"));
		assertThat(BlockState.createPositionRandom("minecraft:oak_door", upperDoor, 10, 21, 30).nextLong(),
				is(BlockState.createPositionRandom("minecraft:oak_door", lowerDoor, 10, 20, 30).nextLong()));
		assertThat(BlockState.createPositionRandom("minecraft:tall_grass", upperDoor, 10, 21, 30).nextLong(),
				is(BlockState.createPositionRandom("minecraft:tall_grass", lowerDoor, 10, 20, 30).nextLong()));

		BlockProperties footBed = new BlockProperties(Map.of("part", "foot", "facing", "north"));
		BlockProperties headBed = new BlockProperties(Map.of("part", "head", "facing", "north"));
		assertThat(BlockState.createPositionRandom("minecraft:red_bed", footBed, 10, 20, 30).nextLong(),
				is(BlockState.createPositionRandom("minecraft:red_bed", headBed, 10, 20, 29).nextLong()));
	}

	@Test
	void matchesMinecraftPositionSeedIntegerOverflow() {
		int x = 1_234_567;
		int y = -73;
		int z = -2_345_678;
		long seed = (long)(x * 3129871) ^ z * 116129781L ^ y;
		seed = seed * seed * 42317861L + seed * 11L;

		assertThat(BlockState.createPositionRandom(x, y, z).nextLong(),
				is(new Random(seed >> 16).nextLong()));
	}
}
