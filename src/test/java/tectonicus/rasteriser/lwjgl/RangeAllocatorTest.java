/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser.lwjgl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RangeAllocatorTest {
	@Test
	void allocatesFirstFitUntilFull() {
		RangeAllocator ranges = new RangeAllocator(100);
		assertEquals(0, ranges.allocate(40));
		assertEquals(40, ranges.allocate(40));
		assertEquals(-1, ranges.allocate(30));
		assertEquals(80, ranges.allocate(20));
		assertEquals(100, ranges.usedBytes());
		assertEquals(-1, ranges.allocate(1));
	}

	@Test
	void freedRangesAreReused() {
		RangeAllocator ranges = new RangeAllocator(100);
		long a = ranges.allocate(30);
		ranges.allocate(30);
		ranges.free(a, 30);
		assertEquals(a, ranges.allocate(20));
	}

	@Test
	void neighbouringFreeRangesMerge() {
		RangeAllocator ranges = new RangeAllocator(90);
		long a = ranges.allocate(30);
		long b = ranges.allocate(30);
		long c = ranges.allocate(30);
		ranges.free(a, 30);
		ranges.free(c, 30);
		assertEquals(2, ranges.freeRangeCount());
		ranges.free(b, 30);
		assertEquals(1, ranges.freeRangeCount());
		assertEquals(0, ranges.usedBytes());
		assertEquals(0, ranges.allocate(90));
	}

	@Test
	void doubleFreeIsRejected() {
		RangeAllocator ranges = new RangeAllocator(100);
		long a = ranges.allocate(10);
		ranges.allocate(10);
		ranges.free(a, 10);
		assertThrows(IllegalStateException.class, () -> ranges.free(a, 10));
	}

	@Test
	void randomChurnReturnsToOneFreeRange() {
		RangeAllocator ranges = new RangeAllocator(1 << 20);
		Random random = new Random(3);
		List<long[]> live = new ArrayList<>();
		for (int i = 0; i < 20_000; i++) {
			if (!live.isEmpty() && (random.nextBoolean() || live.size() > 500)) {
				long[] range = live.remove(random.nextInt(live.size()));
				ranges.free(range[0], range[1]);
			} else {
				long bytes = 256L * (1 + random.nextInt(32));
				long offset = ranges.allocate(bytes);
				if (offset >= 0)
					live.add(new long[] {offset, bytes});
			}
		}
		for (long[] range : live)
			ranges.free(range[0], range[1]);
		assertEquals(0, ranges.usedBytes());
		assertEquals(1, ranges.freeRangeCount());
	}

	@Test
	void alignmentRoundsUp() {
		assertEquals(0, VertexBufferPool.align(0));
		assertEquals(VertexBufferPool.ALIGNMENT, VertexBufferPool.align(1));
		assertEquals(VertexBufferPool.ALIGNMENT, VertexBufferPool.align(VertexBufferPool.ALIGNMENT));
		assertEquals(2L * VertexBufferPool.ALIGNMENT, VertexBufferPool.align(VertexBufferPool.ALIGNMENT + 1));
	}
}
