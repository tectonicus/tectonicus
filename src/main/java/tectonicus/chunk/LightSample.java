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
public record LightSample(float sky, float block, float ambient) {
	public static LightSample ambient(float brightness) {
		return new LightSample(0, 0, brightness);
	}

	public LightSample add(LightSample other) {
		return new LightSample(sky + other.sky, block + other.block, ambient + other.ambient);
	}

	public LightSample scale(float factor) {
		return new LightSample(sky * factor, block * factor, ambient * factor);
	}

	public Colour4f toColour(LightStyle style) {
		if (style == LightStyle.Night) {
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
