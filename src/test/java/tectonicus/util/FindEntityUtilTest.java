/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import org.junit.jupiter.api.Test;
import tectonicus.raw.BedEntity;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.world.Colors;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FindEntityUtilTest {
	@Test
	void findsTheHeadOfEachBedInScanOrder() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockName(0, 0, 0, "minecraft:stone");
		bed(chunk, 3, 70, 5, "minecraft:red_bed", "head");
		bed(chunk, 3, 70, 6, "minecraft:red_bed", "foot");
		bed(chunk, 9, 20, 1, "minecraft:blue_bed", "head"); // lower, so found first
		bed(chunk, 1, 20, 1, "minecraft:straw_bed", "head"); // not a bed
		chunk.setBlockName(2, 20, 2, "minecraft:white_bed"); // no state at all

		Queue<BedEntity> beds = new ArrayDeque<>();
		FindEntityUtil.findBeds(chunk, beds);

		assertEquals(2, beds.size());
		List<BedEntity> found = List.copyOf(beds);
		assertBed(found.get(0), 9, 20, 1, Colors.byName("blue"));
		assertBed(found.get(1), 3, 70, 5, Colors.byName("red"));
	}

	private static void bed(RawChunk chunk, int x, int y, int z, String name, String part) {
		chunk.setBlockName(x, y, z, name);
		chunk.setBlockState(x, y, z, new BlockProperties(Map.of("part", part, "facing", "north")));
	}

	private static void assertBed(BedEntity bed, int x, int y, int z, Colors color) {
		assertEquals(x, bed.getLocalX());
		assertEquals(y, bed.getLocalY());
		assertEquals(z, bed.getLocalZ());
		assertEquals(x, bed.getX()); // chunk 0,0
		assertEquals(z, bed.getZ());
		assertEquals(color.getId(), bed.getColor());
	}
}
