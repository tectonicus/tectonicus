package tectonicus.texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LegacyLavaTextureTest {
	@Test void usesOpaqueHistoricalLavaColors() {
		assertEquals(0xFF9B0000, LegacyLavaTexture.color(0));
		assertEquals(0xFFFFFF80, LegacyLavaTexture.color(1));
		assertEquals(LegacyLavaTexture.color(0), LegacyLavaTexture.color(-1));
		assertEquals(LegacyLavaTexture.color(1), LegacyLavaTexture.color(2));
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void producesRepeatableNonuniformFramesWithHistoricalAlpha(boolean flowing) {
		var first = LegacyLavaTexture.frame(flowing, 1);
		var second = LegacyLavaTexture.frame(flowing, 1);
		int size = flowing ? 32 : 16;
		assertEquals(size, first.getWidth());
		assertEquals(size, first.getHeight());
		java.util.Set<Integer> colors = new java.util.HashSet<>();
		for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
			int pixel = first.getRGB(x, y);
			assertEquals(pixel, second.getRGB(x, y));
			assertTrue(((pixel >> 16) & 255) >= 155);
			assertEquals(255, pixel >>> 24);
			if (flowing) assertEquals(pixel, first.getRGB(x & 15, y & 15));
			colors.add(pixel);
		}
		assertTrue(colors.size() > 1);
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void simulationStepMatchesActualMinecraft10Effect(boolean flowing) throws Exception {
		String jar = System.getenv("MINECRAFT_1_0_JAR");
		assumeTrue(jar != null && !jar.isBlank(), "Supply a local Minecraft 1.0 client");
		try (var loader = new URLClassLoader(new URL[]{Path.of(jar).toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
			Class<?> type = loader.loadClass(flowing ? "ss" : "yu");
			Object vanilla = type.getConstructor().newInstance();
			LegacyLavaTexture effect = new LegacyLavaTexture(flowing);
			// Random impulses created by this step affect subsequent frames only.
			// Supply matching nonuniform initial states to exercise the full stencil.
			float[] current = new float[256], velocity = new float[256];
			for (int i = 0; i < 256; i++) {
				current[i] = (i * 7 % 256) / 512f;
				velocity[i] = (255 - i) / 1024f;
			}
			set(vanilla, "g", current.clone()); set(vanilla, "i", velocity.clone());
			set(effect, "current", current); set(effect, "velocity", velocity);
			type.getMethod("a").invoke(vanilla);
			effect.step();
			byte[] rgba = (byte[])type.getField("a").get(vanilla);
			var image = effect.image();
			for (int i = 0; i < 256; i++) {
				int expected = (rgba[i*4+3] & 255) << 24 | (rgba[i*4] & 255) << 16
						| (rgba[i*4+1] & 255) << 8 | (rgba[i*4+2] & 255);
				assertEquals(expected, image.getRGB(i % 16, i / 16), "pixel " + i);
			}
		}
	}

	private static void set(Object target, String name, float[] values) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, values);
	}
}
