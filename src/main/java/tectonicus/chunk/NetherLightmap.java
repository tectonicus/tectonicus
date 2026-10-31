package tectonicus.chunk;

import tectonicus.util.Colour4f;
import tectonicus.util.Util;

public final class NetherLightmap {
	private static final Colour4f[] LEVELS = new Colour4f[16];
	static {
		for (int i = 0; i < LEVELS.length; i++) {
			float level = i / 15.0f;
			float light = 0.1f + 0.9f * level / (4 - 3 * level);
			LEVELS[i] = new Colour4f(channel(light),
					channel(light * ((light * 0.6f + 0.4f) * 0.6f + 0.4f)),
					channel(light * (light * light * 0.6f + 0.4f)), 1);
		}
	}
	private NetherLightmap() {}
	private static float channel(float value) {
		value = value * 0.96f + 0.75f * 0.04f;
		float inverse = 1 - value;
		value = (value + 1 - inverse * inverse * inverse * inverse) * 0.5f;
		return Util.clamp(value * 0.96f + 0.75f * 0.04f, 0, 1);
	}
	public static Colour4f colour(int level) {
		Colour4f c = LEVELS[Math.clamp(level, 0, 15)];
		return new Colour4f(c.r, c.g, c.b, 1);
	}
	public static float grayscale(Colour4f c) {
		return c.r * 0.2126f + c.g * 0.7152f + c.b * 0.0722f;
	}
}
