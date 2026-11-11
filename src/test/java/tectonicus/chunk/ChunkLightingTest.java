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
	@CsvSource({"0,0,14,14,14", "7,0,19,19,25", "15,0,41,41,71", "0,7,78,61,43", "0,15,252,252,252"})
	void nightUsesTheSharedLightmapForScalarColourAndGrayscale(int sky, int block, int red, int green, int blue) {
		int height = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		try {
			var chunk = new RawChunk();
			chunk.setSkyLight(0, 0, 0, (byte) sky);
			chunk.setBlockLight(0, 0, 0, (byte) block);
			var sample = Chunk.getLightSample(LightStyle.Night, LightFace.Top, chunk, 0, 0, 0, 0.1f);
			assertEquals(sky, sample.sky());
			assertEquals(block, sample.block());
			var colour = sample.toColour(LightStyle.Night);
			assertEquals(red / 255.0f, colour.r, 0.000001f);
			assertEquals(green / 255.0f, colour.g, 0.000001f);
			assertEquals(blue / 255.0f, colour.b, 0.000001f);
			var gray = sample.toColour(LightStyle.Night, true);
			float grayscale = (red * 0.2126f + green * 0.7152f + blue * 0.0722f) / 255;
			assertEquals(grayscale, gray.r, 0.000001f);
			assertEquals(grayscale, Chunk.getLight(LightStyle.Night, LightFace.Top, chunk, 0, 0, 0, 0), 0.000001f);
			assertEquals(gray.r, gray.g);
			assertEquals(gray.r, gray.b);
		} finally {
			Minecraft.setChunkHeight(height);
		}
	}

	@ParameterizedTest
	@CsvSource({"Day,Top,0.98039216", "Day,Bottom,0.49019608", "Day,NorthSouth,0.83333333", "Day,EastWest,0.68627451",
			"None,Top,1", "None,Bottom,0.5", "None,NorthSouth,0.85", "None,EastWest,0.7",
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
	@EnumSource(value = LightStyle.class, names = {"Day", "Night"})
	void neighborsWithBlockLightBorrowOnlyMissingSkylight(LightStyle style) {
		LightSample center = LightSample.overworld(15, 12, 1);
		LightSample neighbor = LightSample.overworld(0, 5, 0.7f);
		LightSample blended = Chunk.applySmoothLightFallback(neighbor, center, LightFace.EastWest, style);

		assertEquals(LightSample.overworld(15, 5, 0.7f), blended);
	}

	@ParameterizedTest
	@EnumSource(LightFace.class)
	void existingSkylightIsNotReplaced(LightFace face) {
		LightSample center = sample(15, 12, 0.15f, face);
		LightSample neighbor = sample(1, 0, 0.15f, face);

		assertSame(neighbor, Chunk.applySmoothLightFallback(neighbor, center, face, LightStyle.Night));
	}

	@ParameterizedTest
	@EnumSource(value = LightStyle.class, names = {"Day", "Night"})
	void lightAtOrBelowLevelTwoDoesNotSpread(LightStyle style) {
		LightSample neighbor = LightSample.overworld(0, 0, 0.7f);
		for (int level = 0; level <= 2; level++) {
			LightSample center = LightSample.overworld(level, level, 0.7f);
			assertSame(neighbor, Chunk.applySmoothLightFallback(neighbor, center, LightFace.EastWest, style));
		}
	}

	@ParameterizedTest
	@EnumSource(value = LightStyle.class, names = {"Day", "Night"})
	void eitherLightComponentAboveLevelTwoEnablesFallback(LightStyle style) {
		LightSample neighbor = LightSample.overworld(0, 0, 0.7f);
		LightSample skyCenter = LightSample.overworld(3, 0, 0.7f);
		LightSample blockCenter = LightSample.overworld(0, 3, 0.7f);

		assertEquals(skyCenter, Chunk.applySmoothLightFallback(neighbor, skyCenter, LightFace.EastWest, style));
		assertEquals(blockCenter, Chunk.applySmoothLightFallback(neighbor, blockCenter, LightFace.EastWest, style));
	}

	private LightSample sample(int sky, int block, float ambient, LightFace face) {
		float faceFactor = switch (face) {
			case Top -> 1.0f;
			case Bottom -> 0.5f;
			case NorthSouth -> 0.95f;
			case EastWest -> 0.9f;
		};
		return LightSample.overworld(sky, block, ambient * faceFactor);
	}
}
