/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTOutputStream;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.SaveFormat;
import tectonicus.Version;
import tectonicus.configuration.Dimension;
import tectonicus.raw.LevelDat;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionHeightTest {
	@TempDir
	Path world;

	private DimensionHeight resolve(Map<String, Tag> types, List<String> packs, String id, Dimension dimension, Version version) {
		return DimensionHeight.resolve(world, types, packs, id, dimension, version, SaveFormat.ANVIL);
	}

	@Test
	void vanillaDimensionsKeepTheirUsualHeights() {
		assertEquals(DimensionHeight.CAVES_AND_CLIFFS, resolve(Map.of(), List.of(), "minecraft:overworld", Dimension.OVERWORLD, Version.VERSION_21));
		assertEquals(DimensionHeight.CLASSIC, resolve(Map.of(), List.of(), "minecraft:the_nether", Dimension.NETHER, Version.VERSION_21));
		assertEquals(DimensionHeight.CLASSIC, resolve(Map.of(), List.of(), "minecraft:overworld", Dimension.OVERWORLD, Version.VERSION_17));
		assertEquals(DimensionHeight.MC_REGION, DimensionHeight.resolve(world, Map.of(), List.of(), "minecraft:overworld",
				Dimension.OVERWORLD, Version.VERSION_UNKNOWN, SaveFormat.MC_REGION));
		// A custom dimension using the vanilla nether type is 256 high even though it isn't the Nether itself.
		assertEquals(DimensionHeight.CLASSIC, resolve(Map.of("mod:caves", new StringTag("type", "minecraft:the_nether")),
				List.of(), "mod:caves", Dimension.OTHER, Version.VERSION_21));
	}

	@Test
	void readsInlineDimensionTypes() {
		CompoundTag type = new CompoundTag("type", Map.of("min_y", new IntTag("min_y", -128), "height", new IntTag("height", 512)));
		assertEquals(new DimensionHeight(-128, 512), resolve(Map.of("mod:deep", type), List.of(), "mod:deep", Dimension.OTHER, Version.VERSION_21));
	}

	@Test
	void readsDimensionTypesFromZipAndFolderDatapacksWithLaterPacksWinning() throws Exception {
		Path packs = Files.createDirectories(world.resolve("datapacks"));
		writeZipPack(packs.resolve("tall.zip"), "data/minecraft/dimension_type/overworld.json", "{\"min_y\": -128, \"height\": 512}");
		Path folder = packs.resolve("taller");
		Files.createDirectories(folder.resolve("data/minecraft/dimension_type"));
		Files.writeString(folder.resolve("data/minecraft/dimension_type/overworld.json"), "{\"min_y\": -256, \"height\": 768}");
		Map<String, Tag> types = Map.of("minecraft:overworld", new StringTag("type", "minecraft:overworld"));

		assertEquals(new DimensionHeight(-128, 512), resolve(types, List.of("file/tall.zip"), "minecraft:overworld", Dimension.OVERWORLD, Version.VERSION_21));
		assertEquals(new DimensionHeight(-256, 768), resolve(types, List.of("file/tall.zip", "file/taller"), "minecraft:overworld", Dimension.OVERWORLD, Version.VERSION_21));
		// Disabled packs are ignored.
		assertEquals(DimensionHeight.CAVES_AND_CLIFFS, resolve(types, List.of(), "minecraft:overworld", Dimension.OVERWORLD, Version.VERSION_21));
	}

	@Test
	void invalidHeightsFallBackToDefaults() throws Exception {
		Path pack = Files.createDirectories(world.resolve("datapacks/bad/data/mod/dimension_type"));
		Files.writeString(pack.resolve("odd.json"), "{\"min_y\": -10, \"height\": 300}");
		assertEquals(DimensionHeight.CAVES_AND_CLIFFS, resolve(Map.of("mod:odd", new StringTag("type", "mod:odd")),
				List.of("file/bad"), "mod:odd", Dimension.OTHER, Version.VERSION_21));
	}

	@Test
	void levelDatRecordsEachDimensionsType() throws Exception {
		CompoundTag inline = new CompoundTag("type", Map.of("min_y", new IntTag("min_y", 0), "height", new IntTag("height", 1024)));
		CompoundTag dimensions = new CompoundTag("dimensions", Map.of(
				"minecraft:overworld", new CompoundTag("minecraft:overworld", Map.of("type", new StringTag("type", "minecraft:overworld"))),
				"mod:sky", new CompoundTag("mod:sky", Map.of("type", inline))));
		CompoundTag data = new CompoundTag("Data", Map.of("WorldGenSettings", new CompoundTag("WorldGenSettings", Map.of("dimensions", dimensions))));
		Path levelDat = world.resolve("level.dat");
		try (NBTOutputStream out = new NBTOutputStream(Files.newOutputStream(levelDat))) {
			out.writeTag(new CompoundTag("", Map.of("Data", data)));
		}
		LevelDat level = new LevelDat(levelDat, "Player");
		assertEquals(new DimensionHeight(0, 1024), resolve(level.getDimensionTypes(), level.getDataPacks(), "mod:sky", Dimension.OTHER, Version.VERSION_21));
		assertEquals(DimensionHeight.CAVES_AND_CLIFFS, resolve(level.getDimensionTypes(), level.getDataPacks(), "minecraft:overworld", Dimension.OVERWORLD, Version.VERSION_21));
	}

	private static void writeZipPack(Path zip, String entry, String json) throws Exception {
		try (OutputStream file = Files.newOutputStream(zip); ZipOutputStream out = new ZipOutputStream(file)) {
			out.putNextEntry(new ZipEntry(entry));
			out.write(json.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
	}
}
