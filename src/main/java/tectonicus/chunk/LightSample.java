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

/** Separate sky and block contributions for smooth per-vertex lighting. */
public record LightSample(float sky, float block, float ambient, Colour4f netherColour) {
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
		return new LightSample(sky + other.sky, block + other.block, ambient + other.ambient, addColour(netherColour, other.netherColour));
	}

	public LightSample scale(float factor) {
		return new LightSample(sky * factor, block * factor, ambient * factor, netherColour == null ? null : new Colour4f(netherColour.r * factor, netherColour.g * factor, netherColour.b * factor, 1));
	}

	public Colour4f toColour(LightStyle style) {
		return toColour(style, false);
	}

	public Colour4f toColour(LightStyle style, boolean oldGrayscaleLighting) {
		if (netherColour != null) {
			if (oldGrayscaleLighting) {
				float light = NetherLightmap.grayscale(netherColour);
				return new Colour4f(light, light, light, 1);
			}
			return new Colour4f(netherColour.r, netherColour.g, netherColour.b, 1);
		}
		if (style == LightStyle.Night && !oldGrayscaleLighting) {
			// Minecraft's overworld night sky is blue, while block light remains warm.
			return new Colour4f(
					Util.clamp(ambient + sky * 0.48f + block, 0, 1),
					Util.clamp(ambient + sky * 0.48f + block * 0.847f, 0, 1),
					Util.clamp(ambient + sky + block * 0.612f, 0, 1), 1);
		}

		float brightness = Util.clamp(sky + block + ambient, 0, 1);
		return new Colour4f(brightness, brightness, brightness, 1);
	}
}
