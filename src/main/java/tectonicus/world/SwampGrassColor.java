/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import lombok.experimental.UtilityClass;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.Biome;
import tectonicus.raw.Biomes;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.RawChunk;
import tectonicus.util.Colour4f;

import java.util.Random;

@UtilityClass
class SwampGrassColor {
	private static final double SKEW = (Math.sqrt(3.0) - 1.0) * 0.5;
	private static final double UNSKEW = (3.0 - Math.sqrt(3.0)) / 6.0;
	// The X/Y components of Minecraft's first twelve simplex gradients.
	private static final int[][] GRADIENTS = {
			{1, 1}, {-1, 1}, {1, -1}, {-1, -1},
			{1, 0}, {-1, 0}, {1, 0}, {-1, 0},
			{0, 1}, {0, -1}, {0, 1}, {0, -1}
	};
	private static final int[] PERMUTATIONS = createPermutations();

	static boolean applies(Biome biome) {
		return biome == Biomes.SWAMP || biome == Biomes.MANGROVE_SWAMP
				|| biome == BiomesOld.SWAMP || biome == BiomesOld.SWAMP_HILLS;
	}

	static Colour4f getColor(ChunkCoord chunk, int x, int z) {
		double worldX = chunk.x * RawChunk.WIDTH + x;
		double worldZ = chunk.z * RawChunk.DEPTH + z;
		return new Colour4f(noise(worldX * 0.0225, worldZ * 0.0225) < -0.1 ? 0xFF4C763C : 0xFF6A7039);
	}

	private static int[] createPermutations() {
		// Biome information noise has a fixed seed, independent of the world seed.
		Random random = new Random(2345L);
		// Vanilla consumes three offset doubles even though biome noise disables offsets.
		random.nextDouble();
		random.nextDouble();
		random.nextDouble();
		int[] permutations = new int[256];
		for (int i = 0; i < permutations.length; i++)
			permutations[i] = i;
		for (int i = 0; i < permutations.length; i++) {
			int other = i + random.nextInt(256 - i);
			int value = permutations[i];
			permutations[i] = permutations[other];
			permutations[other] = value;
		}
		return permutations;
	}

	private static int permute(int index) {
		return PERMUTATIONS[index & 255];
	}

	private static double noise(double x, double z) {
		double skew = (x + z) * SKEW;
		int cellX = (int)Math.floor(x + skew);
		int cellZ = (int)Math.floor(z + skew);
		double unskew = (cellX + cellZ) * UNSKEW;
		double localX = x - (cellX - unskew);
		double localZ = z - (cellZ - unskew);
		int stepX = localX > localZ ? 1 : 0;
		int stepZ = localX > localZ ? 0 : 1;
		int gradient0 = permute(cellX + permute(cellZ)) % 12;
		int gradient1 = permute(cellX + stepX + permute(cellZ + stepZ)) % 12;
		int gradient2 = permute(cellX + 1 + permute(cellZ + 1)) % 12;
		return 70.0 * (corner(gradient0, localX, localZ)
				+ corner(gradient1, localX - stepX + UNSKEW, localZ - stepZ + UNSKEW)
				+ corner(gradient2, localX - 1.0 + 2.0 * UNSKEW, localZ - 1.0 + 2.0 * UNSKEW));
	}

	private static double corner(int gradient, double x, double z) {
		double falloff = 0.5 - x * x - z * z;
		if (falloff < 0.0)
			return 0.0;
		falloff *= falloff;
		return falloff * falloff * (GRADIENTS[gradient][0] * x + GRADIENTS[gradient][1] * z);
	}
}
