/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.Minecraft;
import tectonicus.RegionCache;
import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import org.tectonicus.jnbt.CompoundTag;
import tectonicus.configuration.Dimension;
import tectonicus.raw.ArmorStandEntity;
import tectonicus.raw.BannerEntity;
import tectonicus.raw.Biomes;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.CustomNameTag;
import tectonicus.raw.EnchantmentsTag;
import tectonicus.raw.Item;
import tectonicus.raw.ItemFrameEntity;
import tectonicus.raw.PaintingEntity;
import tectonicus.raw.Player;
import tectonicus.raw.SavedMap;
import tectonicus.raw.LevelDat;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.world.DimensionHeight;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the synthetic Bedrock world in src/test/resources/bedrock/world, made by SourceData/bedrock/test-world:
 * 4x4 Overworld chunks of stone, dirt and grass with a pond, a waterlogged fence, a sign, a double chest and a tree,
 * swamp and cherry grove chunks, an unfinished chunk that must be skipped, and one Nether chunk. Chunk 1,0 holds
 * blocks whose Java state depends on their neighbours or block entities, a painting and an armor stand; the world
 * also has a local player, another player and a map.
 */
class BedrockWorldTest {
	@TempDir
	static Path temp;
	static Path world;
	static Map<Path, Long> before;
	static DimensionHeight previousHeight;
	static final long MAP_UUID = -123456789012L;

	@BeforeAll
	static void copyWorld() throws IOException {
		Path source = Path.of("src/test/resources/bedrock/world");
		world = temp.resolve("world");
		try (Stream<Path> files = Files.walk(source)) {
			for (Path file : files.toList()) {
				Path target = world.resolve(source.relativize(file).toString());
				if (Files.isDirectory(file))
					Files.createDirectories(target);
				else
					Files.copy(file, target);
			}
		}
		Files.createDirectories(world.resolve("db"));
		Files.writeString(world.resolve("db/LOCK"), "");
		before = snapshot(world);
		previousHeight = Minecraft.getDimensionHeight();
		Minecraft.setDimensionHeight(DimensionHeight.CAVES_AND_CLIFFS);
	}

	@AfterAll
	static void restoreHeight() {
		Minecraft.setDimensionHeight(previousHeight);
	}

	private static Map<Path, Long> snapshot(Path dir) throws IOException {
		try (Stream<Path> files = Files.walk(dir)) {
			return files.filter(Files::isRegularFile).collect(Collectors.toMap(p -> p, p -> {
				try {
					return Files.size(p) * 31 + Files.getLastModifiedTime(p).toMillis();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
		}
	}

	private static BedrockWorld.Dimension overworld() {
		return BedrockWorld.forDimensionDir(world.toFile()).orElseThrow();
	}

	@Test
	void detectsTheWorldAndItsDimensions() {
		assertTrue(BedrockWorld.isBedrockWorld(world));
		assertTrue(Minecraft.isValidDimensionDir(world.toFile()));
		assertTrue(Minecraft.isValidDimensionDir(world.resolve("DIM-1").toFile()));
		assertEquals(BedrockWorld.NETHER, BedrockWorld.forDimensionDir(world.resolve("DIM-1").toFile()).orElseThrow().id());
		assertEquals(SaveFormat.BEDROCK, new RegionCache(world.resolve("DIM1").toFile()).getFormat());
		assertFalse(BedrockWorld.isBedrockWorld(temp));
	}

	@Test
	void readsLevelDat() {
		LevelDat level = new LevelDat(world.resolve("level.dat"), "");
		assertTrue(level.isBedrock());
		assertEquals("Synthetic Bedrock", level.getWorldName());
		assertEquals("1.21.120", level.getBedrockVersion());
		assertEquals("1.21.10", level.getVersion());
		assertEquals(12345L, level.getWorldSeed());
		assertEquals(64, level.getSpawnPosition().y); // Saved as 32767, "find the surface"
	}

	@Test
	void listsOnlyFinishedChunks() {
		List<ChunkCoord> chunks = overworld().chunkCoords();
		assertEquals(16, chunks.size());
		assertFalse(chunks.contains(new ChunkCoord(5, 5)));
		assertTrue(chunks.contains(new ChunkCoord(-2, -2)));
		assertEquals(List.of(new ChunkCoord(0, 0)), BedrockWorld.forDimensionDir(world.resolve("DIM-1").toFile()).orElseThrow().chunkCoords());
	}

	@Test
	void convertsBlocksToJavaStates() throws IOException {
		RawChunk chunk = overworld().read(new ChunkCoord(0, 0));
		int y0 = 64; // Local y of world y 0
		assertEquals("minecraft:bedrock", chunk.getBlockName(0, y0 - 64, 0));
		assertEquals("minecraft:stone", chunk.getBlockName(4, y0 + 10, 4));
		assertEquals("minecraft:grass_block", chunk.getBlockName(10, y0 + 61, 10));
		assertEquals("minecraft:water", chunk.getBlockName(2, y0 + 61, 2));
		assertEquals("minecraft:air", chunk.getBlockName(10, y0 + 70, 10));

		assertEquals("minecraft:oak_fence", chunk.getBlockName(3, y0 + 62, 3));
		assertEquals("true", chunk.getBlockState(3, y0 + 62, 3).get("waterlogged"));
		assertEquals("false", chunk.getBlockState(12, y0 + 62, 2).get("waterlogged"));

		assertEquals("minecraft:chest", chunk.getBlockName(9, y0 + 62, 10));
		assertEquals("right", chunk.getBlockState(9, y0 + 62, 10).get("type"));
		assertEquals("left", chunk.getBlockState(10, y0 + 62, 10).get("type"));
		assertEquals("south", chunk.getBlockState(10, y0 + 62, 10).get("facing"));
	}

	@Test
	void readsSigns() throws IOException {
		RawChunk chunk = overworld().read(new ChunkCoord(0, 0));
		Map<String, SignEntity> signs = chunk.getSigns();
		assertEquals(1, signs.size());
		SignEntity sign = signs.values().iterator().next();
		assertEquals(List.of("Hello", "Bedrock", "World", ""), List.of(sign.getText1(), sign.getText2(), sign.getText3(), sign.getText4()));
		assertEquals(8, sign.getX());
		assertEquals(62, sign.getY());
		assertEquals("black", sign.getColor());
		assertEquals("minecraft:oak_sign", sign.getBlockName());
	}

	@Test
	void readsBiomes() throws IOException {
		assertEquals(Biomes.PLAINS, overworld().read(new ChunkCoord(0, 0)).getBiome(5, 100, 5));
		assertEquals(Biomes.SWAMP, overworld().read(new ChunkCoord(-1, -1)).getBiome(5, 100, 5));
		assertEquals(Biomes.CHERRY_GROVE, overworld().read(new ChunkCoord(1, 1)).getBiome(5, 0, 5));
	}

	@Test
	void computesLight() throws IOException {
		RawChunk chunk = overworld().read(new ChunkCoord(0, 0));
		int y0 = 64;
		assertEquals(15, chunk.getSkyLight(10, y0 + 62, 10));
		assertEquals(0, chunk.getSkyLight(10, y0 + 50, 10));
		assertEquals(14, chunk.getSkyLight(2, y0 + 61, 2)); // Water dims sky light by one
		assertEquals(0, chunk.getSkyLight(2, y0 + 60, 2)); // Dirt under the pond
		assertEquals(0, chunk.getBlockLight(10, y0 + 62, 10));

		RawChunk east = overworld().read(new ChunkCoord(1, 0));
		assertEquals(15, east.getBlockLight(6, y0 + 62, 14)); // Glowstone
		assertEquals(14, east.getBlockLight(6, y0 + 63, 14));
		assertEquals(13, east.getBlockLight(7, y0 + 63, 14));
		assertEquals(14, east.getSkyLight(9, y0 + 62, 1)); // Under the beacon's iron, lit from the side

		RawChunk nether = BedrockWorld.forDimensionDir(world.resolve("DIM-1").toFile()).orElseThrow().read(new ChunkCoord(0, 0));
		assertEquals(0, nether.getSkyLight(0, 64 + 10, 0)); // Air above the netherrack: no sky in the Nether
	}

	@Test
	void derivesStatesFromNeighbours() throws IOException {
		int y0 = 64;
		RawChunk chunk = overworld().read(new ChunkCoord(0, 0));
		assertEquals("true", chunk.getBlockState(15, y0 + 62, 2).get("east")); // Joins the fence in the next chunk
		assertEquals("true", chunk.getBlockState(15, y0 + 62, 2).get("west"));
		assertEquals("false", chunk.getBlockState(12, y0 + 62, 2).get("west"));
		assertEquals("false", chunk.getBlockState(10, y0 + 61, 10).get("snowy"));

		RawChunk east = overworld().read(new ChunkCoord(1, 0));
		assertEquals("true", east.getBlockState(0, y0 + 62, 2).get("west"));
		assertEquals("false", east.getBlockState(1, y0 + 62, 2).get("east"));
		assertEquals("outer_left", east.getBlockState(4, y0 + 62, 4).get("shape"));
		assertEquals("true", east.getBlockState(8, y0 + 62, 8).get("east")); // Pane against stone
		assertEquals("false", east.getBlockState(8, y0 + 62, 8).get("west"));
		assertEquals("side", east.getBlockState(3, y0 + 62, 12).get("east"));
		assertEquals("side", east.getBlockState(3, y0 + 62, 12).get("west"));
		assertEquals("none", east.getBlockState(3, y0 + 62, 12).get("north"));
		assertEquals("side", east.getBlockState(2, y0 + 62, 12).get("west")); // The end of a line runs straight on

		BlockProperties lowerDoor = east.getBlockState(11, y0 + 62, 12);
		BlockProperties upperDoor = east.getBlockState(11, y0 + 63, 12);
		assertEquals(lowerDoor.get("facing"), upperDoor.get("facing"));
		assertEquals("true", upperDoor.get("open"));
		assertEquals(upperDoor.get("hinge"), lowerDoor.get("hinge"));
		assertEquals("right", lowerDoor.get("hinge"));
		assertEquals("true", east.getBlockState(13, y0 + 61, 14).get("snowy"));
	}

	@Test
	void convertsBlockEntities() throws IOException {
		int y0 = 64;
		RawChunk east = overworld().read(new ChunkCoord(1, 0));
		assertEquals("minecraft:red_banner", east.getBlockName(0, y0 + 62, 0));
		BannerEntity banner = east.getBanners().values().iterator().next();
		assertEquals(1, banner.getPatterns().size());
		assertEquals("minecraft:blue_bed", east.getBlockName(1, y0 + 62, 0));
		assertEquals("minecraft:blue_bed", east.getBlockName(1, y0 + 62, 1));
		assertEquals(11, east.getBeds().values().iterator().next().getColor());
		assertEquals("minecraft:creeper_head", east.getBlockName(2, y0 + 62, 0));
		assertEquals("4", east.getBlockState(2, y0 + 62, 0).get("rotation"));
		assertEquals("minecraft:potted_poppy", east.getBlockName(3, y0 + 62, 0));
		assertEquals("minecraft:air", east.getBlockName(4, y0 + 62, 0));
		assertEquals("minecraft:air", east.getBlockName(5, y0 + 62, 0));
		assertEquals("up", east.getBlockState(7, y0 + 62, 0).get("facing"));
		assertEquals("minecraft:angler_pottery_sherd", east.getDecoratedPots().values().iterator().next().getSherd1());
		assertEquals(1, east.getBeacons().values().iterator().next().getLevels());

		List<ItemFrameEntity> frames = east.getItemFrames();
		assertEquals(2, frames.size());
		ItemFrameEntity frame = frames.stream().filter(f -> !f.isGlow()).findFirst().orElseThrow();
		assertEquals("minecraft:diamond", frame.getItem().id);
		assertEquals(2, frame.getItemRotation());
		assertEquals(3, frame.getDirection());
		ItemFrameEntity glow = frames.stream().filter(ItemFrameEntity::isGlow).findFirst().orElseThrow();
		assertTrue(glow.isFilledMap());
		assertEquals(BedrockItems.mapId(MAP_UUID), glow.getMapId());

		ContainerEntity shulker = east.getContainers().getFirst();
		assertEquals("minecraft:diamond", shulker.getItems().getFirst().id);
		assertEquals(5, shulker.getItems().getFirst().count);

		ContainerEntity chest = overworld().read(new ChunkCoord(0, 0)).getContainers().stream()
				.filter(c -> !c.getItems().isEmpty()).findFirst().orElseThrow();
		Item sword = chest.getItems().getFirst();
		assertEquals("minecraft:diamond_sword", sword.id);
		assertEquals("Blade", sword.getComponent(CustomNameTag.class).name);
		assertEquals("minecraft:sharpness", sword.getComponent(EnchantmentsTag.class).enchantments.getFirst().id);
	}

	@Test
	void convertsEntities() throws IOException {
		RawChunk east = overworld().read(new ChunkCoord(1, 0));
		PaintingEntity painting = east.getPaintings().getFirst();
		assertEquals("minecraft:kebab", painting.getMotive());
		assertEquals(26, painting.getX());
		assertEquals(63, painting.getY());
		ArmorStandEntity stand = east.getArmorStands().getFirst();
		assertEquals("minecraft:diamond_helmet", stand.getHeadArmor().id);
		assertEquals("minecraft:iron_sword", stand.getMainHandItem().id);
		assertTrue(stand.isShowArms());
	}

	@Test
	void readsPlayersAndMaps() throws IOException {
		Player player = new LevelDat(world.resolve("level.dat"), "Steve").getSinglePlayer();
		assertEquals("Steve", player.getName());
		assertEquals(62.0, player.getPosition().y, 1e-4);
		assertEquals(18, player.getHealth(), 1e-6);
		assertEquals("minecraft:apple", player.getInventory().getFirst().id);

		List<CompoundTag> others = BedrockWorld.open(world).serverPlayers();
		assertEquals(1, others.size());
		assertEquals(Dimension.NETHER, new Player(null, others.getFirst()).getDimension());

		SavedMap map = SavedMap.fromRgba(BedrockWorld.open(world).mapPixels(MAP_UUID));
		assertEquals(0xFFFF0000, map.toImage().getRGB(64, 64));
	}

	@Test
	void leavesTheWorldUntouched() throws IOException {
		BedrockWorld.Dimension dimension = overworld();
		for (ChunkCoord coord : dimension.chunkCoords())
			assertNotNull(dimension.read(coord));
		assertEquals(before, snapshot(world));
	}
}
