/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class BlockPropertiesTest {

	private BlockProperties properties;

	@BeforeEach
	public void setUp()
	{
		properties = new BlockProperties();
		properties.put("facing", "west");
		properties.put("half", "lower");
		properties.put("hinge", "left");
		properties.put("open", "false");
		properties.put("powered", "false");
	}

	@Test
	void returnsCorrectToString() {
		String propsString = properties.toString();
		assertThat(propsString, is(equalTo("facing=west,half=lower,hinge=left,open=false,powered=false")));
	}

	@Test
	void propertiesContainsVariantString() {
		boolean containsString = properties.containsAll("facing=west,half=lower,hinge=left,open=false");

		assertThat(containsString, is(true));
	}

	@Test
	void insertInMapUnorderedReturnsTrue() {
		BlockProperties unordered = new BlockProperties();
		unordered.put("half", "lower");
		unordered.put("open", "false");
		unordered.put("facing", "west");
		unordered.put("powered", "false");
		unordered.put("hinge", "left");

		boolean containsString = unordered.containsAll("facing=west,half=lower,hinge=left,open=false");

		assertThat(containsString, is(true));
	}
}
