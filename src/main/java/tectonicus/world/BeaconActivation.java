/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */


package tectonicus.world;

import tectonicus.Minecraft;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.BeaconEntity;
import tectonicus.raw.RawChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Levels property is unreliable so we reconstruct beacon state. */
public final class BeaconActivation {
	private static final Set<String> BASE_BLOCKS = Set.of("minecraft:iron_block", "minecraft:gold_block",
			"minecraft:diamond_block", "minecraft:emerald_block", "minecraft:netherite_block");

	private BeaconActivation() {}

	@FunctionalInterface
	public interface BeamObstruction {
		boolean blocksBeam(RawChunk chunk, int x, int y, int z);
	}

	public static int resolve(RawChunk origin, BeaconEntity beacon, Function<ChunkCoord, RawChunk> chunks,
			BeamObstruction obstruction) {
		int x = beacon.getLocalX(), y = beacon.getLocalY(), z = beacon.getLocalZ();
		// A pyramid reaches at most four blocks into a neighboring chunk. Retain these
		// references for the scan, including missing chunks, rather than querying per block.
		Map<ChunkCoord, RawChunk> neighbors = new HashMap<>();
		int levels = 0;
		layers: for (int depth = 1; depth <= 4 && y - depth >= 0; depth++) {
			for (int dx = -depth; dx <= depth; dx++) {
				for (int dz = -depth; dz <= depth; dz++) {
					int bx = x + dx, bz = z + dz;
					RawChunk chunk = origin;
					if (bx < 0 || bx >= RawChunk.WIDTH || bz < 0 || bz >= RawChunk.DEPTH) {
						ChunkCoord coord = new ChunkCoord(origin.getChunkCoord().x + Math.floorDiv(bx, RawChunk.WIDTH),
								origin.getChunkCoord().z + Math.floorDiv(bz, RawChunk.DEPTH));
						if (!neighbors.containsKey(coord))
							neighbors.put(coord, chunks.apply(coord));
						chunk = neighbors.get(coord);
					}
					if (chunk == null || !isBase(chunk, Math.floorMod(bx, RawChunk.WIDTH), y - depth,
							Math.floorMod(bz, RawChunk.DEPTH)))
						break layers;
				}
			}
			levels = depth;
		}
		if (levels == 0)
			return 0;
		for (int by = y + 1; by < Minecraft.getChunkHeight(); by++) {
			String name = origin.getBlockName(x, by, z);
			int id = origin.getBlockId(x, by, z);
			// Bedrock allows beacons through the Nether ceiling; tinted glass does not.
			if ("minecraft:bedrock".equals(name) || name == null && id == 7)
				continue;
			if ("minecraft:tinted_glass".equals(name) || obstruction.blocksBeam(origin, x, by, z))
				return 0;
		}
		return levels;
	}

	private static boolean isBase(RawChunk chunk, int x, int y, int z) {
		String name = chunk.getBlockName(x, y, z);
		if (name != null)
			return BASE_BLOCKS.contains(name);
		return switch (chunk.getBlockId(x, y, z)) {
			case 41, 42, 57, 133 -> true;
			default -> false;
		};
	}
}
