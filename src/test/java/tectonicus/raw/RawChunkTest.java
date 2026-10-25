/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongArrayTag;
import org.jnbt.NBTInputStream.Compression;
import org.jnbt.NBTOutputStream;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.Version;
import tectonicus.Minecraft;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkData;
import tectonicus.world.WorldInfo;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RawChunkTest {
	private int previousHeight;

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
	void convertsInternalYToWorldYUsingMinimumSectionOffset() {
		assertEquals(0, RawChunk.toWorldY(64, -4));
		assertEquals(80, RawChunk.toWorldY(144, -4));
		assertEquals(5, RawChunk.toWorldY(5, 0));
	}

	@ParameterizedTest
	@CsvSource({"postprocessed,true", "fullchunk,true", "full,true", "minecraft:full,true",
			"empty,false", "carved,false", "liquid_carved,false", "decorated,false", "lighted,false", "not_full,false"})
	void legacyChunkStatusesLoadCompletedTerrainAndSkipGenerationStages(String status, boolean complete) throws Exception {
		RawChunk chunk = readLegacyChunk(status);
		assertEquals(complete, chunk.isFullChunk());
		if (complete) {
			assertEquals(new ChunkCoord(7, -3), chunk.getChunkCoord());
			assertEquals("minecraft:stone", chunk.getBlockName(0, 0, 0));
			assertEquals("minecraft:stone", chunk.getBlockName(15, 15, 15));
		} else {
			assertNull(chunk.getBlockName(0, 0, 0));
		}
	}

	@Test
	void chunksWithoutAStatusStillLoad() throws Exception {
		RawChunk chunk = readLegacyChunk(null);
		assertTrue(chunk.isFullChunk());
		assertEquals("minecraft:stone", chunk.getBlockName(0, 0, 0));
	}

	@ParameterizedTest
	@ValueSource(strings = {"full", "minecraft:full"})
	void modernRootLevelStatusesStillLoad(String status) throws Exception {
		CompoundTag root = new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", 3953),
				"Status", new StringTag("Status", status),
				"xPos", new IntTag("xPos", 7), "zPos", new IntTag("zPos", -3),
				"yPos", new IntTag("yPos", 0),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of()),
				"sections", new ListTag("sections", CompoundTag.class, List.of())));
		RawChunk chunk = readChunk(root, Version.VERSION_21);
		assertTrue(chunk.isFullChunk());
		assertEquals(new ChunkCoord(7, -3), chunk.getChunkCoord());
	}

	private RawChunk readLegacyChunk(String status) throws Exception {
		CompoundTag paletteEntry = new CompoundTag("", Map.of("Name", new StringTag("Name", "minecraft:stone")));
		CompoundTag section = new CompoundTag("", Map.of(
				"Y", new ByteTag("Y", (byte) 0),
				"Palette", new ListTag("Palette", CompoundTag.class, List.of(paletteEntry)),
				"BlockStates", new LongArrayTag("BlockStates", new long[256])));
		Map<String, Tag> level = new HashMap<>();
		level.put("xPos", new IntTag("xPos", 7));
		level.put("zPos", new IntTag("zPos", -3));
		level.put("Sections", new ListTag("Sections", CompoundTag.class, List.of(section)));
		if (status != null)
			level.put("Status", new StringTag("Status", status));
		return readChunk(new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", 1631), "Level", new CompoundTag("Level", level))), Version.VERSION_13);
	}

	private RawChunk readChunk(CompoundTag root, Version version) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) {
			out.writeTag(root);
		}
		return new RawChunk(new ChunkData(bytes.toByteArray(), Compression.Gzip), null, new WorldInfo(version, false));
	}
}
