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
		List<BlockStateModel> models = wrapper.getModels(properties);

		assertThat(properties.get("facing"), is("north"));
		assertThat(models, hasSize(1));
		assertThat(models.getFirst().getModel(), is("north_model"));
	}
}
