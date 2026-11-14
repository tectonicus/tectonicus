/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import tectonicus.BlockOcclusion;
import tectonicus.BlockTypeRegistry;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.blockTypes.Glass;
import tectonicus.blockTypes.Slab;
import tectonicus.blockTypes.SolidBlockType;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.raw.RawChunk;
import tectonicus.texture.SubTexture;
import tectonicus.world.subset.FullWorldSubset;
import tectonicus.world.subset.HeightRange;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOcclusionTest {
	private int previousHeight;

	@BeforeEach
	void setChunkHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
	}

	@AfterEach
	void restoreChunkHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@Test
	void resolvesNumericDataAndNamedMaterialsIncludingCustomBlocks() {
		var texture = new SubTexture(null, 0, 0, 1, 1, Version.VERSION_4);
		var legacy = new BlockTypeRegistry();
		var solid = new SolidBlockType("Solid", texture);
		legacy.register(1, solid);
		legacy.register(1, 2, new Slab("Slab", texture, texture));
		legacy.register(20, new Glass("Glass", texture));
		legacy.register("custom:stone", solid);
		var models = new BlockRegistry();
		models.getBlockStates().put("minecraft:stone", new BlockStateWrapper("minecraft:stone"));
		var glass = new BlockStateWrapper("minecraft:glass");
		glass.setFullOpaqueBlock(false);
		models.getBlockStates().put("minecraft:glass", glass);
		var ice = new BlockStateWrapper("minecraft:ice");
		ice.setFullOpaqueBlock(false);
		models.getBlockStates().put("minecraft:ice", ice);

		var chunk = new RawChunk();
		chunk.setBlockId(0, 0, 0, 1);
		chunk.setBlockId(1, 0, 0, 1);
		chunk.setBlockData(1, 0, 0, (byte) 2);
		chunk.setBlockId(2, 0, 0, 20);
		chunk.setBlockName(3, 0, 0, "minecraft:stone");
		chunk.setBlockName(4, 0, 0, "minecraft:glass");
		chunk.setBlockName(5, 0, 0, "minecraft:ice");
		chunk.setBlockName(6, 0, 0, "custom:stone");

		var expected = new BlockOcclusion[]{BlockOcclusion.OPAQUE, BlockOcclusion.OPEN, BlockOcclusion.OPEN,
				BlockOcclusion.OPAQUE, BlockOcclusion.OPEN, BlockOcclusion.TRANSLUCENT, BlockOcclusion.OPAQUE};
		for (int x = 0; x < expected.length; x++)
			assertEquals(expected[x], World.resolveBlockOcclusion(chunk, x, 0, 0, legacy, models), "x=" + x);
	}

	@Test
	void namedLegacyOverrideTakesPrecedenceOverJsonModel() {
		String name = "minecraft:stone";
		var texture = new SubTexture(null, 0, 0, 1, 1, Version.VERSION_4);
		var legacy = new BlockTypeRegistry();
		legacy.register(name, new Slab("Custom Stone Slab", texture, texture));
		var models = new BlockRegistry();
		models.getBlockStates().put(name, new BlockStateWrapper(name));
		var chunk = new RawChunk();
		chunk.setBlockName(0, 0, 0, name);

		assertEquals(BlockOcclusion.OPEN, World.resolveBlockOcclusion(chunk, 0, 0, 0, legacy, models));
	}

	@Test
	void slicedNeighborsExposeBothFacesOfTheRetainedBlock() {
		var legacy = new BlockTypeRegistry();
		legacy.register(1, new SolidBlockType("Stone", new SubTexture(null, 0, 0, 1, 1, Version.VERSION_4)));
		var models = new BlockRegistry();
		models.getBlockStates().put("minecraft:stone", new BlockStateWrapper("minecraft:stone"));
		var chunk = new RawChunk();
		chunk.setBlockId(3, 4, 3, 1);
		chunk.setBlockName(3, 5, 3, "minecraft:stone");
		chunk.setBlockName(3, 6, 3, "minecraft:stone");
		assertEquals(BlockOcclusion.OPAQUE, World.resolveBlockOcclusion(chunk, 3, 4, 3, legacy, models));
		assertEquals(BlockOcclusion.OPAQUE, World.resolveBlockOcclusion(chunk, 3, 6, 3, legacy, models));

		var slice = new FullWorldSubset(new HeightRange(5, 5));
		slice.getRenderBlockFilter(chunk.getChunkCoord()).filter(chunk);

		assertEquals(BlockOcclusion.OPEN, World.resolveBlockOcclusion(chunk, 3, 4, 3, legacy, models));
		assertEquals(BlockOcclusion.OPAQUE, World.resolveBlockOcclusion(chunk, 3, 5, 3, legacy, models));
		assertEquals(BlockOcclusion.OPEN, World.resolveBlockOcclusion(chunk, 3, 6, 3, legacy, models));
	}
}
