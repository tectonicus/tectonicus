/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class BiomeWaterTests {
	@Test
	public void enumLookupValidId() {
		assertThat(BiomeWater.byId(6), is(equalTo(BiomeWater.SWAMP)));
	}

	@Test
	public void enumLookupInvalidId() {
		assertThat(BiomeWater.byId(99999), is(equalTo(BiomeWater.OCEAN)));
	}

	@Test
	public void getNameReturnsLowercaseName() {
		assertThat(BiomeWater.LUKEWARM_OCEAN.getName(), is(equalTo("lukewarm_ocean")));
	}
}
