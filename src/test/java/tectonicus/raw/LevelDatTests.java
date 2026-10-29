/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jnbt.CompoundTag;
import org.jnbt.LongTag;
import org.jnbt.IntArrayTag;
import org.jnbt.StringTag;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.configuration.Dimension;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LevelDatTests {
	@TempDir
	Path directory;

	@ParameterizedTest
	@ValueSource(longs = {0, -1, 268372865167095444L, Long.MIN_VALUE, Long.MAX_VALUE})
	void readsLegacyAndModernWorldSeedLocations(long seed) throws Exception {
		Path level = directory.resolve("level.dat");
		writeNbt(level, new CompoundTag("", Map.of("Data", new CompoundTag("Data",
				Map.of("RandomSeed", new LongTag("RandomSeed", seed))))));
		assertEquals(seed, new LevelDat(level, "").getWorldSeed());

		writeNbt(level, new CompoundTag("", Map.of("Data", new CompoundTag("Data", Map.of(
				"WorldGenSettings", new CompoundTag("WorldGenSettings", Map.of("seed", new LongTag("seed", seed))))))));
		assertEquals(seed, new LevelDat(level, "").getWorldSeed());

		writeNbt(level, new CompoundTag("", Map.of("Data", new CompoundTag("Data", Map.of()))));
		Path settings = directory.resolve("data/minecraft/world_gen_settings.dat");
		Files.createDirectories(settings.getParent());
		writeNbt(settings, new CompoundTag("", Map.of("data", new CompoundTag("data", Map.of("seed", new LongTag("seed", seed))))));
		assertEquals(seed, new LevelDat(level, "").getWorldSeed());
	}

	@Test
	void missingSeedStaysUnknownRatherThanDefaultingToZero() throws Exception {
		Path level = directory.resolve("level.dat");
		writeNbt(level, new CompoundTag("", Map.of("Data", new CompoundTag("Data", Map.of()))));
		assertNull(new LevelDat(level, "").getWorldSeed());
	}

	@Test
	void retainsTheCustomWorldSpawnDimension() throws Exception {
		Path level = directory.resolve("level.dat");
		writeNbt(level, new CompoundTag("", Map.of("Data", new CompoundTag("Data", Map.of(
				"spawn", new CompoundTag("spawn", Map.of("dimension", new StringTag("dimension", "example:moon"),
						"pos", new IntArrayTag("pos", new int[]{10, 64, 20}))))))));
		LevelDat data = new LevelDat(level, "");
		assertEquals(Dimension.OTHER, data.getSpawnDimension());
		assertEquals("example:moon", data.getSpawnDimensionId());
	}

	private void writeNbt(Path path, Tag tag) throws Exception {
		try (NBTOutputStream out = new NBTOutputStream(Files.newOutputStream(path))) {
			out.writeTag(tag);
		}
	}
	@Test
	void createLevelDat() {
		LevelDat data = new LevelDat(Paths.get("src/test/resources/level1_12.dat"), "");
		assertThat(data.getWorldName(), is("Block Gallery"));
		assertThat(data.isSnapshot(), is(false));
		assertThat(data.getVersion(), is("1.12.2"));
	}
	
	@Test
	void createLevelDatFromOldLevel() {
		LevelDat data = new LevelDat(Paths.get("src/test/resources/oldLevel.dat"), "");
		assertThat(data.getWorldName(), is("Test18"));
		assertThat(data.getVersion(), is(IsNull.nullValue()));
	}
	
	@Test
	void verifySpawnTagLoading() {
		LevelDat data = new LevelDat(Paths.get("src/test/resources/level.dat"), "");
		assertThat(data.getVersion(), is("1.21.9"));
		assertThat(data.getSpawnPosition().x, is(601L));
		assertThat(data.getSpawnPosition().y, is(64L));
		assertThat(data.getSpawnPosition().z, is(-56L));
		assertThat(data.getSpawnDimension(), is(Dimension.OVERWORLD));
	}
	
	@Test
	void testNetherSpawnLevelDat() {
		LevelDat data = new LevelDat(Paths.get("src/test/resources/netherSpawnLevel.dat"), "");
		assertThat(data.getVersion(), is("1.21.9"));
		assertThat(data.getSpawnDimension(), is(Dimension.NETHER));
	}
	
	@Test
	void verifyOldFormatSpawnCoordinates() {
		LevelDat data = new LevelDat(Paths.get("src/test/resources/level1_12.dat"), "");
		assertThat(data.getSpawnPosition().x, is(601L));
		assertThat(data.getSpawnPosition().y, is(64L));
		assertThat(data.getSpawnPosition().z, is(-56L));
	}
}
