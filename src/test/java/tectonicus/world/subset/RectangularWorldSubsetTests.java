/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.BlockFilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

class RectangularWorldSubsetTests {

	private RectangularWorldSubset subset;
	
	@BeforeEach
	void setUp() {
		// Create a 200x200 block rectangle centered at (0, 0)
		subset = new RectangularWorldSubset(new Vector3l(0, 0, 0), 200, 200);
	}

	@Test
	void testConstructorCalculatesCorrectBounds() {
		// 200x200 centered at (0, 0) should extend ±100 on each side
		assertThat(subset.getMinX(), is(equalTo(-100L)));
		assertThat(subset.getMaxX(), is(equalTo(100L)));
		assertThat(subset.getMinZ(), is(equalTo(-100L)));
		assertThat(subset.getMaxZ(), is(equalTo(100L)));
	}

	@Test
	void testConstructorCalculatesBufferedBounds() {
		// Buffer should be RawChunk.WIDTH + RawChunk.DEPTH (32 blocks)
		final long buffer = RawChunk.WIDTH + RawChunk.DEPTH;
		
		assertThat(subset.getBufferedMinX(), is(equalTo(-100L - buffer)));
		assertThat(subset.getBufferedMaxX(), is(equalTo(100L + buffer)));
		assertThat(subset.getBufferedMinZ(), is(equalTo(-100L - buffer)));
		assertThat(subset.getBufferedMaxZ(), is(equalTo(100L + buffer)));
	}

	@Test
	void testAsymmetricRectangle() {
		// Create 400x300 rectangle centered at (100, 50)
		RectangularWorldSubset asymmetric = new RectangularWorldSubset(
			new Vector3l(100, 0, 50), 400, 300
		);
		
		assertThat(asymmetric.getMinX(), is(equalTo(-100L)));
		assertThat(asymmetric.getMaxX(), is(equalTo(300L)));
		assertThat(asymmetric.getMinZ(), is(equalTo(-100L)));
		assertThat(asymmetric.getMaxZ(), is(equalTo(200L)));
	}

	@Test
	void testContainsChunkInBounds() {
		// Chunk at (2, 2) should be inside (2*16=32 blocks from center)
		ChunkCoord insideChunk = new ChunkCoord(2, 2);
		assertThat(subset.contains(insideChunk), is(true));
	}

	@Test
	void testContainsChunkNearEdge() {
		// Chunk at (6, 0) should be near edge but still inside (6*16=96 blocks)
		ChunkCoord edgeChunk = new ChunkCoord(6, 0);
		assertThat(subset.contains(edgeChunk), is(true));
	}

	@Test
	void testContainsChunkOutsideBounds() {
		// Chunk at (10, 0) should be outside (10*16=160 blocks > 100+buffer)
		ChunkCoord outsideChunk = new ChunkCoord(10, 0);
		assertThat(subset.contains(outsideChunk), is(false));
	}

	@Test
	void testContainsChunkFarOutside() {
		// Chunk at (20, 20) should be far outside
		ChunkCoord farOutsideChunk = new ChunkCoord(20, 20);
		assertThat(subset.contains(farOutsideChunk), is(false));
	}

	@Test
	void testContainsBlockInBounds() {
		// Block at (50, 50) should be inside
		assertThat(subset.containsBlock(50, 50), is(true));
	}

	@Test
	void testContainsBlockAtExactBoundary() {
		// Block at exact boundary (100, 100) should be included
		assertThat(subset.containsBlock(100, 100), is(true));
		assertThat(subset.containsBlock(-100, -100), is(true));
	}

	@Test
	void testContainsBlockJustOutsideBoundary() {
		// Block just outside boundary (101, 0) should be excluded
		assertThat(subset.containsBlock(101, 0), is(false));
		assertThat(subset.containsBlock(-101, 0), is(false));
	}

	@Test
	void testContainsBlockOutsideBounds() {
		// Block at (200, 200) should be outside
		assertThat(subset.containsBlock(200, 200), is(false));
	}

	@Test
	void testGetBlockFilterForCenterChunk() {
		// Get filter for chunk at (0, 0)
		ChunkCoord chunk = new ChunkCoord(0, 0);
		BlockFilter filter = subset.getBlockFilter(chunk);
		
		// Filter should not be null
		assertThat(filter, is(notNullValue()));
	}

	@Test
	void testNegativeOrigin() {
		// Test with negative origin coordinates
		RectangularWorldSubset negative = new RectangularWorldSubset(
			new Vector3l(-500, 0, -500), 200, 200
		);
		
		assertThat(negative.getMinX(), is(equalTo(-600L)));
		assertThat(negative.getMaxX(), is(equalTo(-400L)));
		assertThat(negative.getMinZ(), is(equalTo(-600L)));
		assertThat(negative.getMaxZ(), is(equalTo(-400L)));
	}

	@Test
	void testContainsBlockWithNegativeOrigin() {
		RectangularWorldSubset negative = new RectangularWorldSubset(
			new Vector3l(-500, 0, -500), 200, 200
		);
		
		// Block at (-500, -500) is the center
		assertThat(negative.containsBlock(-500, -500), is(true));
		
		// Block at (-400, -500) is inside
		assertThat(negative.containsBlock(-400, -500), is(true));
		
		// Block at (-600, -500) is inside
		assertThat(negative.containsBlock(-600, -500), is(true));
		
		// Block at (-300, -500) is outside
		assertThat(negative.containsBlock(-300, -500), is(false));
	}

	@Test
	void testCreateRegionIteratorNotNull() {
		// Region iterator should not be null
		assertThat(subset.createRegionIterator(null, null) != null, is(true));
	}

	@Test
	void testBufferZoneProperlyCalculated() {
		// Verify buffer zone is consistent
		final long buffer = RawChunk.WIDTH + RawChunk.DEPTH;
		final long expectedBufferMinX = -100L - buffer;
		final long expectedBufferMaxX = 100L + buffer;
		
		assertThat(subset.getBufferedMinX(), is(equalTo(expectedBufferMinX)));
		assertThat(subset.getBufferedMaxX(), is(equalTo(expectedBufferMaxX)));
	}
}
