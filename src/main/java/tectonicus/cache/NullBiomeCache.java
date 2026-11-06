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

public class NullBiomeCache implements BiomeCache
{
	public BiomeData loadBiomeData(ChunkCoord coord)
	{
		return null;
	}
}
