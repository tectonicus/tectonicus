/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;
import tectonicus.blockTypes.Banner.Pattern;

import java.util.Collections;
import java.util.List;

public class BannerEntity extends BlockEntity {
	@Getter
	private final Integer baseColor;
	private final List<Pattern> patterns;
	
	public BannerEntity(int x, int y, int z, int localX, int localY, int localZ, Integer baseColor, List<Pattern> patterns) {
		super(x, y, z, localX, localY, localZ);
		this.baseColor = baseColor;
		this.patterns = patterns;
	}
	
	public List<Pattern> getPatterns() {
		return Collections.unmodifiableList(patterns);
	}
}
