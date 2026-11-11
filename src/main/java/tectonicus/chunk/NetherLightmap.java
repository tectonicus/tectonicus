package tectonicus.chunk;

import tectonicus.util.Colour4f;
import tectonicus.util.Util;

public final class NetherLightmap {
	public static final float DEFAULT_BRIGHTNESS = 0.5f;
	static final NetherLightmap DEFAULT = new NetherLightmap(DEFAULT_BRIGHTNESS);
	private final Colour4f[] levels = new Colour4f[16];
	private final float brightness;

	private NetherLightmap(float brightness) {
		this.brightness = brightness;
		for (int i = 0; i < levels.length; i++) {
			float level = i / 15.0f;
			float light = 0.1f + 0.9f * level / (4 - 3 * level);
			levels[i] = new Colour4f(channel(light),
					channel(light * ((light * 0.6f + 0.4f) * 0.6f + 0.4f)),
					channel(light * (light * light * 0.6f + 0.4f)), 1);
		}
	}

	public static NetherLightmap forBrightness(float brightness) {
		if (!Float.isFinite(brightness) || brightness < 0 || brightness > 1)
			throw new IllegalArgumentException("lightmapBrightness must be between 0 and 1");
		return brightness == DEFAULT_BRIGHTNESS ? DEFAULT : new NetherLightmap(brightness);
	}

	public float brightness() {
		return brightness;
	}

	private float channel(float value) {
		value = value * 0.96f + 0.75f * 0.04f;
		float inverse = 1 - value;
		float inverseFourth = inverse * inverse * inverse * inverse;
		// Preserve the original operation order at the default brightness.
		if (brightness == DEFAULT_BRIGHTNESS)
			value = (value + 1 - inverseFourth) * 0.5f;
		else
			value = value * (1 - brightness) + (1 - inverseFourth) * brightness;
		return Util.clamp(value * 0.96f + 0.75f * 0.04f, 0, 1);
	}

	public static Colour4f colour(int level) {
		return DEFAULT.sample(level);
	}

	public Colour4f sample(int level) {
		Colour4f c = levels[Math.clamp(level, 0, 15)];
		return new Colour4f(c.r, c.g, c.b, 1);
	}
	public static float grayscale(Colour4f c) {
		return c.r * 0.2126f + c.g * 0.7152f + c.b * 0.0722f;
	}
}
