/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class RawSign extends BlockEntity
{
	public RawSign(int blockId, int data, int x, int y, int z, int localX,
			int localY, int localZ, String text1, String text2, String text3,
			String text4)
	{
		super(blockId, data, x, y, z, localX, localY, localZ, text1, text2, text3,
				text4);
	}
}
