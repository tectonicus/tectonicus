/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.RegionCoord;
import tectonicus.SaveFormat;
import tectonicus.cache.RegionHashStore;
import tectonicus.chunk.ChunkCoord;
import tectonicus.util.Vector3l;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CircularWorldSubsetRegionTests {
	@TempDir
	Path directory;

	@Test
	void preprocessesTheReportedMissingRegionAndStoresItsChunkHash() throws Exception {
		CircularWorldSubset subset = new CircularWorldSubset(new Vector3l(-1000313, 0, -1000278), 200);
		ChunkCoord chunk = new ChunkCoord(-62529, -62529);
		assertTrue(subset.contains(chunk));
		assertFalse(subset.containsBlock(chunk.x * 16 + 8, chunk.z * 16 + 8));
		Files.createDirectories(directory.resolve("region"));
		Files.createFile(directory.resolve("region/r.-1955.-1955.mca"));
		RegionHashStore hashes = new RegionHashStore(directory.resolve("cache").toFile());
		byte[] expected = {1, 2, 3};
		RegionIterator iterator = subset.createRegionIterator(SaveFormat.ANVIL, directory.toFile());
		while (iterator.hasNext()) {
			File file = iterator.next();
			if (file != null) {
				assertEquals("r.-1955.-1955.mca", file.getName());
				hashes.startRegion(new RegionCoord(-1955, -1955));
				hashes.addHash(chunk, expected);
				hashes.endRegion();
			}
		}
		assertTrue(Files.exists(directory.resolve("cache/hashStore/r--1955--1955.hashes")));
		assertArrayEquals(expected, hashes.getChunkHash(chunk));
	}

	@ParameterizedTest
	@CsvSource({"-1000313, -1000278, 200", "0, 0, 0", "511, 511, 200", "-511, -511, 512", "1023, -1023, 513"})
	void visitsEveryRegionContainingAnAcceptedChunk(long x, long z, long radius) throws Exception {
		CircularWorldSubset subset = new CircularWorldSubset(new Vector3l(x, 0, z), radius);
		Path regions = Files.createDirectories(directory.resolve("region"));
		Set<String> expected = new HashSet<>();
		long minX = Math.floorDiv(x - radius - 64, 16), maxX = Math.floorDiv(x + radius + 64, 16);
		long minZ = Math.floorDiv(z - radius - 64, 16), maxZ = Math.floorDiv(z + radius + 64, 16);
		for (long cx = minX; cx <= maxX; cx++) {
			for (long cz = minZ; cz <= maxZ; cz++) {
				ChunkCoord chunk = new ChunkCoord(cx, cz);
				if (subset.contains(chunk))
					expected.add(RegionCoord.getFilenameFromChunkCoord(chunk));
			}
		}
		for (String name : expected)
			Files.createFile(regions.resolve(name));
		Set<String> visited = new HashSet<>();
		RegionIterator iterator = subset.createRegionIterator(SaveFormat.ANVIL, directory.toFile());
		while (iterator.hasNext()) {
			File file = iterator.next();
			if (file != null)
				assertTrue(visited.add(file.getName()), "Duplicate region: " + file);
		}
		assertEquals(expected, visited);
	}
}
