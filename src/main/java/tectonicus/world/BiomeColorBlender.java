/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import lombok.experimental.UtilityClass;
import tectonicus.Version;
import tectonicus.configuration.Map;
import tectonicus.util.Colour4f;

@UtilityClass
class BiomeColorBlender {
	static int resolveRadius(Map map, Version worldVersion, Version textureVersion) {
		if (map.isBiomeBlendRadiusConfigured())
			return map.getBiomeBlendRadius();

		// Worlds saved before 1.9 have no version name; use the texture version as a fallback.
		Version version = worldVersion == Version.VERSION_UNKNOWN ? textureVersion : worldVersion;
		return version.getNumVersion() <= Version.VERSION_12.getNumVersion() ? 1 : Map.DEFAULT_BIOME_BLEND_RADIUS;
	}

	static Colour4f blend(int x, int z, int radius, ColorResolver resolver) {
		if (radius == 0)
			return new Colour4f(resolver.getColor(x, z));

		int red = 0;
		int green = 0;
		int blue = 0;
		for (int sampleZ = z - radius; sampleZ <= z + radius; sampleZ++) {
			for (int sampleX = x - radius; sampleX <= x + radius; sampleX++) {
				int rgb = resolver.getColor(sampleX, sampleZ).toRgb();
				red += (rgb >> 16) & 255;
				green += (rgb >> 8) & 255;
				blue += rgb & 255;
			}
		}

		int diameter = radius * 2 + 1;
		int sampleCount = diameter * diameter;
		// Vanilla averages the integer channels, truncating before converting to floats.
		return new Colour4f(red / sampleCount, green / sampleCount, blue / sampleCount);
	}

	@FunctionalInterface
	interface ColorResolver {
		Colour4f getColor(int x, int z);
	}
}
