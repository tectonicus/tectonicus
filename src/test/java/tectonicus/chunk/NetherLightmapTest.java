package tectonicus.chunk;

import org.junit.jupiter.api.Test;
import tectonicus.configuration.LightStyle;
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
}
