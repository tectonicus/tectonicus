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
import tectonicus.Region;
import tectonicus.RegionCache;
import tectonicus.RegionCoord;
import tectonicus.Version;
import tectonicus.world.WorldInfo;
import tectonicus.world.filter.NullBlockFilter;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

	@Test
	void unreadableChunkIsReadOnlyOnce(@TempDir Path world) throws IOException {
		File regionDir = world.resolve("region").toFile();
		regionDir.mkdirs();
		ByteBuffer bytes = ByteBuffer.allocate(3 * 4096);
		bytes.putInt(0, (2 << 8) | 1);     // chunk 0,0 at sector 2, one sector long
		bytes.putInt(2 * 4096, 5);         // payload length including the compression byte
		bytes.put(2 * 4096 + 4, (byte) 9); // unknown compression type: loading throws
		Files.write(new File(regionDir, "r.0.0.mca").toPath(), bytes.array());

		AtomicInteger regionLookups = new AtomicInteger();
		RegionCache regionCache = new RegionCache(world.toFile()) {
			@Override
			public Region getRegion(RegionCoord coord) {
				regionLookups.incrementAndGet();
				return super.getRegion(coord);
			}
		};
		ChunkLocator locator = new ChunkLocator(coord -> new BiomeData(), regionCache, world.toFile());
		WorldInfo worldInfo = new WorldInfo(Version.VERSION_21, false);

		ChunkCoord coord = new ChunkCoord(0, 0);
		assertNull(locator.loadChunk(coord, new NullBlockFilter(), null, worldInfo));
		assertEquals(1, regionLookups.get());
		assertNull(locator.loadChunk(coord, new NullBlockFilter(), null, worldInfo));
		assertEquals(1, regionLookups.get(), "a chunk that failed to load is not read again");
	}

	private static void createChunkFile(Path baseDir, String xDir, String zDir, String filename) throws IOException {
		File dir = baseDir.resolve(xDir).resolve(zDir).toFile();
		dir.mkdirs();
		Files.write(new File(dir, filename).toPath(), new byte[] {1});
	}
}
