/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.configuration.Dimension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WorldBorderTest {
	@TempDir Path world;

	@Test
	void readsTheLevelDatBorderAndScalesItForTheNether() throws Exception {
		write(world.resolve("level.dat"), "Data", Map.of("BorderCenterX", 800.0, "BorderCenterZ", -160.0, "BorderSize", 4000.0));
		assertEquals(Optional.of(new WorldBorder(800, -160, 4000)), WorldBorder.load(world, world, Dimension.OVERWORLD));
		assertEquals(Optional.of(new WorldBorder(100, -20, 500)), WorldBorder.load(world, world.resolve("DIM-1"), Dimension.NETHER));
	}

	@Test
	void prefersSavedDataAndADimensionsOwnBorder() throws Exception {
		write(world.resolve("level.dat"), "Data", Map.of("BorderSize", 4000.0));
		write(world.resolve("data/minecraft/world_border.dat"), "data", Map.of("center_x", 16.0, "center_z", 32.0, "size", 1000.0));
		Path nether = world.resolve("dimensions/minecraft/the_nether");
		assertEquals(Optional.of(new WorldBorder(16, 32, 1000)), WorldBorder.load(world, world, Dimension.OVERWORLD));
		assertEquals(Optional.of(new WorldBorder(2, 4, 125)), WorldBorder.load(world, nether, Dimension.NETHER));
		write(nether.resolve("data/minecraft/world_border.dat"), "data", Map.of("size", 300.0));
		assertEquals(Optional.of(new WorldBorder(0, 0, 300)), WorldBorder.load(world, nether, Dimension.NETHER));
	}

	@Test
	void worldsWithoutABorderHaveNone() throws Exception {
		write(world.resolve("level.dat"), "Data", Map.of());
		assertEquals(Optional.empty(), WorldBorder.load(world, world, Dimension.OVERWORLD));
	}

	private static void write(Path file, String compound, Map<String, Double> values) throws Exception {
		Files.createDirectories(file.getParent());
		Map<String, Tag> tags = new java.util.HashMap<>();
		values.forEach((name, value) -> tags.put(name, new DoubleTag(name, value)));
		try (NBTOutputStream out = new NBTOutputStream(Files.newOutputStream(file))) {
			out.writeTag(new CompoundTag("", Map.of(compound, new CompoundTag(compound, tags))));
		}
	}
}
