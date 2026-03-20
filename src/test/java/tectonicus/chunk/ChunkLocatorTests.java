/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.chunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.RegionCache;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class ChunkLocatorTests {
	@Test
	void findsAlphaChunkCoordsFromChunkDirectories(@TempDir Path tempPath) throws IOException {
		createChunkFile(tempPath, "0", "0", "c.0.0.dat");
		createChunkFile(tempPath, "0", "0", "c.-1.z.dat");
		createChunkFile(tempPath, "a", "b", "ignore.dat");

		BiomeCache biomeCache = coord -> new BiomeData();
		ChunkLocator locator = new ChunkLocator(biomeCache, new RegionCache(tempPath.toFile()), tempPath.toFile());

		ChunkCoord[] coords = locator.getAlphaChunkCoords();
		Set<String> actual = new HashSet<>();
		for (ChunkCoord coord : coords) {
			actual.add(coord.x + "," + coord.z);
		}

		assertThat(actual.contains("0,0"), is(true));
		assertThat(actual.contains("-1,35"), is(true));
		assertThat(actual.size(), is(2));
	}

	private static void createChunkFile(Path baseDir, String xDir, String zDir, String filename) throws IOException {
		File dir = baseDir.resolve(xDir).resolve(zDir).toFile();
		dir.mkdirs();
		Files.write(new File(dir, filename).toPath(), new byte[] {1});
	}
}
