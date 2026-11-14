/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.raw;

import tectonicus.blockTypes.Banner.Pattern;

import java.util.List;

public record BannerPatternsTag(List<Pattern> patterns) {
	public BannerPatternsTag {
		patterns = List.copyOf(patterns);
	}
}
