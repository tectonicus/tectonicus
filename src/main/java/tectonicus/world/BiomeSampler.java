/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import tectonicus.Version;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.Dimension;
import tectonicus.raw.Biome;
import tectonicus.raw.RawChunk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

/** Converts saved quart biomes to block biomes using Minecraft's seeded Voronoi lookup. */
class BiomeSampler {
	private final Long zoomSeed;
	private final boolean constantColumn;

	BiomeSampler(Long worldSeed, Version version, Dimension dimension) {
		zoomSeed = worldSeed != null ? obfuscateSeed(worldSeed) : null;
		// Vanilla's Overworld used FuzzyOffsetConstantColumnBiomeZoomer until 1.18.
		constantColumn = dimension == Dimension.OVERWORLD && version.getNumVersion() < Version.VERSION_18.getNumVersion();
	}

	Biome getBiome(RawChunk chunk, int x, int y, int z, Function<ChunkCoord, RawChunk> chunks) {
		// 1.14 and older columns, and 26.4+ final block biomes, are already resolved.
		if (zoomSeed == null || !chunk.usesQuartBiomes(y))
			return chunk.getBiome(x, y, z);

		ChunkCoord origin = chunk.getChunkCoord();
		int worldX = (int)(origin.x * RawChunk.WIDTH + x);
		int worldZ = (int)(origin.z * RawChunk.DEPTH + z);
		QuartPosition selected = selectQuart(zoomSeed, worldX, constantColumn ? 0 : chunk.getWorldY(y), worldZ);
		ChunkCoord target = new ChunkCoord(selected.x >> 2, selected.z >> 2);
		RawChunk source = target.equals(origin) ? chunk : chunks.apply(target);
		// At an unexplored edge, retain the available biome rather than inventing neighboring data.
		if (source == null)
			return chunk.getBiome(x, y, z);
		int localY = selected.y * 4 - source.getWorldY(0);
		return source.getNoiseBiome((selected.x & 3) * 4, localY, (selected.z & 3) * 4);
	}

	static long obfuscateSeed(long seed) {
		try {
			byte[] bytes = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(seed).array();
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
			return ByteBuffer.wrap(digest).order(ByteOrder.LITTLE_ENDIAN).getLong();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required for biome sampling", e);
		}
	}

	static QuartPosition selectQuart(long seed, int x, int y, int z) {
		int shiftedX = x - 2;
		int shiftedY = y - 2;
		int shiftedZ = z - 2;
		int quartX = shiftedX >> 2;
		int quartY = shiftedY >> 2;
		int quartZ = shiftedZ >> 2;
		double fractionX = (shiftedX & 3) / 4.0;
		double fractionY = (shiftedY & 3) / 4.0;
		double fractionZ = (shiftedZ & 3) / 4.0;
		double bestDistance = Double.POSITIVE_INFINITY;
		int bestCorner = 0;
		for (int corner = 0; corner < 8; corner++) {
			int dx = (corner >> 2) & 1;
			int dy = (corner >> 1) & 1;
			int dz = corner & 1;
			double distance = fiddledDistance(seed, quartX + dx, quartY + dy, quartZ + dz,
					fractionX - dx, fractionY - dy, fractionZ - dz);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestCorner = corner;
			}
		}
		return new QuartPosition(quartX + ((bestCorner >> 2) & 1),
				quartY + ((bestCorner >> 1) & 1), quartZ + (bestCorner & 1));
	}

	private static double fiddledDistance(long seed, int x, int y, int z, double dx, double dy, double dz) {
		long state = next(seed, x);
		state = next(state, y);
		state = next(state, z);
		state = next(state, x);
		state = next(state, y);
		state = next(state, z);
		double jitterX = fiddle(state);
		state = next(state, seed);
		double jitterY = fiddle(state);
		state = next(state, seed);
		double jitterZ = fiddle(state);
		// Preserve vanilla's Z, Y, X summation order and strict tie-breaking.
		return square(dz + jitterZ) + square(dy + jitterY) + square(dx + jitterX);
	}

	private static long next(long state, long salt) {
		return state * (state * 6364136223846793005L + 1442695040888963407L) + salt;
	}

	private static double fiddle(long state) {
		return (((state >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
	}

	private static double square(double value) {
		return value * value;
	}

	record QuartPosition(int x, int y, int z) {}
}
