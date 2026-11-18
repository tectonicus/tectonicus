/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaintingNewTest {
	@ParameterizedTest
	@CsvSource({
			"Kebab, minecraft:kebab",
			"Aztec2, minecraft:aztec2",
			"SkullAndRoses, minecraft:skull_and_roses",
			"DonkeyKong, minecraft:donkey_kong",
			"BurningSkull, minecraft:burning_skull",
			"minecraft:kebab, minecraft:kebab",
			"minecraft:Kebab, minecraft:kebab",
			"mymod:portrait, mymod:portrait"
	})
	void normalisesSavedMotives(String saved, String expected) {
		assertEquals(expected, PaintingNew.normaliseMotive(saved));
	}
}
