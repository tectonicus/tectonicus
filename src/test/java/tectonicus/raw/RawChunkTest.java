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
import tectonicus.world.subset.FullWorldSubset;
import tectonicus.world.subset.HeightRange;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	@Test
	void blockStatesAffectChunkHashByValueAndPosition() throws Exception {
		RawChunk chunk = new RawChunk();
		chunk.setBlockName(0, 0, 0, "minecraft:furnace");
		chunk.setBlockName(1, 0, 0, "minecraft:furnace");
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] withoutStates = chunk.calculateHash(digest);
		chunk.setBlockState(0, 0, 0, new BlockProperties(Map.of()));
		assertArrayEquals(withoutStates, chunk.calculateHash(digest));

		var properties = new LinkedHashMap<String, String>();
		properties.put("lit", "false");
		properties.put("facing", "north");
		var unlit = new BlockProperties();
		unlit.setProperties(properties);
		chunk.setBlockState(0, 0, 0, unlit);
		byte[] unlitHash = chunk.calculateHash(digest);
		assertFalse(MessageDigest.isEqual(withoutStates, unlitHash));

		var sameState = new BlockProperties(properties.reversed());
		chunk.setBlockState(0, 0, 0, sameState);
		assertArrayEquals(unlitHash, chunk.calculateHash(digest));

		chunk.setBlockState(0, 0, 0, new BlockProperties(Map.of("facing", "north", "lit", "true")));
		assertFalse(MessageDigest.isEqual(unlitHash, chunk.calculateHash(digest)));

		chunk.setBlockState(0, 0, 0, null);
		chunk.setBlockState(1, 0, 0, sameState);
		assertFalse(MessageDigest.isEqual(unlitHash, chunk.calculateHash(digest)));
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
	@CsvSource({"false, 0", "true, 64"})
	void slicesLegacyAnvilAtTheSameWorldYInAnUpgradedSave(boolean upgraded, int offset) throws Exception {
		Minecraft.setChunkHeight(upgraded ? 384 : 256);
		RawChunk chunk = readLegacyChunk("full", new WorldInfo(upgraded ? Version.VERSION_21 : Version.VERSION_13, upgraded));
		var subset = new FullWorldSubset(new HeightRange(2, 2));
		subset.getRenderBlockFilter(chunk.getChunkCoord()).filter(chunk);
		assertEquals(2, chunk.getWorldY(offset + 2));
		assertEquals("minecraft:stone", chunk.getBlockName(3, offset + 2, 7));
		assertEquals("minecraft:air", chunk.getBlockName(3, offset + 1, 7));
		assertEquals("minecraft:air", chunk.getBlockName(3, offset + 3, 7));
	}

	@Test
	void negativeSliceUsesExactOccupiedHeightsAndLeavesSourceIntactDuringDiscovery() throws Exception {
		Minecraft.setChunkHeight(384);
		CompoundTag root = new CompoundTag("", Map.of("yPos", new IntTag("yPos", -4),
				"sections", new ListTag("sections", CompoundTag.class, List.of()),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of())));
		RawChunk chunk = readChunk(root, Version.VERSION_21);
		chunk.setBlockName(3, 44, 9, "minecraft:stone");
		chunk.setBlockId(3, 45, 9, 1);
		chunk.setBlockName(3, 200, 9, "minecraft:stone");
		var slice = new FullWorldSubset(new HeightRange(-20, -19));
		assertArrayEquals(new int[] {12, 78}, chunk.getVisibleRenderHeightBounds(slice.visibility(chunk.getChunkCoord())));
		var empty = new FullWorldSubset(new HeightRange(null, -21));
		assertNull(chunk.getVisibleRenderHeightBounds(empty.visibility(chunk.getChunkCoord())));
		assertEquals("minecraft:stone", chunk.getBlockName(3, 200, 9));
		slice.getRenderBlockFilter(chunk.getChunkCoord()).filter(chunk);
		assertEquals("minecraft:stone", chunk.getBlockName(3, 44, 9));
		assertEquals(1, chunk.getBlockId(3, 45, 9));
		assertEquals("minecraft:air", chunk.getBlockName(3, 200, 9));
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

	@Test
	void datapackHeightsPlaceSectionsFromTheDimensionMinimum() throws Exception {
		Minecraft.setChunkHeight(512);
		RawChunk tall = readChunk(modernSections(-8, Map.of(-8, "minecraft:stone", 23, "minecraft:glass")),
				new WorldInfo(Version.VERSION_21, true, -128));
		assertEquals("minecraft:stone", tall.getBlockName(0, 0, 0));
		assertEquals("minecraft:glass", tall.getBlockName(0, 511, 0));
		assertEquals(-128, tall.getWorldY(0));
		assertEquals(383, tall.getWorldY(511));

		Minecraft.setChunkHeight(128);
		RawChunk raised = readChunk(modernSections(4, Map.of(4, "minecraft:stone", 11, "minecraft:glass")),
				new WorldInfo(Version.VERSION_21, true, 64));
		assertEquals("minecraft:stone", raised.getBlockName(0, 0, 0));
		assertEquals("minecraft:glass", raised.getBlockName(0, 127, 0));
		assertEquals(64, raised.getWorldY(0));
	}

	private static CompoundTag modernSections(int minSectionY, Map<Integer, String> blocks) {
		List<Tag> sections = blocks.entrySet().stream().<Tag>map(entry -> {
			CompoundTag palette = new CompoundTag("", Map.of("Name", new StringTag("Name", entry.getValue())));
			return new CompoundTag("", Map.of("Y", new ByteTag("Y", entry.getKey().byteValue()),
					"block_states", new CompoundTag("block_states", Map.of("palette", new ListTag("palette", CompoundTag.class, List.of(palette))))));
		}).toList();
		return new CompoundTag("", Map.of("Status", new StringTag("Status", "full"), "yPos", new IntTag("yPos", minSectionY),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of()),
				"sections", new ListTag("sections", CompoundTag.class, sections)));
	}

	private RawChunk readLegacyChunk(String status) throws Exception {
		return readLegacyChunk(status, new WorldInfo(Version.VERSION_13, false));
	}

	private RawChunk readLegacyChunk(String status, WorldInfo worldInfo) throws Exception {
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
				"DataVersion", new IntTag("DataVersion", 1631), "Level", new CompoundTag("Level", level))), worldInfo);
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

	@Test
	void hashChangesWhenOnlyARenderedBlockEntityChanges() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] plain = potChunk(List.of(pot(8, "minecraft:brick"))).calculateHash(digest);
		byte[] samePots = potChunk(List.of(pot(8, "minecraft:brick"))).calculateHash(digest);
		byte[] sherdChanged = potChunk(List.of(pot(8, "minecraft:skull_pottery_sherd"))).calculateHash(digest);

		assertArrayEquals(plain, samePots);
		assertFalse(MessageDigest.isEqual(plain, sherdChanged), "a new pot sherd must invalidate the cached tile");
	}

	@Test
	void renderedEntityHashIgnoresSaveOrder() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] forward = potChunk(List.of(pot(1, "minecraft:brick"), pot(2, "minecraft:skull_pottery_sherd"))).calculateHash(digest);
		byte[] reversed = potChunk(List.of(pot(2, "minecraft:skull_pottery_sherd"), pot(1, "minecraft:brick"))).calculateHash(digest);

		assertArrayEquals(forward, reversed);
	}

	private static CompoundTag pot(int x, String sherd) {
		List<Tag> sherds = List.of(new StringTag("", sherd), new StringTag("", "minecraft:brick"),
				new StringTag("", "minecraft:brick"), new StringTag("", "minecraft:brick"));
		return new CompoundTag("", Map.of(
				"id", new StringTag("id", "minecraft:decorated_pot"),
				"x", new IntTag("x", x), "y", new IntTag("y", 70), "z", new IntTag("z", 8),
				"sherds", new ListTag("sherds", StringTag.class, sherds)));
	}

	private RawChunk potChunk(List<CompoundTag> pots) throws Exception {
		CompoundTag root = new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", 3953),
				"Status", new StringTag("Status", "minecraft:full"),
				"yPos", new IntTag("yPos", -4),
				"block_entities", new ListTag("block_entities", CompoundTag.class, new java.util.ArrayList<Tag>(pots)),
				"sections", new ListTag("sections", CompoundTag.class, List.of())));
		RawChunk chunk = readChunk(root, Version.VERSION_21);
		assertEquals(pots.size(), chunk.getDecoratedPots().size());
		return chunk;
	}

	private RawChunk readChunk(CompoundTag root, Version version) throws Exception {
		return readChunk(root, new WorldInfo(version, false));
	}

	@Test
	void readsSignMessagesFromLegacyJsonAndModernNbtAsLiteralText() throws Exception {
		var legacy = readSign(Version.VERSION_19, Map.of(
				"Text1", new StringTag("Text1", "{\"text\": \"A \\\"quote\\\" \\\\ path\", \"extra\": [{\"text\":\" 日\",\"color\":\"dark_aqua\"}]}"),
				"Text2", new StringTag("Text2", "\"Use <&>\""),
				"Color", new StringTag("Color", "red"), "GlowingText", new ByteTag("GlowingText", (byte) 1)));
		assertEquals("A \"quote\" \\ path 日", legacy.getText1());
		assertEquals("Use <&>", legacy.getText2());
		assertEquals("", legacy.getText3());
		assertEquals("red", legacy.getColor());
		assertTrue(legacy.isGlowing());
		assertEquals(List.of(new SignTextColor(16, 18, 0x00aaaa)), legacy.getTextColors().getFirst());

		var messages = new ListTag("messages", CompoundTag.class, List.of(
				new CompoundTag("", Map.of("", new StringTag("", "\"literal quotes\""))),
				new CompoundTag("", Map.of("text", new StringTag("text", "Café"),
						"color", new StringTag("color", "#123abc"),
						"extra", new ListTag("extra", StringTag.class, List.of(new StringTag("", " 日")))))));
		var modern = readSign(Version.VERSION_26_3, Map.of("front_text", new CompoundTag("front_text", Map.of(
				"messages", messages, "color", new StringTag("color", "light_gray"),
				"has_glowing_text", new ByteTag("has_glowing_text", (byte) 1))),
				"back_text", new CompoundTag("back_text", Map.of("color", new StringTag("color", "yellow"),
						"messages", new ListTag("messages", StringTag.class, List.of(new StringTag("", "裏面")))))));
		assertEquals("\"literal quotes\"", modern.getText1());
		assertEquals("Café 日", modern.getText2());
		assertEquals("", modern.getText4());
		assertEquals("light_gray", modern.getColor());
		assertTrue(modern.isGlowing());
		assertEquals(List.of(new SignTextColor(0, 4, 0x123abc), new SignTextColor(4, 6, 0x123abc)),
				modern.getTextColors().get(1));
		assertEquals("裏面", modern.getBack().lines().getFirst());
		assertEquals("yellow", modern.getBack().color());
		assertFalse(modern.getBack().glowing());
	}

	@Test
	void signStyleAndBackTextChangesInvalidateChunkGeometry() throws Exception {
		var chunk = new RawChunk();
		var digest = MessageDigest.getInstance("SHA-256");
		var front = new SignEntity.Face(List.of("Hello", "", "", ""), "red", false, List.of());
		chunk.setSigns(Map.of("sign", new SignEntity(1, 64, 1, 1, 64, 1, front, null, 0, null, "")));
		byte[] plainHash = chunk.calculateHash(digest);
		var glowing = new SignEntity.Face(front.lines(), front.color(), true, front.textColors());
		chunk.setSigns(Map.of("sign", new SignEntity(1, 64, 1, 1, 64, 1, glowing, null, 0, null, "")));
		byte[] glowingHash = chunk.calculateHash(digest);
		assertFalse(java.util.Arrays.equals(plainHash, glowingHash));
		chunk.setSigns(Map.of("sign", new SignEntity(1, 64, 1, 1, 64, 1, glowing, front, 0, null, "")));
		assertFalse(java.util.Arrays.equals(glowingHash, chunk.calculateHash(digest)));
	}

	private SignEntity readSign(Version version, Map<String, Tag> text) throws Exception {
		var fields = new HashMap<>(text);
		fields.put("id", new StringTag("id", "minecraft:sign"));
		fields.put("x", new IntTag("x", 8));
		fields.put("y", new IntTag("y", 4));
		fields.put("z", new IntTag("z", 8));
		var sign = new CompoundTag("", fields);
		var root = new CompoundTag("", Map.of("DataVersion", new IntTag("DataVersion", version.getDataVersion()),
				"Status", new StringTag("Status", "full"),
				"sections", new ListTag("sections", CompoundTag.class, List.of()),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of(sign))));
		return readChunk(root, version).getSigns().values().iterator().next();
	}

	private RawChunk readChunk(CompoundTag root, WorldInfo worldInfo) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) {
			out.writeTag(root);
		}
		return new RawChunk(new ChunkData(bytes.toByteArray(), Compression.Gzip), null, worldInfo);
	}
}
