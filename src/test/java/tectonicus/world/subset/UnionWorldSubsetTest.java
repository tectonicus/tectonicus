/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

import org.junit.jupiter.api.Test;
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
}
