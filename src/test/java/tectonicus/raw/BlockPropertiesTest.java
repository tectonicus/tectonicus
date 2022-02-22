/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class BlockPropertiesTest {

	private BlockProperties properties;

	@BeforeEach
	public void setUp()
	{
		Map<String, String> props = new HashMap<>();
		props.put("facing", "west");
		props.put("half", "lower");
		props.put("hinge", "left");
		props.put("open", "false");
		props.put("powered", "false");

		properties = new BlockProperties(props);
	}

	@Test
	void returnsCorrectToString() {
		String propsString = properties.toString();
		assertThat(propsString, is(equalTo("facing=west,half=lower,hinge=left,open=false,powered=false")));
	}

	@Test
	void propertiesContainsVariantString() {
		boolean containsString = properties.contains("facing=west,half=lower,hinge=left,open=false");

		assertThat(containsString, is(true));
	}

	@Test
	void insertInMapUnorderedReturnsTrue() {
		Map<String, String> unordered = new HashMap<>();
		unordered.put("half", "lower");
		unordered.put("open", "false");
		unordered.put("facing", "west");
		unordered.put("powered", "false");
		unordered.put("hinge", "left");

		BlockProperties properties = new BlockProperties(unordered);

		boolean containsString = properties.contains("facing=west,half=lower,hinge=left,open=false");

		assertThat(containsString, is(true));
	}
}
