/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.chunk;

import tectonicus.configuration.LightStyle;
import tectonicus.util.Colour4f;
import tectonicus.util.Util;

/**
 * Separate sky and block contributions for smooth per-vertex lighting.
 * Overworld lightmap samples carry raw 0..15 coordinates in sky/block,
 * and directional shade in ambient. Interpolation precedes lightmap conversion.
 * Other samples retain Tectonicus's scaled light and ambient contributions.
 */
public record LightSample(float sky, float block, float ambient, Colour4f netherColour, OverworldLightmap overworldLightmap) {
	public LightSample(float sky, float block, float ambient, Colour4f netherColour) {
		this(sky, block, ambient, netherColour, null);
	}

	/** Raw lightmap coordinates and independent directional shade. */
	public static LightSample overworld(float sky, float block, float shade) {
		return overworld(sky, block, shade, OverworldLightmap.forBrightness(0));
	}

	public static LightSample overworld(float sky, float block, float shade, OverworldLightmap lightmap) {
		return new LightSample(sky, block, shade, null, java.util.Objects.requireNonNull(lightmap));
	}

	public LightSample(float sky, float block, float ambient) {
		this(sky, block, ambient, null);
	}
	private static Colour4f addColour(Colour4f a, Colour4f b) {
		if (a == null) return b;
		if (b == null) return a;
		return new Colour4f(a.r + b.r, a.g + b.g, a.b + b.b, 1);
	}

	public static LightSample ambient(float brightness) {
		return new LightSample(0, 0, brightness);
	}

	public LightSample add(LightSample other) {
		return new LightSample(sky + other.sky, block + other.block, ambient + other.ambient,
				addColour(netherColour, other.netherColour), overworldLightmap != null ? overworldLightmap : other.overworldLightmap);
	}

	public LightSample scale(float factor) {
		return new LightSample(sky * factor, block * factor, ambient * factor,
				netherColour == null ? null : new Colour4f(netherColour.r * factor, netherColour.g * factor, netherColour.b * factor, 1),
				overworldLightmap);
	}

	public Colour4f toColour(LightStyle style) {
		return toColour(style, false);
	}

	public Colour4f toColour(LightStyle style, boolean oldGrayscaleLighting) {
		if (overworldLightmap != null) {
			Colour4f colour = overworldLightmap.sample(style, sky, block);
			if (oldGrayscaleLighting) {
				float light = NetherLightmap.grayscale(colour) * ambient;
				return new Colour4f(light, light, light, 1);
			}
			return new Colour4f(colour.r * ambient, colour.g * ambient, colour.b * ambient, 1);
		}
		if (netherColour != null) {
			if (oldGrayscaleLighting) {
				float light = NetherLightmap.grayscale(netherColour);
				return new Colour4f(light, light, light, 1);
			}
			return new Colour4f(netherColour.r, netherColour.g, netherColour.b, 1);
		}
		float brightness = Util.clamp(sky + block + ambient, 0, 1);
		return new Colour4f(brightness, brightness, brightness, 1);
	}
}
