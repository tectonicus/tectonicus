/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import tectonicus.ChunkCoord;

public class NullBiomeCache implements BiomeCache
{
	private BiomeData dummyData;
	
	public NullBiomeCache()
	{
		dummyData = new BiomeData();
	}
	
	public BiomeData loadBiomeData(ChunkCoord coord)
	{
		return dummyData;
	}
}
