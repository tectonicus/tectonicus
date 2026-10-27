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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void containsAllMatchesAnUnorderedSubset() {
		Map<String, String> required = new LinkedHashMap<>();
		required.put("open", "false");
		required.put("hinge", "left");
		required.put("facing", "west");
		required.put("half", "lower");

		assertTrue(properties.containsAll(required));
	}

	@ParameterizedTest
	@ValueSource(strings = {"east", "wes"})
	void containsAllRejectsMismatchedValuesEvenWhenAnotherPropertyMatches(String facing) {
		assertFalse(properties.containsAll(Map.of("facing", facing, "half", "lower")));
	}

	@Test
	void containsAllRejectsMissingKeysEvenWhenAnotherPropertyMatches() {
		assertFalse(properties.containsAll(Map.of("missing", "west", "half", "lower")));
	}

	@Test
	void containsAllMatchesEmptyRequirementsForPopulatedAndEmptyProperties() {
		assertTrue(properties.containsAll(Map.of()));
		assertTrue(new BlockProperties().containsAll(Map.of()));
	}
}
