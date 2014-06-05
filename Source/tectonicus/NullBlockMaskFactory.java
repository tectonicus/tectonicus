/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.raw.RawChunk;

public class NullBlockMaskFactory implements BlockMaskFactory
{
	private BlockMask fullMask;
	
	public NullBlockMaskFactory()
	{
		fullMask = new BlockMask();
		fullMask.setAllVisible();
	}
	
	@Override
	public BlockMask createMask(ChunkCoord coord, RawChunk rawChunk)
	{
		return fullMask;
	}
	
}
