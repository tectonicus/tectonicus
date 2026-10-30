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

	@Test
	void renderBoundsIgnoreAirAndLightOnlySections() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockName(0, 240, 0, "minecraft:cave_air");
		chunk.setBlockName(1, 240, 0, "minecraft:void_air");
		chunk.setBlockData(0, 128, 0, (byte) 0);
		assertNull(chunk.getRenderHeightBounds());
		chunk.setBlockName(0, 64, 0, "minecraft:stone");
		assertArrayEquals(new int[] {32, 112}, chunk.getRenderHeightBounds());
	}

	@Test
	void legacyIdsAndUpperSectionsContributeToRenderBounds() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(0, 15, 0, 1);
		assertArrayEquals(new int[] {0, 48}, chunk.getRenderHeightBounds());
		chunk.setBlockId(0, 240, 0, 1);
		assertArrayEquals(new int[] {0, 256}, chunk.getRenderHeightBounds());
	}

	@Test
	void entitiesKeepFullHeightBoundsEvenWithoutBlocks() {
		RawChunk chunk = new RawChunk();
		chunk.getPaintings().add(null);
		assertArrayEquals(new int[] {0, 256}, chunk.getRenderHeightBounds());
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

	@Test
	void missingBeaconLevelsStillLoadsEffectsAndUsesResolvedLevelsForMarkers() throws Exception {
		CompoundTag beacon = new CompoundTag("", Map.of(
				"id", new StringTag("id", "minecraft:beacon"),
				"x", new IntTag("x", 8), "y", new IntTag("y", 36), "z", new IntTag("z", 8),
				"primary_effect", new StringTag("primary_effect", "minecraft:speed")));
		CompoundTag root = new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", 3953),
				"Status", new StringTag("Status", "minecraft:full"),
				"yPos", new IntTag("yPos", -4),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of(beacon)),
				"sections", new ListTag("sections", CompoundTag.class, List.of())));
		RawChunk chunk = readChunk(root, Version.VERSION_21);
		BeaconEntity entity = chunk.getBeacons().values().iterator().next();
		assertArrayEquals(new int[] {0, 256}, chunk.getRenderHeightBounds());
		assertEquals(0, entity.getLevels());
		assertEquals(tectonicus.world.Effect.SPEED, entity.getPrimaryEffect());
		var markers = new java.util.ArrayDeque<BeaconEntity>();
		var filter = new tectonicus.configuration.filter.BeaconFilter(tectonicus.configuration.filter.BeaconFilterType.ACTIVATED);
		tectonicus.util.FindEntityUtil.findBeacons(chunk, markers, filter, ignored -> 4);
		assertEquals(1, markers.size());
		assertEquals(4, markers.peek().getLevels());
		assertEquals(entity.getY(), markers.peek().getY());
		assertEquals(entity.getPrimaryEffect(), markers.peek().getPrimaryEffect());
		markers.clear();
		tectonicus.util.FindEntityUtil.findBeacons(chunk, markers, filter, ignored -> 0);
		assertTrue(markers.isEmpty());
	}

	@ParameterizedTest
	@CsvSource({"EnderChest,true", "minecraft:ender_chest,true", "minecraft:chest,false"})
	void containerMarkersIncludeEnderChestsAndRespectContainerFilter(String id, boolean enderChest) throws Exception {
		CompoundTag chest = new CompoundTag("", Map.of(
				"id", new StringTag("id", id),
				"x", new IntTag("x", -17), "y", new IntTag("y", 36), "z", new IntTag("z", -1)));
		boolean legacy = id.equals("EnderChest");
		Map<String, Tag> chunkTags = new HashMap<>();
		chunkTags.put("Status", new StringTag("Status", "full"));
		chunkTags.put("yPos", new IntTag("yPos", -4));
		String entitiesKey = legacy ? "TileEntities" : "block_entities";
		String sectionsKey = legacy ? "Sections" : "sections";
		chunkTags.put(entitiesKey, new ListTag(entitiesKey, CompoundTag.class, List.of(chest)));
		chunkTags.put(sectionsKey, new ListTag(sectionsKey, CompoundTag.class, List.of()));
		CompoundTag root = legacy
				? new CompoundTag("", Map.of("Level", new CompoundTag("Level", chunkTags), "DataVersion", new IntTag("DataVersion", 1343)))
				: new CompoundTag("", chunkTags);
		RawChunk chunk = readChunk(root, legacy ? Version.VERSION_12 : Version.VERSION_21);
		assertEquals(1, chunk.getContainers().size());
		ContainerEntity entity = chunk.getContainers().getFirst();
		assertEquals(enderChest, entity.isEnderChest());
		assertEquals(enderChest ? "Ender Chest" : "Chest", entity.getCustomName());
		assertEquals(-17, entity.getX());
		assertEquals(36, entity.getY());
		assertEquals(-1, entity.getZ());
		assertTrue(entity.getItems().isEmpty());

		var markers = new java.util.concurrent.ConcurrentLinkedQueue<ContainerEntity>();
		tectonicus.util.FindEntityUtil.findContainers(chunk, new tectonicus.configuration.filter.ContainerFilter(), markers);
		assertTrue(markers.isEmpty());
		tectonicus.util.FindEntityUtil.findContainers(chunk,
				new tectonicus.configuration.filter.ContainerFilter(tectonicus.configuration.filter.ContainerFilterType.PLAYER), markers);
		assertEquals(List.of(entity), List.copyOf(markers));
	}

	@Test
	void loadsAllModernContainerKindsAndShulkerColorsFromActualBlocks() throws Exception {
		var blocks = new java.util.ArrayList<String>();
		for (ContainerKind kind : ContainerKind.values()) blocks.add(kind.getId());
		for (tectonicus.world.Colors color : tectonicus.world.Colors.values()) blocks.add(ContainerKind.colorName(color) + "_shulker_box");
		for (String block : blocks) {
			String id = block.endsWith("shulker_box") ? "minecraft:shulker_box" : "minecraft:" + block;
			CompoundTag entity = new CompoundTag("", Map.of("id", new StringTag("id", id),
					"x", new IntTag("x", 8), "y", new IntTag("y", 4), "z", new IntTag("z", 8)));
			CompoundTag palette = new CompoundTag("", Map.of("Name", new StringTag("Name", "minecraft:" + block)));
			CompoundTag section = new CompoundTag("", Map.of("Y", new ByteTag("Y", (byte) 0),
					"block_states", new CompoundTag("block_states", Map.of("palette", new ListTag("palette", CompoundTag.class, List.of(palette))))));
			CompoundTag root = new CompoundTag("", Map.of("Status", new StringTag("Status", "full"), "yPos", new IntTag("yPos", 0),
					"block_entities", new ListTag("block_entities", CompoundTag.class, List.of(entity)),
					"sections", new ListTag("sections", CompoundTag.class, List.of(section))));
			ContainerEntity loaded = readChunk(root, Version.VERSION_21).getContainers().getFirst();
			assertEquals(block.endsWith("shulker_box") ? ContainerKind.SHULKER_BOX : ContainerKind.valueOf(block.toUpperCase(java.util.Locale.ROOT)), loaded.getContainerKind());
			assertEquals(ContainerKind.shulkerColor("minecraft:" + block, 0), loaded.getShulkerColor());
		}
	}

	@Test
	void legacyBlockIdsDistinguishTrappedChestsAndEveryShulkerColor() throws Exception {
		for (int blockId : java.util.stream.IntStream.concat(java.util.stream.IntStream.of(54, 130, 146), java.util.stream.IntStream.rangeClosed(219, 234)).toArray()) {
			byte[] blocks = new byte[4096];
			java.util.Arrays.fill(blocks, (byte) blockId);
			byte[] data = new byte[2048];
			java.util.Arrays.fill(data, (byte) 0x55);
			String id = blockId == 130 ? "EnderChest" : blockId >= 219 ? "minecraft:shulker_box" : "Chest";
			CompoundTag entity = new CompoundTag("", Map.of("id", new StringTag("id", id),
					"x", new IntTag("x", 8), "y", new IntTag("y", 4), "z", new IntTag("z", 8)));
			CompoundTag section = new CompoundTag("", Map.of("Y", new ByteTag("Y", (byte) 0),
					"Blocks", new org.jnbt.ByteArrayTag("Blocks", blocks), "Data", new org.jnbt.ByteArrayTag("Data", data)));
			CompoundTag level = new CompoundTag("Level", Map.of("TileEntities", new ListTag("TileEntities", CompoundTag.class, List.of(entity)),
					"Sections", new ListTag("Sections", CompoundTag.class, List.of(section))));
			ContainerEntity loaded = readChunk(new CompoundTag("", Map.of("Level", level)), Version.VERSION_12).getContainers().getFirst();
			assertEquals(ContainerKind.fromBlock(id, null, blockId), loaded.getContainerKind());
			assertEquals(ContainerKind.shulkerColor(null, blockId), loaded.getShulkerColor());
			if (loaded.getContainerKind().isChest()) assertEquals("east", loaded.getFacing());
		}
		assertNull(ContainerKind.fromBlock("minecraft:chest", "minecraft:copper_chest", 0));
	}

	private RawChunk readChunk(CompoundTag root, Version version) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) {
			out.writeTag(root);
		}
		return new RawChunk(new ChunkData(bytes.toByteArray(), Compression.Gzip), null, new WorldInfo(version, false));
	}
}
