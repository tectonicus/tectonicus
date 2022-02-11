/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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

class BiomesOldTests {
	@Test
	void enumLookupValidId() {
		assertThat(BiomesOld.byId(6), is(equalTo(BiomesOld.SWAMP)));
	}

	@Test
	void enumLookupInvalidId() {
		assertThat(BiomesOld.byId(99999), is(equalTo(BiomesOld.OCEAN)));
	}

	@Test
	void getNameReturnsLowercaseName() {
		assertThat(BiomesOld.LUKEWARM_OCEAN.getName(), is(equalTo("lukewarm_ocean")));
	}
}
