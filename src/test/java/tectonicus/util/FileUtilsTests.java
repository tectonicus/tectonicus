/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class FileUtilsTests {

	@Test
	void testValidJsonReturnsTrue() {
		boolean isValid = FileUtils.isJSONValid("{\"foo\":\"bar\"}");

		assertThat(isValid, is(equalTo(true)));
	}

	@Test
	void testInvalidJsonReturnsFalse() {
		boolean isValid = FileUtils.isJSONValid("This is not valid json");

		assertThat(isValid, is(equalTo(false)));
	}
}
