/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.Biomes;
import tectonicus.raw.BiomesOld;

import static org.junit.jupiter.api.Assertions.*;

class SwampGrassColorTest {
	// Expected colors sampled directly from Minecraft 1.12.2's biome information noise.
	@ParameterizedTest
	@CsvSource({
			"0,0,0x6A7039", "1,1,0x6A7039", "-1,-1,0x6A7039",
			"15,0,0x6A7039", "16,0,0x6A7039", "-16,0,0x4C763C", "-17,0,0x4C763C",
			"0,15,0x4C763C", "0,16,0x4C763C", "0,-16,0x4C763C", "0,-17,0x4C763C",
			"100,200,0x4C763C", "-100,200,0x6A7039", "100,-200,0x6A7039", "-100,-200,0x4C763C",
			"571,-10,0x6A7039", "580,-30,0x6A7039", "1024,1024,0x6A7039", "-1024,-1024,0x6A7039",
			"29999999,29999999,0x6A7039", "-29999999,29999999,0x6A7039",
			"29999999,-29999999,0x6A7039", "-29999999,-29999999,0x4C763C"
	})
	void matchesVanillaAtWorldCoordinatesAndAcrossChunkEdges(int x, int z, int expected) {
		ChunkCoord chunk = ChunkCoord.fromWorldCoord(x, z);
		var color = SwampGrassColor.getColor(chunk, x & 15, z & 15);
		assertEquals(expected, color.toRgb());
		assertEquals(1.0f, color.a);
		// A blend sample can also address the same block outside the original chunk.
		assertEquals(expected, SwampGrassColor.getColor(new ChunkCoord(chunk.x + 1, chunk.z - 1),
				(x & 15) - 16, (z & 15) + 16).toRgb());
	}

	@Test
	void appliesToSwampsAndMangrovesIncludingLegacyHills() {
		assertTrue(SwampGrassColor.applies(Biomes.SWAMP));
		assertTrue(SwampGrassColor.applies(Biomes.MANGROVE_SWAMP));
		assertTrue(SwampGrassColor.applies(BiomesOld.SWAMP));
		assertTrue(SwampGrassColor.applies(BiomesOld.SWAMP_HILLS));
		assertFalse(SwampGrassColor.applies(Biomes.FOREST));
		assertFalse(SwampGrassColor.applies(BiomesOld.FOREST));
	}

	@ParameterizedTest
	@CsvSource({"0,0x6A7039", "1,0x5C723A", "2,0x5A733A", "3,0x5A733A",
			"4,0x59733A", "5,0x59733A", "6,0x58733A", "7,0x58733A"})
	void blendsPositionDependentColorsBeforeAveraging(int radius, int expected) {
		var result = BiomeColorBlender.blend(0, 14, radius,
				(x, z) -> SwampGrassColor.getColor(new ChunkCoord(), x, z));
		assertEquals(expected, result.toRgb());
	}

	@Test
	void foliageOverridesRemainConstantAndDistinctFromGrass() {
		assertEquals(0x6A7039, Biomes.SWAMP.getFoliageColor().toRgb());
		assertEquals(0x8DB127, Biomes.MANGROVE_SWAMP.getFoliageColor().toRgb());
		assertEquals(0x7B5334, Biomes.SWAMP.getDryFoliageColor().toRgb());
		assertEquals(0x7B5334, Biomes.MANGROVE_SWAMP.getDryFoliageColor().toRgb());
		assertEquals(0x6A7039, BiomesOld.SWAMP.getFoliageColor().toRgb());
		assertEquals(0x6A7039, BiomesOld.SWAMP_HILLS.getFoliageColor().toRgb());
	}
}
