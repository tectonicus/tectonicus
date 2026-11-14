/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.BlockIds;
import tectonicus.Minecraft;
import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UnionWorldSubsetTest {
	@TempDir
	Path directory;
	private int previousHeight;

	@BeforeEach
	void setDefaultChunkHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(256);
	}

	@AfterEach
	void restoreChunkHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@Test
	void visitsSharedRegionOnceAndSkipsTheGap() throws Exception {
		var near = new CircularWorldSubset(new Vector3l(256, 64, 256), 12);
		var overlap = new RectangularWorldSubset(new Vector3l(261, 64, 256), 20, 20);
		var far = new RectangularWorldSubset(new Vector3l(-1_023_744, 64, 256), 16, 16);
		var union = new UnionWorldSubset(List.of(new UnionWorldSubset.Member("Near", near),
				new UnionWorldSubset.Member("Overlap", overlap), new UnionWorldSubset.Member("Far", far)));

		var regions = Files.createDirectory(directory.resolve("region"));
		for (String name : List.of("r.0.0.mca", "r.-1000.0.mca", "r.-2000.0.mca")) {
			Files.createFile(regions.resolve(name));
		}

		var iterator = union.createRegionIterator(SaveFormat.ANVIL, directory.toFile());
		Set<String> visited = new HashSet<>();
		while (iterator.hasNext()) {
			assertTrue(iterator.hasNext());
			assertTrue(visited.add(iterator.next().getName()));
		}
		assertEquals(Set.of("r.0.0.mca", "r.-2000.0.mca"), visited);
		assertTrue(union.contains(new ChunkCoord(16, 16)));
		assertTrue(union.contains(new ChunkCoord(-63_984, 16)));
		assertFalse(union.contains(new ChunkCoord(-32_000, 16)));
	}

	@Test
	void keepsColumnsAndMarkersInEitherShape() {
		var circle = new CircularWorldSubset(new Vector3l(3, 200, 8), 4);
		var rectangle = new RectangularWorldSubset(null, 6, 6);
		var union = new UnionWorldSubset(List.of(new UnionWorldSubset.Member("Circle", circle),
				new UnionWorldSubset.Member("Rectangle", rectangle)));
		assertTrue(union.needsOrigin());
		union.setOrigin(new Vector3l(10, 64, 8));
		assertFalse(union.needsOrigin());
		assertEquals(3, union.getOrigin().x);

		assertTrue(union.containsBlock(3, 8));
		assertTrue(union.containsBlock(10, 8));
		assertFalse(union.containsBlock(15, 8));

		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(256);
		try {
			var chunk = new RawChunk();
			for (int x = 0; x < 16; x++) {
				chunk.setBlockId(x, 0, 8, BlockIds.STONE);
			}
			union.getBlockFilter(new ChunkCoord(0, 0)).filter(chunk);
			assertEquals(BlockIds.STONE, chunk.getBlockId(3, 0, 8));
			assertEquals(BlockIds.STONE, chunk.getBlockId(7, 0, 8));
			assertEquals(BlockIds.STONE, chunk.getBlockId(10, 0, 8));
			assertEquals(BlockIds.AIR, chunk.getBlockId(15, 0, 8));
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}

	@Test
	void circleMembershipUsesTheRenderedColumnAtFractionalAndNegativePositions() {
		var circle = new CircularWorldSubset(new Vector3l(0, 64, 0), 2);
		var include = new UnionWorldSubset(List.of(new UnionWorldSubset.Member("Circle", circle)));
		var exclude = new UnionWorldSubset(List.of(), List.of(circle));
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(256);
		try {
			for (long chunkX : new long[] {-1, 0}) {
				var coord = new ChunkCoord(chunkX, 0);
				var includedChunk = new RawChunk();
				var excludedChunk = new RawChunk();
				for (int x = 0; x < RawChunk.WIDTH; x++) {
					includedChunk.setBlockId(x, 0, 1, BlockIds.STONE);
					excludedChunk.setBlockId(x, 0, 1, BlockIds.STONE);
				}
				include.getBlockFilter(coord).filter(includedChunk);
				exclude.getBlockFilter(coord).filter(excludedChunk);
				for (int x = 0; x < RawChunk.WIDTH; x++) {
					double markerX = chunkX * RawChunk.WIDTH + x + 0.75;
					boolean rendered = includedChunk.getBlockId(x, 0, 1) == BlockIds.STONE;
					assertEquals(rendered, circle.containsBlock(markerX, 1.75));
					assertEquals(rendered, include.containsBlock(markerX, 1.75));
					assertEquals(!rendered, exclude.containsBlock(markerX, 1.75));
					assertEquals(!rendered, excludedChunk.getBlockId(x, 0, 1) == BlockIds.STONE);
				}
			}
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}

	@Test
	void memberHeightBoundsUseOnlyItsUnexcludedTerrain() {
		var summit = new UnionWorldSubset.Member("Summit", new RectangularWorldSubset(new Vector3l(8, 64, 8), 4, 4));
		var lowland = new UnionWorldSubset.Member("Lowland", new RectangularWorldSubset(new Vector3l(1, 64, 1), 1, 1));
		var cutout = new CircularWorldSubset(new Vector3l(8, 64, 8), 1);
		var union = new UnionWorldSubset(List.of(summit, lowland), List.of(cutout));
		var coord = new ChunkCoord(0, 0);
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(384);
		try {
			var chunk = new RawChunk();
			chunk.setBlockName(10, 244, 10, "minecraft:stone");
			chunk.setBlockName(8, 360, 8, "minecraft:stone");
			chunk.setBlockId(1, 12, 1, BlockIds.STONE);
			chunk.setBlockName(6, 350, 6, "minecraft:cave_air");
			assertArrayEquals(new int[] {208, 288}, chunk.getRenderHeightBounds(union.getBlockFilter(summit, coord)::containsColumn));
			assertArrayEquals(new int[] {0, 48}, chunk.getRenderHeightBounds(union.getBlockFilter(lowland, coord)::containsColumn));
			assertEquals(BlockIds.STONE, chunk.getBlockId(1, 12, 1));
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}

	@Test
	void subtractsOnlyTheExcludedHeightAndProjectsEachIncludedMember() {
		var origin = new Vector3l(8, 0, 8);
		var lower = new UnionWorldSubset.Member("Lower", new CircularWorldSubset(origin, 4, new HeightRange(null, 80)));
		var upper = new UnionWorldSubset.Member("Upper", new RectangularWorldSubset(origin, 8, 8, new HeightRange(80, null)));
		var exclusion = new CircularWorldSubset(origin, 2, new HeightRange(80, 80));
		var subset = new UnionWorldSubset(List.of(lower, upper), List.of(exclusion), new HeightRange(79, 81));
		var coord = new ChunkCoord(0, 0);
		var chunk = new RawChunk();
		for (int y = 78; y <= 82; y++) {
			chunk.setBlockId(8, y, 8, BlockIds.STONE);
			chunk.setBlockId(12, y, 8, BlockIds.STONE);
		}
		assertArrayEquals(new int[] {47, 113}, chunk.getVisibleRenderHeightBounds(subset.visibility(coord, lower)));
		assertArrayEquals(new int[] {48, 114}, chunk.getVisibleRenderHeightBounds(subset.visibility(coord, upper)));
		subset.getRenderBlockFilter(coord).filter(chunk);
		assertEquals(BlockIds.AIR, chunk.getBlockId(8, 78, 8));
		assertEquals(BlockIds.STONE, chunk.getBlockId(8, 79, 8));
		assertEquals(BlockIds.AIR, chunk.getBlockId(8, 80, 8));
		assertEquals(BlockIds.STONE, chunk.getBlockId(12, 80, 8));
		assertEquals(BlockIds.STONE, chunk.getBlockId(8, 81, 8));
		assertEquals(BlockIds.AIR, chunk.getBlockId(8, 82, 8));
		var exclusionOnly = new UnionWorldSubset(List.of(), List.of(exclusion), new HeightRange(79, 81));
		assertTrue(exclusionOnly.containsBlock(100, 80, 100));
		assertFalse(exclusionOnly.containsBlock(8, 80, 8));
		assertFalse(exclusionOnly.containsBlock(100, 82, 100));
	}

	@Test
	void removesColumnsFromOverlappingIncludesOrTheFullDimension() {
		var base = new RectangularWorldSubset(new Vector3l(5, 64, 8), 10, 10);
		var overlap = new CircularWorldSubset(new Vector3l(7, 64, 8), 4);
		var members = List.of(new UnionWorldSubset.Member("Base", base), new UnionWorldSubset.Member("Overlap", overlap));
		var circle = new CircularWorldSubset(null, 2);
		var rectangle = new RectangularWorldSubset(new Vector3l(9, 64, 8), 2, 4);
		List<BoundedWorldSubset> exclusions = List.of(circle, rectangle);
		var mixed = new UnionWorldSubset(members, exclusions);
		var full = new UnionWorldSubset(List.of(), exclusions);
		assertTrue(mixed.needsOrigin());
		mixed.setOrigin(new Vector3l(5, 64, 8));
		assertFalse(mixed.needsOrigin());
		assertNotEquals(new UnionWorldSubset(members).toString(), mixed.toString());
		assertTrue(full.contains(new ChunkCoord(100, -100)));
		assertNull(full.getOrigin());

		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(256);
		try {
			for (var subset : List.of(mixed, full)) {
				var chunk = new RawChunk();
				for (int x = 0; x < 16; x++) {
					chunk.setBlockId(x, 0, 8, BlockIds.STONE);
					chunk.setBlockId(x, 200, 8, BlockIds.STONE);
				}
				subset.getBlockFilter(new ChunkCoord(0, 0)).filter(chunk);
				for (int x : List.of(5, 7, 9)) {
					assertFalse(subset.containsBlock(x + 0.25, 8.25));
					assertEquals(BlockIds.AIR, chunk.getBlockId(x, 0, 8));
					assertEquals(BlockIds.AIR, chunk.getBlockId(x, 200, 8));
				}
				assertEquals(BlockIds.STONE, chunk.getBlockId(2, 0, 8));
				assertEquals(subset == full ? BlockIds.STONE : BlockIds.AIR, chunk.getBlockId(15, 0, 8));
			}
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}
}
