/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.cache.NullBiomeCache;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.world.WorldInfo;
import tectonicus.world.filter.NullBlockFilter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegionTest {
	@Test
	void corruptChunkWithoutEntityRegionIsLoggedNotThrown(@TempDir Path world) throws Exception {
		// Pre-1.17 layout: region/r.0.0.mca with no entities/ folder next to it.
		File regionDir = world.resolve("region").toFile();
		regionDir.mkdirs();
		File regionFile = new File(regionDir, "r.0.0.mca");

		ByteBuffer bytes = ByteBuffer.allocate(3 * 4096);
		bytes.putInt(0, (2 << 8) | 1); // chunk 0,0 at sector 2, one sector long
		bytes.putInt(2 * 4096, 5);      // payload length including the compression byte
		bytes.put(2 * 4096 + 4, (byte) 2); // zlib
		bytes.put(2 * 4096 + 5, (byte) 0x12); // not a valid zlib stream
		Files.write(regionFile.toPath(), bytes.array());

		Region region = new Region(regionFile);
		Chunk chunk = region.loadChunk(new ChunkCoord(0, 0), new NullBiomeCache(), new NullBlockFilter(), null,
				new WorldInfo(Version.VERSION_12, false));

		assertNotNull(chunk);
		assertNull(chunk.getRawChunk());
	}
}
