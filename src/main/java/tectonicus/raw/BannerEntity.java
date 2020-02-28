/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.Collections;
import java.util.List;

import tectonicus.blockTypes.Banner.Pattern;

public class BannerEntity extends BlockEntity
{
	private final int baseColor;
	private final List<Pattern> patterns;
	
	public BannerEntity(int x, int y, int z, int localX, int localY, int localZ, int baseColor, List<Pattern> patterns)
	{
		super(x, y, z, localX, localY, localZ);
		this.baseColor = baseColor;
		this.patterns = patterns;
	}
	
	public int getBaseColor() { return baseColor; }
	public List<Pattern> getPatterns() { return Collections.unmodifiableList(patterns); }
}
