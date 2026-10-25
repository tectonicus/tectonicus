/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import tectonicus.Version;
import tectonicus.configuration.MutableMap;
import tectonicus.util.Colour4f;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BiomeColorBlenderTest {
	@ParameterizedTest
	@EnumSource(Version.class)
	void defaultRadiusMatchesTheWorldVersion(Version version) {
		int expected = switch (version) {
			case VERSION_ALPHA_BETA, VERSION_4, VERSION_5, VERSIONS_6_TO_8, VERSION_RV,
					VERSIONS_9_TO_11, VERSION_9, VERSION_10, VERSION_11, VERSION_12 -> 1;
			default -> 2;
		};
		assertEquals(expected, BiomeColorBlender.resolveRadius(new MutableMap("map"), version, Version.VERSION_26_3));
	}

	@Test
	void unversionedWorldsUseTheTextureVersion() {
		MutableMap map = new MutableMap("map");
		assertEquals(1, BiomeColorBlender.resolveRadius(map, Version.VERSION_UNKNOWN, Version.VERSION_4));
		assertEquals(1, BiomeColorBlender.resolveRadius(map, Version.VERSION_UNKNOWN, Version.VERSION_12));
		assertEquals(2, BiomeColorBlender.resolveRadius(map, Version.VERSION_UNKNOWN, Version.VERSION_13));
		assertEquals(2, BiomeColorBlender.resolveRadius(map, Version.VERSION_UNKNOWN, Version.VERSION_UNKNOWN));
	}

	@Test
	void knownWorldVersionTakesPrecedenceOverTextureVersion() {
		assertEquals(2, BiomeColorBlender.resolveRadius(new MutableMap("map"), Version.VERSION_13, Version.VERSION_12));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
	void explicitConfigOverridesBothLegacyAndModernDefaults(int radius) {
		MutableMap map = new MutableMap("map");
		map.setBiomeBlendRadius(radius);
		assertEquals(radius, BiomeColorBlender.resolveRadius(map, Version.VERSION_12, Version.VERSION_12));
		assertEquals(radius, BiomeColorBlender.resolveRadius(map, Version.VERSION_13, Version.VERSION_13));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
	void blendsAcrossAChunkEdgeAtEverySupportedRadius(int radius) {
		Colour4f west = new Colour4f(255, 0, 0);
		Colour4f east = new Colour4f(0, 0, 255);
		Set<String> sampled = new HashSet<>();
		Colour4f result = BiomeColorBlender.blend(15, 0, radius, (x, z) -> {
			assertTrue(x >= 15 - radius && x <= 15 + radius);
			assertTrue(z >= -radius && z <= radius);
			assertTrue(sampled.add(x + "," + z), "Each block should be sampled once");
			return x < 16 ? west : east;
		});

		int width = 2 * radius + 1;
		assertEquals(width * width, sampled.size());
		assertEquals(new Colour4f(255 * (radius + 1) / width, 0, 255 * radius / width).toRgb(), result.toRgb());
		assertEquals(1, result.a);
		assertEquals(0xFF0000, west.toRgb(), "Blending must not modify shared biome colors");
		assertEquals(0x0000FF, east.toRgb());
	}

	@Test
	void disabledBlendingReadsOnlyTheCenterAndReturnsACopy() {
		Colour4f center = new Colour4f(63, 118, 228);
		AtomicInteger calls = new AtomicInteger();
		Colour4f result = BiomeColorBlender.blend(-16, -1, 0, (x, z) -> {
			assertEquals(-16, x);
			assertEquals(-1, z);
			calls.incrementAndGet();
			return center;
		});
		assertEquals(1, calls.get());
		assertEquals(center.toRgb(), result.toRgb());
		assertNotSame(center, result);
	}

	@Test
	void averagesEachChannelWithMinecraftIntegerTruncation() {
		Colour4f center = new Colour4f(8, 8, 18);
		Colour4f neighbor = new Colour4f(0, 9, 9);
		Colour4f result = BiomeColorBlender.blend(0, 0, 1,
				(x, z) -> x == 0 && z == 0 ? center : neighbor);
		assertEquals(0x00080A, result.toRgb());
		assertEquals(0, result.r);
	}

	@Test
	void includesDiagonalCornersAtMaximumRadius() {
		Colour4f result = BiomeColorBlender.blend(-1, -16, 7,
				(x, z) -> Math.abs(x + 1) == 7 && Math.abs(z + 16) == 7
						? new Colour4f(255, 255, 255) : new Colour4f(0, 0, 0));
		assertEquals(0x040404, result.toRgb()); // Four white corners among 225 samples.
	}
}
