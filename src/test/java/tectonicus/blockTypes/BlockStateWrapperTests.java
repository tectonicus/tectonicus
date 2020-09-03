/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.junit.jupiter.api.Test;
import tectonicus.raw.BlockProperties;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;


class BlockStateWrapperTests {

	@Test
	void wrapperReturnsModelsFromBlockVariant() {
		BlockRegistry test = new BlockRegistry("");
		test.deserializeBlockstates();
		BlockStateWrapper wrapper = test.getBlock("minecraft:acacia_door");

		BlockProperties properties = new BlockProperties();
		properties.put("facing", "south");
		properties.put("half", "lower");
		properties.put("hinge", "left");
		properties.put("open", "false");
		properties.put("powered", "false");

		List<BlockStateModel> models = wrapper.getModels(properties);

		assertFalse(models.isEmpty());
		assertThat(models.size(), is(equalTo(1)));
	}

	@Test
	void wrapperReturnsModelsFromMultipart() {
		BlockRegistry test = new BlockRegistry("");
		test.deserializeBlockstates();
		BlockStateWrapper wrapper = test.getBlock("minecraft:acacia_fence");

		BlockProperties properties = new BlockProperties();
		properties.put("north", "false");
		properties.put("south", "true");
		properties.put("east", "false");
		properties.put("west", "false");
		properties.put("waterlogged", "false");

		List<BlockStateModel> models = wrapper.getModels(properties);

		assertFalse(models.isEmpty());
		assertThat(models.size(), is(equalTo(2)));
		assertThat(models.get(1).getYRotation(), is(equalTo(180)));
	}

	@Test
	void wrapperReturnsModelsFromComplicatedMultipart() {
		BlockRegistry test = new BlockRegistry("");
		test.deserializeBlockstates();
		BlockStateWrapper wrapper = test.getBlock("minecraft:redstone_wire");

		BlockProperties properties = new BlockProperties();
		properties.put("north", "up");
		properties.put("south", "side");
		properties.put("east", "none");
		properties.put("west", "none");
		properties.put("powered", "0");

		List<BlockStateModel> models = wrapper.getModels(properties);

		assertFalse(models.isEmpty());
		assertThat(models.size(), is(equalTo(3)));
		assertThat(models.get(1).getModel(), is(equalTo("minecraft:block/redstone_dust_side_alt0")));
	}
}
