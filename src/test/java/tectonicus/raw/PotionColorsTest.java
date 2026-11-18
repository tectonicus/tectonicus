/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PotionColorsTest {
	@Test
	void findsLongAndStrongVariantsByTheirBasePotion() {
		assertEquals(0x33EBFF, PotionColors.of("minecraft:swiftness"));
		assertEquals(0x33EBFF, PotionColors.of("minecraft:long_swiftness"));
		assertEquals(0xF82423, PotionColors.of("strong_healing"));
		assertEquals(PotionColors.WATER, PotionColors.of("minecraft:water"));
		assertNull(PotionColors.of("minecraft:unknown"));
		assertNull(PotionColors.of(null));
	}
}
