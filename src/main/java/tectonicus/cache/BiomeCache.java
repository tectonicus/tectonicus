/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import tectonicus.chunk.ChunkCoord;

public interface BiomeCache extends AutoCloseable
{
	public BiomeData loadBiomeData(ChunkCoord coord);

	default String getCacheKey() { return "stored-biomes"; }

	@Override
	default void close() {}
}
