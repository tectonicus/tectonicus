/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawChunkTest {
	@Test
	void convertsInternalYToWorldYUsingMinimumSectionOffset() {
		assertEquals(0, RawChunk.toWorldY(64, -4));
		assertEquals(80, RawChunk.toWorldY(144, -4));
		assertEquals(5, RawChunk.toWorldY(5, 0));
	}
}
