/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongArrayTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkData;
import tectonicus.configuration.Dimension;
import tectonicus.raw.Biomes;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.RawChunk;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BiomeSamplerTest {
	private static final long WORLD_SEED = 268372865167095444L;
	private int previousHeight;

	@BeforeEach
	void setWorldHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(384);
	}

	@AfterEach
	void restoreWorldHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@Test
	void fixesReportedWaterAndGrassBoundariesInSavedWorld() throws Exception {
		BiomeSampler sampler = new BiomeSampler(WORLD_SEED, Version.VERSION_26_3, Dimension.OVERWORLD);
		RawChunk water = new RawChunk(Path.of("src/test/resources/biomes/boundary-26.3-35--1.nbt").toFile());
		RawChunk grass = new RawChunk(Path.of("src/test/resources/biomes/boundary-26.3-36--4.nbt").toFile());
		assertSame(Biomes.OCEAN, water.getBiome(11, 63 + 64, 6));
		assertSame(Biomes.SWAMP, sampler.getBiome(water, 11, 63 + 64, 6, coord -> fail("Unexpected neighboring chunk")));
		assertSame(Biomes.DESERT, grass.getBiome(8, 66 + 64, 12));
		assertSame(Biomes.OCEAN, sampler.getBiome(grass, 8, 66 + 64, 12, coord -> fail("Unexpected neighboring chunk")));
	}

	@Test
	void hashesTheWorldSeedWithVanillaLittleEndianSha256() {
		assertEquals(822356173898575612L, BiomeSampler.obfuscateSeed(WORLD_SEED));
	}

	// Selected quart positions captured from the official 1.15.2 and 1.17.1 zoomers, zoom seed zero.
	@ParameterizedTest
	@CsvSource({"571,63,-10,142,15,-3", "584,66,-52,146,16,-14", "0,0,0,-1,-1,-1",
			"-1,-1,-1,-1,-1,-1", "15,63,15,3,15,3", "16,64,16,4,16,4",
			"-16,-64,-16,-4,-16,-5", "-17,319,-17,-4,79,-5",
			"29999999,319,-29999999,7499999,79,-7500000"})
	void matchesVanillaQuartSelections(int x, int y, int z, int qx, int qy, int qz) {
		assertEquals(new BiomeSampler.QuartPosition(qx, qy, qz), BiomeSampler.selectQuart(0, x, y, z));
	}

	@ParameterizedTest
	@EnumSource(value = Version.class, names = {"VERSION_15", "VERSION_16", "VERSION_17"})
	void earlyOverworldSamplingIgnoresHeightButOtherDimensionsUseThreeDimensions(Version version) throws Exception {
		int[] volume = new int[1024];
		Arrays.fill(volume, 1);
		volume[(0 * 4 + 2) * 4 + 2] = 6; // Constant-column result for world X=584, Z=-52.
		volume[(16 * 4 + 3) * 4 + 1] = 0; // 3D result at world Y=66.
		RawChunk chunk = legacy(36, -4, new IntArrayTag("Biomes", volume));
		BiomeSampler overworld = new BiomeSampler(WORLD_SEED, version, Dimension.OVERWORLD);
		assertSame(BiomesOld.SWAMP, overworld.getBiome(chunk, 8, 66, 12, coord -> null));
		assertSame(BiomesOld.SWAMP, overworld.getBiome(chunk, 8, 130, 12, coord -> null));
		assertSame(BiomesOld.OCEAN, new BiomeSampler(WORLD_SEED, version, Dimension.NETHER).getBiome(chunk, 8, 66, 12, coord -> null));
		assertSame(BiomesOld.OCEAN, new BiomeSampler(WORLD_SEED, version, Dimension.END).getBiome(chunk, 8, 66, 12, coord -> null));
		assertSame(BiomesOld.OCEAN, new BiomeSampler(WORLD_SEED, Version.VERSION_18, Dimension.OVERWORLD).getBiome(chunk, 8, 66, 12, coord -> null));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void oldColumnBiomesStayExactEvenWhenRenderingWithANewerVersion(boolean bytes) throws Exception {
		int[] ids = new int[256];
		ids[12 * 16 + 8] = 134;
		byte[] byteIds = new byte[256];
		byteIds[12 * 16 + 8] = (byte)134;
		RawChunk chunk = legacy(36, -4, bytes ? new ByteArrayTag("Biomes", byteIds) : new IntArrayTag("Biomes", ids));
		for (Version version : List.of(Version.VERSION_12, Version.VERSION_14, Version.VERSION_26_3)) {
			assertSame(BiomesOld.SWAMP_HILLS, new BiomeSampler(WORLD_SEED, version, Dimension.OVERWORLD)
					.getBiome(chunk, 8, 66, 12, coord -> fail("Column biomes must not query neighbors")));
		}
	}

	@Test
	void samplesAcrossNegativeChunkBoundaryAndUsesNoiseDataInNewNeighbors() throws Exception {
		RawChunk center = modern(4671, 0, 0, 0, uniform("biomes", "forest"), null);
		RawChunk neighbor = modern(5125, 0, -1, 0, uniform("biomes", "desert"), uniform("noise_biomes", "swamp"));
		BiomeSampler sampler = new BiomeSampler(WORLD_SEED, Version.VERSION_26_3, Dimension.OVERWORLD);
		assertSame(Biomes.SWAMP, sampler.getBiome(center, 0, 0, 0, coord -> {
			assertEquals(new ChunkCoord(0, -1), coord);
			return neighbor;
		}));
		assertSame(Biomes.DESERT, sampler.getBiome(neighbor, 0, 0, 0, coord -> fail("Final biomes must not be zoomed again")));
		assertSame(Biomes.FOREST, sampler.getBiome(center, 0, 0, 0, coord -> null));
		assertSame(Biomes.FOREST, new BiomeSampler(null, Version.VERSION_26_3, Dimension.OVERWORLD)
				.getBiome(center, 0, 0, 0, coord -> fail("Unknown seed must not invent a boundary")));
	}

	@ParameterizedTest
	@ValueSource(ints = {5119, 5125})
	void finalBlockBiomesDecodeAll4096EntriesIncludingPaddedLongs(int dataVersion) throws Exception {
		List<Tag> palette = List.of(new StringTag("", "minecraft:desert"), new StringTag("", "minecraft:swamp"),
				new StringTag("", "minecraft:ocean"), new StringTag("", "minecraft:forest"), new StringTag("", "minecraft:meadow"));
		long[] data = new long[(4096 + 20) / 21]; // Five entries use three bits, leaving one unused bit per long.
		for (int i = 0; i < 4096; i++)
			data[i / 21] |= (long)(i % 5) << (i % 21 * 3);
		CompoundTag biomes = new CompoundTag("biomes", Map.of("palette", new ListTag("palette", StringTag.class, palette),
				"data", new LongArrayTag("data", data)));
		RawChunk chunk = modern(dataVersion, 0, 0, -4, biomes, null);
		BiomeSampler sampler = new BiomeSampler(WORLD_SEED, Version.VERSION_26_3, Dimension.OVERWORLD);
		for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
			int index = (y * 16 + z) * 16 + x;
			Biomes expected = Biomes.byId(((String)palette.get(index % 5).getValue()).substring(10));
			assertSame(expected, sampler.getBiome(chunk, x, y + 64, z, coord -> fail("Final biomes must be direct")));
		}
	}

	@Test
	void clampsLegacyQuartLookupsAtBothHeightLimits() throws Exception {
		int[] ids = new int[1024];
		Arrays.fill(ids, 6);
		ids[(63 * 4 + 3) * 4 + 3] = 2;
		RawChunk chunk = legacy(0, 0, new IntArrayTag("Biomes", ids));
		assertSame(BiomesOld.SWAMP, chunk.getBiome(15, -4, 15));
		assertSame(BiomesOld.DESERT, chunk.getBiome(15, 256, 15));
	}

	@ParameterizedTest
	@ValueSource(ints = {256, 1024})
	void biomeOnlyEditsInvalidateLegacyChunkHashes(int volume) throws Exception {
		int[] ids = new int[volume];
		RawChunk before = legacy(0, 0, new IntArrayTag("Biomes", ids));
		ids[0] = 6;
		RawChunk after = legacy(0, 0, new IntArrayTag("Biomes", ids));
		assertFalse(Arrays.equals(before.calculateHash(MessageDigest.getInstance("SHA-1")),
				after.calculateHash(MessageDigest.getInstance("SHA-1"))));
	}

	@ParameterizedTest
	@ValueSource(ints = {4671, 5125})
	void biomeOnlyEditsInvalidateModernChunkHashes(int version) throws Exception {
		RawChunk before = modern(version, 0, 0, 0, uniform("biomes", "forest"), null);
		RawChunk after = modern(version, 0, 0, 0, uniform("biomes", "swamp"), null);
		assertFalse(Arrays.equals(before.calculateHash(MessageDigest.getInstance("SHA-1")),
				after.calculateHash(MessageDigest.getInstance("SHA-1"))));
	}

	@Test
	void noiseBiomeEditsInvalidateHashesUsedByOlderNeighborChunks() throws Exception {
		RawChunk before = modern(5125, 0, 0, 0, uniform("biomes", "forest"), uniform("noise_biomes", "forest"));
		RawChunk after = modern(5125, 0, 0, 0, uniform("biomes", "forest"), uniform("noise_biomes", "swamp"));
		assertFalse(Arrays.equals(before.calculateHash(MessageDigest.getInstance("SHA-1")),
				after.calculateHash(MessageDigest.getInstance("SHA-1"))));
	}

	private RawChunk legacy(int x, int z, Tag biomes) throws Exception {
		CompoundTag level = new CompoundTag("Level", Map.of("xPos", new IntTag("xPos", x), "zPos", new IntTag("zPos", z),
				"Sections", new ListTag("Sections", CompoundTag.class, List.of()), "Biomes", biomes));
		return read(new CompoundTag("", Map.of("Level", level)));
	}

	private RawChunk modern(int version, int x, int z, int minSectionY, CompoundTag biomes, CompoundTag noiseBiomes) throws Exception {
		var sectionTags = new java.util.HashMap<String, Tag>();
		sectionTags.put("Y", new ByteTag("Y", (byte)0));
		sectionTags.put("biomes", biomes);
		if (noiseBiomes != null)
			sectionTags.put("noise_biomes", noiseBiomes);
		return read(new CompoundTag("", Map.of("DataVersion", new IntTag("DataVersion", version),
				"xPos", new IntTag("xPos", x), "zPos", new IntTag("zPos", z), "yPos", new IntTag("yPos", minSectionY),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of()),
				"sections", new ListTag("sections", CompoundTag.class, List.of(new CompoundTag("", sectionTags))))));
	}

	private CompoundTag uniform(String name, String biome) {
		return new CompoundTag(name, Map.of("palette", new ListTag("palette", StringTag.class,
				List.of(new StringTag("", "minecraft:" + biome)))));
	}

	private RawChunk read(CompoundTag root) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) {
			out.writeTag(root);
		}
		return new RawChunk(new ChunkData(bytes.toByteArray(), NBTInputStream.Compression.Gzip), null,
				new WorldInfo(Version.VERSION_26_3, false));
	}
}
