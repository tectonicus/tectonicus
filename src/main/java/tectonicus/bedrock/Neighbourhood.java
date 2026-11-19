/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import tectonicus.bedrock.BedrockBlockMapper.JavaState;

/**
 * A chunk's blocks and those of the eight chunks around it, as read from the save, for the states and light that
 * depend on neighbouring blocks. Positions are relative to the centre chunk, from -16 to 31 horizontally; a missing
 * neighbour reads as air.
 */
public final class Neighbourhood {
	private final Blocks[] chunks; // 3x3, index (dz + 1) * 3 + dx + 1

	private Neighbourhood(Blocks[] chunks) {
		this.chunks = chunks;
	}

	/** Gets the neighbouring chunk at an offset from -1 to 1, or null when it is not in the world. */
	@FunctionalInterface
	public interface Source {
		Blocks get(int dx, int dz);
	}

	public static Neighbourhood of(Source source) {
		Blocks[] chunks = new Blocks[9];
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++)
				chunks[(dz + 1) * 3 + dx + 1] = source.get(dx, dz);
		if (chunks[4] == null)
			throw new IllegalArgumentException("The centre chunk is required");
		return new Neighbourhood(chunks);
	}

	/** A neighbourhood with only the centre chunk. */
	public static Neighbourhood of(Blocks centre) {
		return of((dx, dz) -> dx == 0 && dz == 0 ? centre : null);
	}

	public Blocks centre() {
		return chunks[4];
	}

	/** The chunk at an offset from -1 to 1, or null. */
	public Blocks chunk(int dx, int dz) {
		return chunks[(dz + 1) * 3 + dx + 1];
	}

	/** The state at a position relative to the centre chunk, or null for air or a missing chunk. */
	public JavaState get(int x, int y, int z) {
		if (x < -16 || x >= 32 || z < -16 || z >= 32)
			return null;
		Blocks chunk = chunks[((z >> 4) + 1) * 3 + (x >> 4) + 1];
		return chunk == null ? null : chunk.get(x & 15, y, z & 15);
	}

	/** The block's name, with air for an empty or missing position. */
	public String name(int x, int y, int z) {
		JavaState state = get(x, y, z);
		return state == null ? "minecraft:air" : state.name();
	}
}
