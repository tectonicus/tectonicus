/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.chunk;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.Minecraft;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.raw.RawChunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChunkLightingTest {
	@ParameterizedTest
	@CsvSource({"Day,Top,1", "Day,Bottom,0.5", "Day,NorthSouth,0.85", "Day,EastWest,0.7",
			"None,Top,1", "None,Bottom,0.5", "None,NorthSouth,0.85", "None,EastWest,0.7",
			"Night,Top,0.1", "Night,Bottom,0.05", "Night,NorthSouth,0.095", "Night,EastWest,0.09",
			"Cave,Top,0.1", "Cave,Bottom,0.05", "Cave,NorthSouth,0.095", "Cave,EastWest,0.09"})
	void flatLightingRetainsEveryStyleAndFaceFactor(LightStyle style, LightFace face, float expected) {
		assertEquals(expected, Chunk.getLight(style, face, null, 0, 0, 0, 0.1f), 0.000001f);
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void unlitNeighborsBorrowFaceLightWithoutChangingAmbient(LightFace face) {
		LightSample center = sample(15, 12, 0.15f, face);
		LightSample neighbor = sample(0, 0, 0.08f, face);
		LightSample blended = Chunk.applySmoothLightFallback(neighbor, center, face, LightStyle.Night);

		assertEquals(center.sky(), blended.sky());
		assertEquals(center.block(), blended.block());
		assertEquals(neighbor.ambient(), blended.ambient());
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void neighborsWithBlockLightBorrowOnlyMissingSkylight(LightFace face) {
		LightSample center = sample(15, 12, 0.15f, face);
		LightSample neighbor = sample(0, 5, 0.15f, face);
		LightSample blended = Chunk.applySmoothLightFallback(neighbor, center, face, LightStyle.Night);

		assertEquals(center.sky(), blended.sky());
		assertEquals(neighbor.block(), blended.block());
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void existingSkylightIsNotReplaced(LightFace face) {
		LightSample center = sample(15, 12, 0.15f, face);
		LightSample neighbor = sample(1, 0, 0.15f, face);

		assertSame(neighbor, Chunk.applySmoothLightFallback(neighbor, center, face, LightStyle.Night));
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void lightAtOrBelowLevelTwoDoesNotSpread(LightFace face) {
		LightSample neighbor = sample(0, 0, 0.15f, face);
		for (int level = 0; level <= 2; level++) {
			LightSample center = sample(level, level, 0.15f, face);
			assertSame(neighbor, Chunk.applySmoothLightFallback(neighbor, center, face, LightStyle.Night));
		}
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void eitherLightComponentAboveLevelTwoEnablesFallback(LightFace face) {
		LightSample neighbor = sample(0, 0, 0.15f, face);
		LightSample skyCenter = sample(3, 0, 0.15f, face);
		LightSample blockCenter = sample(0, 3, 0.15f, face);

		assertEquals(skyCenter, Chunk.applySmoothLightFallback(neighbor, skyCenter, face, LightStyle.Night));
		assertEquals(blockCenter, Chunk.applySmoothLightFallback(neighbor, blockCenter, face, LightStyle.Night));
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void daySamplesSeparateLightComponentsAndPreserveDirectionalBrightness(LightFace face) {
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		try {
			RawChunk chunk = new RawChunk();
			chunk.setSkyLight(0, 0, 0, (byte) 15);
			chunk.setBlockLight(0, 0, 0, (byte) 0);
			LightSample sample = Chunk.getLightSample(LightStyle.Day, face, chunk, 0, 0, 0, 0);
			assertEquals(daySample(15, 0, face), sample);
			assertEquals(sample, Chunk.getLightSample(LightStyle.Day, face, chunk, 0, 0, 0, 0.3f));
			assertEquals(Chunk.getLight(LightStyle.Day, face, chunk, 0, 0, 0, 0), sample.toColour(LightStyle.Day).r, 0.000001f);
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void dayFallbackUsesTheSameRawLevelThresholdAsNight(LightFace face) {
		LightSample dark = daySample(0, 0, face);
		for (int level = 0; level <= 2; level++)
			assertSame(dark, Chunk.applySmoothLightFallback(dark, daySample(level, level, face), face, LightStyle.Day));
		LightSample skyCenter = daySample(3, 0, face);
		LightSample blockCenter = daySample(0, 3, face);
		assertEquals(skyCenter, Chunk.applySmoothLightFallback(dark, skyCenter, face, LightStyle.Day));
		assertEquals(blockCenter, Chunk.applySmoothLightFallback(dark, blockCenter, face, LightStyle.Day));
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void dayFallbackPreservesExistingBlockLight(LightFace face) {
		LightSample center = daySample(15, 12, face);
		LightSample neighbor = daySample(0, 5, face);
		LightSample result = Chunk.applySmoothLightFallback(neighbor, center, face, LightStyle.Day);
		assertEquals(center.sky(), result.sky());
		assertEquals(neighbor.block(), result.block());
		assertEquals(neighbor.ambient(), result.ambient());
	}

	private LightSample daySample(int sky, int block, LightFace face) {
		float faceFactor = switch (face) {
			case Top -> 1.0f;
			case Bottom -> 0.5f;
			case NorthSouth -> 0.85f;
			case EastWest -> 0.7f;
		};
		return new LightSample(sky * 0.7f / 15 * faceFactor, block / 15.0f * faceFactor, 0.3f * faceFactor);
	}

	private LightSample sample(int sky, int block, float ambient, LightFace face) {
		float faceFactor = switch (face) {
			case Top -> 1.0f;
			case Bottom -> 0.5f;
			case NorthSouth -> 0.95f;
			case EastWest -> 0.9f;
		};
		return new LightSample(sky * 0.24f / 16 * faceFactor, block * 1.4f / 16 * faceFactor, ambient * faceFactor);
	}
}
