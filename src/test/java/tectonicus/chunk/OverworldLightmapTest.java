/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.chunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.Minecraft;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.raw.RawChunk;

import static org.junit.jupiter.api.Assertions.*;

class OverworldLightmapTest {
	private int previousHeight;

	@BeforeEach void setChunkHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
	}

	@AfterEach void restoreChunkHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@ParameterizedTest
	@CsvSource({"0,14", "7,57", "11,110", "15,250"})
	void matchesMinecraft10GammaZeroSkylight(int sky, int expectedByte) {
		var colour = OverworldLightmap.forBrightness(0).sample(LightStyle.Day, sky, 0);
		assertEquals(expectedByte / 255.0f, colour.r, 0.000001f);
		assertEquals(colour.r, colour.g);
		assertEquals(colour.r, colour.b);
	}

	@Test
	void blockLightIsWarmAndBothContributionsSaturate() {
		var torch = OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 0, 7);
		assertTrue(torch.r > torch.g && torch.g > torch.b);
		var saturated = OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 15, 15);
		assertEquals(252 / 255.0f, saturated.r, 0.000001f);
		assertEquals(saturated.r, saturated.g);
		assertEquals(saturated.r, saturated.b);
		for (int sky = 0; sky < 16; sky++) {
			for (int block = 0; block < 16; block++) {
				var c = OverworldLightmap.forBrightness(0).sample(LightStyle.Day, sky, block);
				assertTrue(c.r >= 0 && c.r <= 1 && c.g >= 0 && c.g <= 1 && c.b >= 0 && c.b <= 1);
				if (sky > 0) assertTrue(c.r >= OverworldLightmap.forBrightness(0).sample(LightStyle.Day, sky - 1, block).r);
				if (block > 0) assertTrue(c.r >= OverworldLightmap.forBrightness(0).sample(LightStyle.Day, sky, block - 1).r);
			}
		}
	}

	@ParameterizedTest
	@CsvSource({"0,0.5,25", "0,1,35", "7,0.5,105", "7,1,153", "11,0.5,167", "11,1,224", "15,0.5,251", "15,1,252"})
	void brightnessMatchesModernGammaSkylight(int sky, float brightness, int expectedByte) {
		var colour = OverworldLightmap.forBrightness(brightness).sample(LightStyle.Day, sky, 0);
		assertEquals(expectedByte / 255.0f, colour.r, 0.000001f);
		assertEquals(colour.r, colour.g);
		assertEquals(colour.r, colour.b);
	}

	@Test
	void brightnessPreservesWarmHueAndBoostsAllLightLevelsMonotonically() {
		var bright = OverworldLightmap.forBrightness(1);
		var middle = OverworldLightmap.forBrightness(0.5f);
		for (int sky = 0; sky < 16; sky++) {
			for (int block = 0; block < 16; block++) {
				var moody = OverworldLightmap.forBrightness(0).sample(LightStyle.Day, sky, block);
				var half = middle.sample(LightStyle.Day, sky, block);
				var full = bright.sample(LightStyle.Day, sky, block);
				assertTrue(moody.r <= half.r && half.r <= full.r);
				assertTrue(moody.g <= half.g && half.g <= full.g);
				assertTrue(moody.b <= half.b && half.b <= full.b);
			}
		}
		var moody = OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 0, 7);
		var full = bright.sample(LightStyle.Day, 0, 7);
		assertTrue(full.r > full.g && full.g > full.b);
		// Remove the final .03 offset; the remaining RGB ratios stay constant,
		// allowing for the lightmap's eight-bit quantization.
		assertEquals((moody.g - 0.03f) / (moody.r - 0.03f), (full.g - 0.03f) / (full.r - 0.03f), 0.03f);
		assertEquals((moody.b - 0.03f) / (moody.r - 0.03f), (full.b - 0.03f) / (full.r - 0.03f), 0.03f);
	}

	@Test
	void configuredLightmapSurvivesAccumulationFallbackAndFaceShading() {
		var lightmap = OverworldLightmap.forBrightness(0.5f);
		var a = LightSample.overworld(3, 0, 0.7f, lightmap);
		var b = LightSample.overworld(15, 0, 0.7f, lightmap);
		var blend = LightSample.ambient(0).add(a).add(b).scale(0.5f);
		assertSame(lightmap, blend.overworldLightmap());
		assertEquals(lightmap.sample(LightStyle.Day, 9, 0).r * 0.7f, blend.toColour(LightStyle.Day).r, 0.000001f);
		var fallback = Chunk.applySmoothLightFallback(LightSample.overworld(0, 0, 0.7f, lightmap), b,
				LightFace.EastWest, LightStyle.Day);
		assertSame(lightmap, fallback.overworldLightmap());
		assertEquals(lightmap.sample(LightStyle.Day, 15, 0).r * 0.7f, fallback.toColour(LightStyle.Day).r, 0.000001f);
	}

	@Test
	void invalidBrightnessIsRejected() {
		for (float brightness : new float[]{-0.1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY}) {
			assertThrows(IllegalArgumentException.class, () -> OverworldLightmap.forBrightness(brightness));
			assertThrows(IllegalArgumentException.class, () -> new tectonicus.configuration.RenderOptions().setLightmapBrightness(brightness));
		}
	}

	@ParameterizedTest
	@CsvSource({"Day,0", "Day,1", "Night,0", "Night,1"})
	void chunkSamplingUsesTheLightmapForCellsAndBoundaries(LightStyle style, float brightness) {
		var lightmap = OverworldLightmap.forBrightness(brightness);
		var chunk = new RawChunk();
		chunk.setSkyLight(0, 0, 0, (byte) 7);
		chunk.setBlockLight(0, 0, 0, (byte) 4);
		var expected = lightmap.sample(style, 7, 4);
		var boundary = lightmap.sample(style, 15, 0);
		for (LightFace face : LightFace.values()) {
			float shade = Chunk.getFaceFactor(style, face);
			var sample = Chunk.getLightSample(style, face, chunk, 0, 0, 0, lightmap, null);
			assertEquals(LightSample.overworld(7, 4, shade, lightmap), sample);
			var colour = sample.toColour(style);
			assertEquals(expected.r * shade, colour.r, 0.000001f);
			assertEquals(expected.g * shade, colour.g, 0.000001f);
			assertEquals(expected.b * shade, colour.b, 0.000001f);
			assertEquals(NetherLightmap.grayscale(expected) * shade,
					Chunk.getLight(style, face, chunk, 0, 0, 0, lightmap, null), 0.000001f);
			assertEquals(LightSample.overworld(15, 0, shade, lightmap),
					Chunk.getLightSample(style, face, null, 0, 0, 0, lightmap, null));
			assertEquals(NetherLightmap.grayscale(boundary) * shade,
					Chunk.getLight(style, face, chunk, 0, -1, 0, lightmap, null), 0.000001f);
		}
	}

	@ParameterizedTest
	@CsvSource({"0,14,41,71", "1,35,100,179"})
	void dayAndNightShareBrightnessAndBlockLight(float brightness, int unlit, int nightRed, int nightBlue) {
		var lightmap = OverworldLightmap.forBrightness(brightness);
		var sky = lightmap.sample(LightStyle.Night, 15, 0);
		assertEquals(nightRed / 255.0f, sky.r);
		assertEquals(sky.r, sky.g);
		assertEquals(nightBlue / 255.0f, sky.b);
		assertEquals(unlit / 255.0f, lightmap.sample(LightStyle.Night, 0, 0).r);
		for (int block = 0; block < 16; block++) {
			var day = lightmap.sample(LightStyle.Day, 0, block);
			var night = lightmap.sample(LightStyle.Night, 0, block);
			assertEquals(day.r, night.r);
			assertEquals(day.g, night.g);
			assertEquals(day.b, night.b);
		}
	}

	@Test
	void smoothLightingConvertsBlendedCoordinatesRatherThanAveragingBrightness() {
		var blended = LightSample.overworld(3, 0, 1).add(LightSample.overworld(15, 0, 1)).scale(0.5f);
		assertTrue(blended.overworldLightmap() != null);
		assertEquals(OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 9, 0).r, blended.toColour(LightStyle.Day).r, 0.000001f);
		assertTrue(blended.toColour(LightStyle.Day).r <
				(OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 3, 0).r + OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 15, 0).r) / 2);
		assertEquals((OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 9, 0).r + OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 10, 0).r) / 2,
				OverworldLightmap.forBrightness(0).sample(LightStyle.Day, 9.5f, 0).r, 0.000001f);
	}

}
