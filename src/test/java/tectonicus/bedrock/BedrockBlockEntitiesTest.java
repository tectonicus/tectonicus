/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.junit.jupiter.api.Test;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.StringTag;
import tectonicus.Minecraft;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.world.DimensionHeight;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockBlockEntitiesTest {
	private static String pot(String plant) {
		DimensionHeight previous = Minecraft.getDimensionHeight();
		Minecraft.setDimensionHeight(DimensionHeight.CAVES_AND_CLIFFS);
		try {
			return potIn(plant);
		} finally {
			Minecraft.setDimensionHeight(previous);
		}
	}

	private static String potIn(String plant) {
		RawChunk chunk = new RawChunk(0, 0, 0);
		chunk.setBlockName(1, 2, 3, "minecraft:flower_pot");
		chunk.setBlockState(1, 2, 3, new BlockProperties());
		Neighbourhood blocks = Neighbourhood.of(new Blocks(0, 0, 0, new BedrockBlockMapper.JavaState[1][]));
		BedrockBlockEntities converter = new BedrockBlockEntities(chunk, blocks, BedrockBlockMapper.get());
		CompoundTag plantBlock = new CompoundTag("PlantBlock", Map.of("name", new StringTag("name", plant),
				"states", new CompoundTag("states", Map.of())));
		converter.convert("FlowerPot", new CompoundTag("", Map.of("PlantBlock", plantBlock)), 1, 2, 3);
		return chunk.getBlockName(1, 2, 3);
	}

	@Test
	void pottedPlants() {
		assertEquals("minecraft:potted_poppy", pot("minecraft:poppy"));
		assertEquals("minecraft:flower_pot", pot("minecraft:air"));
	}
}
