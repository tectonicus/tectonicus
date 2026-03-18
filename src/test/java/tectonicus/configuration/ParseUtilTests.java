/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ParseUtilTests {
	@Test
	void testParseDimension() {
		DimensionInfo dimensionInfo = ParseUtil.parseDimension("the_nether");
		
		assertThat(dimensionInfo.dimension(), is(Dimension.NETHER));
		assertThat(dimensionInfo.name(), is("the_nether"));
	}

	@Test
	void testParseBackgroundColorDefaultsByDimension() {
		assertThat(ParseUtil.parseBackgroundColor(null, Dimension.OVERWORLD), is("#e5e3df"));
		assertThat(ParseUtil.parseBackgroundColor("", Dimension.NETHER), is("#e5e3df"));
		assertThat(ParseUtil.parseBackgroundColor("", Dimension.END), is("#281932"));
		assertThat(ParseUtil.parseBackgroundColor(null, Dimension.OTHER), is("#e5e3df"));
	}

	@Test
	void testParseBackgroundColorReturnsProvidedColor() {
		assertThat(ParseUtil.parseBackgroundColor("#112233", Dimension.OVERWORLD), is("#112233"));
		assertThat(ParseUtil.parseBackgroundColor("#abcdef", Dimension.END), is("#abcdef"));
	}
}
