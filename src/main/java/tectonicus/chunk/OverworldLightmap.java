/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.chunk;

import tectonicus.configuration.LightStyle;
import tectonicus.util.Colour4f;
import tectonicus.util.Util;

/** Overworld lightmap for fixed daylight and midnight conditions. */
public final class OverworldLightmap {
	/** Increment when changing the lightmap to invalidate cached tiles and views. */
	public static final int REVISION = 3;
	private static final OverworldLightmap MOODY = new OverworldLightmap(0);
	private final Colour4f[][] day;
	private final Colour4f[][] night;

	private OverworldLightmap(float brightness) {
		day = build(brightness, 1, 1);
		// Java 1.21.11's Overworld timeline supplies a factor of 0.24 and tint #7a7aff at midnight.
		night = build(brightness, 0.24f, 122 / 255.0f);
	}

	private static Colour4f[][] build(float brightness, float skyFactor, float skyRedGreen) {
		Colour4f[][] levels = new Colour4f[16][16];
		for (int sky = 0; sky < 16; sky++) {
			float s = brightness(sky) * skyFactor;
			for (int block = 0; block < 16; block++) {
				float b = brightness(block) * 1.5f;
				float red = baseChannel(s * skyRedGreen + b);
				float green = baseChannel(s * skyRedGreen + b * ((b * 0.6f + 0.4f) * 0.6f + 0.4f));
				float blue = baseChannel(s + b * (b * b * 0.6f + 0.4f));
				if (brightness > 0) {
					// Minecraft 1.21.11 boosts the largest component, scaling RGB together
					// to retain warm block-light colors instead of whitening each channel.
					float max = Math.max(red, Math.max(green, blue));
					float inverse = 1 - max;
					float factor = (1 - inverse * inverse * inverse * inverse) / max;
					red = red * (1 - brightness) + red * factor * brightness;
					green = green * (1 - brightness) + green * factor * brightness;
					blue = blue * (1 - brightness) + blue * factor * brightness;
				}
				levels[sky][block] = new Colour4f(channel(red), channel(green), channel(blue), 1);
			}
		}
		return levels;
	}

	public static OverworldLightmap forBrightness(float brightness) {
		if (!Float.isFinite(brightness) || brightness < 0 || brightness > 1)
			throw new IllegalArgumentException("lightmapBrightness must be between 0 and 1");
		return brightness == 0 ? MOODY : new OverworldLightmap(brightness);
	}

	private static float brightness(int level) {
		float inverse = 1 - level / 15.0f;
		return (1 - inverse) / (inverse * 3 + 1);
	}

	private static float baseChannel(float value) {
		return Math.min(1, value * 0.96f + 0.03f);
	}

	private static float channel(float value) {
		value = Util.clamp(value * 0.96f + 0.03f, 0, 1);
		// The game uploads an eight-bit lightmap texture.
		return (int) (value * 255) / 255.0f;
	}

	/** Bilinear lookup after interpolating sky/block coordinates, with brightness baked into the table. */
	public Colour4f sample(LightStyle style, float sky, float block) {
		Colour4f[][] levels = style == LightStyle.Night ? night : day;
		sky = Util.clamp(sky, 0, 15);
		block = Util.clamp(block, 0, 15);
		int s = (int) sky, b = (int) block;
		float sw = sky - s, bw = block - b;
		Colour4f a = levels[s][b], c = levels[Math.min(s + 1, 15)][b];
		Colour4f d = levels[s][Math.min(b + 1, 15)], e = levels[Math.min(s + 1, 15)][Math.min(b + 1, 15)];
		return new Colour4f(blend(a.r, c.r, d.r, e.r, sw, bw),
				blend(a.g, c.g, d.g, e.g, sw, bw), blend(a.b, c.b, d.b, e.b, sw, bw), 1);
	}

	private static float blend(float a, float b, float c, float d, float sky, float block) {
		return (a * (1 - sky) + b * sky) * (1 - block) + (c * (1 - sky) + d * sky) * block;
	}
}
