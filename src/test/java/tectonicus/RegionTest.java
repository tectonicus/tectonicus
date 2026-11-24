/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.NBTOutputStream;
import org.tectonicus.jnbt.StringTag;
import tectonicus.cache.NullBiomeCache;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.world.WorldInfo;
import tectonicus.world.filter.NullBlockFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTest {
	private int previousHeight;

	// Chunks size their section arrays from the global chunk height, which is unset in tests.
	@BeforeEach
	void setChunkHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(256);
	}

	@AfterEach
	void restoreChunkHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

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

	@Test
	void chunksAreReadFromTheirOwnSectorsOfTheRegion(@TempDir Path world) throws Exception {
		File regionDir = world.resolve("region").toFile();
		regionDir.mkdirs();
		File regionFile = new File(regionDir, "r.0.0.mca");

		// Two chunks in consecutive sectors, with junk filling every byte that is not part of a chunk, so a chunk read
		// from the wrong offset or with the wrong length fails to decompress.
		byte[] chunk = gzippedChunk();
		ByteBuffer bytes = ByteBuffer.allocate(4 * 4096);
		Arrays.fill(bytes.array(), 2 * 4096, 4 * 4096, (byte) 0x5A);
		bytes.putInt(0, (2 << 8) | 1); // chunk 0,0 at sector 2
		bytes.putInt(4, (3 << 8) | 1); // chunk 1,0 at sector 3
		for (int sector = 2; sector <= 3; sector++) {
			bytes.putInt(sector * 4096, chunk.length + 1);
			bytes.put(sector * 4096 + 4, (byte) 1); // gzip
			bytes.put(sector * 4096 + 5, chunk);
		}
		Files.write(regionFile.toPath(), bytes.array());

		Region region = new Region(regionFile);
		for (ChunkCoord coord : List.of(new ChunkCoord(0, 0), new ChunkCoord(1, 0))) {
			Chunk loaded = region.loadChunk(coord, new NullBiomeCache(), new NullBlockFilter(), null,
					new WorldInfo(Version.VERSION_21, false));

			assertNotNull(loaded.getRawChunk(), "chunk " + coord);
			assertTrue(loaded.getRawChunk().isFullChunk(), "chunk " + coord);
		}
	}

	private static byte[] gzippedChunk() throws Exception {
		CompoundTag root = new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", Version.VERSION_21.getDataVersion()),
				"Status", new StringTag("Status", "full"),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of()),
				"sections", new ListTag("sections", CompoundTag.class, List.of())));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (NBTOutputStream nbt = new NBTOutputStream(out)) {
			nbt.writeTag(root);
		}
		byte[] chunk = out.toByteArray();
		assertTrue(chunk.length < 4096 - 5);
		return chunk;
	}
}
