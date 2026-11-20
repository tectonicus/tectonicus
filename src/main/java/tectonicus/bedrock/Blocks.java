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
import tectonicus.raw.RawChunk;

/**
 * A Bedrock chunk's blocks as Java states, one array per section indexed by {@link RawChunk#sectionIndex}. A null
 * section holds only air.
 */
public record Blocks(int chunkX, int chunkZ, int minSectionY, JavaState[][] sections) {
	/** The state at a position within the chunk, with y counted from the lowest section, or null for air. */
	public JavaState get(int x, int y, int z) {
		if (y < 0)
			return null;
		int section = y >> 4;
		if (section >= sections.length)
			return null;
		JavaState[] states = sections[section];
		return states == null ? null : states[RawChunk.sectionIndex(x, y & 15, z)];
	}

	/** The height of the highest block that is not air, plus one, or 0 for an empty chunk. */
	public int top() {
		for (int section = sections.length - 1; section >= 0; section--) {
			JavaState[] states = sections[section];
			if (states == null)
				continue;
			int top = -1;
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					for (int y = 15; y > top; y--)
						if (!states[RawChunk.sectionIndex(x, y, z)].isAir()) {
							top = y;
							break;
						}
			if (top >= 0)
				return section * 16 + top + 1;
		}
		return 0;
	}
}
