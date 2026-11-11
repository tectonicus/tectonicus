package tectonicus.chunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.Minecraft;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.raw.RawChunk;
import static org.junit.jupiter.api.Assertions.*;

class NetherLightmapTest {
	@Test
	void lightLevelsAreMonotonicAndBecomeWhite() {
		var dark = NetherLightmap.colour(0);
		assertTrue(dark.r > 0);
		assertTrue(dark.r > dark.g && dark.g > dark.b);
		for (int level = 1; level <= 15; level++) {
			var previous = NetherLightmap.colour(level - 1);
			var current = NetherLightmap.colour(level);
			assertTrue(current.r > previous.r && current.g > previous.g && current.b > previous.b);
			assertTrue(current.r <= 1 && current.g <= 1 && current.b <= 1);
		}
		var bright = NetherLightmap.colour(15);
		assertEquals(bright.r, bright.g);
		assertEquals(bright.r, bright.b);
		assertEquals(0.9852f, bright.r, 0.00001f);
	}
	@Test
	void smoothSamplesBlendLightmapColoursAndSupportGrayscale() {
		var a = new LightSample(0, 0, 0, NetherLightmap.colour(0));
		var b = new LightSample(0, 1, 0, NetherLightmap.colour(15));
		var result = a.scale(0.5f).add(b.scale(0.5f)).toColour(LightStyle.Night);
		assertEquals((a.netherColour().r + b.netherColour().r) / 2, result.r, 0.000001f);
		var gray = a.toColour(LightStyle.Night, true);
		assertEquals(gray.r, gray.g);
		assertEquals(gray.r, gray.b);
	}

	@ParameterizedTest
	@CsvSource({"0,0.299834,0.235756,0.165106", "0.5,0.516692,0.429982,0.315886", "1,0.733551,0.624208,0.466666"})
	void configuredBrightnessReachesScalarAndSmoothSamples(float brightness, float red, float green, float blue) {
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		try {
			var chunk = new RawChunk();
			chunk.setSkyLight(0, 0, 0, (byte) 15);
			chunk.setBlockLight(0, 0, 0, (byte) 7);
			var overworld = OverworldLightmap.forBrightness(0);
			var nether = NetherLightmap.forBrightness(brightness);
			var sample = Chunk.getLightSample(LightStyle.Night, LightFace.Top, chunk, 0, 0, 0, overworld, nether);
			assertEquals(0, sample.sky());
			var colour = sample.toColour(LightStyle.Night);
			assertEquals(red, colour.r, 0.000001f);
			assertEquals(green, colour.g, 0.000001f);
			assertEquals(blue, colour.b, 0.000001f);
			assertEquals(red * 0.2126f + green * 0.7152f + blue * 0.0722f,
					Chunk.getLight(LightStyle.Night, LightFace.Top, chunk, 0, 0, 0, overworld, nether), 0.000001f);
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}
}
