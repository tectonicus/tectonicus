/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *  
 */

package tectonicus.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.DimensionInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DirUtilsTests {
	@TempDir
	private Path tempDir;
	
	@Test
	void testGetDimensionDirOverworldFallsBackToOldStructure() {
		// When new directory structure doesn't exist, it should fall back to old structure (world root)
		Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.OVERWORLD, "overworld"));
		assertThat(dimensionDir, is(tempDir));
	}
	
	@Test
	void testGetDimensionDirNetherFallsBackToOldStructure() {
		// When new directory structure doesn't exist, it should fall back to DIM-1
		Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.NETHER, "nether"));
		assertThat(dimensionDir, is(tempDir.resolve("DIM-1")));
	}
	
	@Test
	void testGetDimensionDirEndFallsBackToOldStructure() {
		// When new directory structure doesn't exist, it should fall back to DIM1
		Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.END, "end"));
		assertThat(dimensionDir, is(tempDir.resolve("DIM1")));
	}
	
	@Nested
	class TestsForNewDirectoryStructure {
		@BeforeEach
		void setUp() {
			Paths.get(tempDir.toString(), "dimensions", "minecraft", "overworld").toFile().mkdirs();
			Paths.get(tempDir.toString(), "dimensions", "minecraft", "the_nether").toFile().mkdirs();
			Paths.get(tempDir.toString(), "dimensions", "minecraft", "the_end").toFile().mkdirs();
			Paths.get(tempDir.toString(), "dimensions", "minecraft", "level10").toFile().mkdirs();
		}
		
		@Test
		void testGetDimensionDirOverworldNewStructure() {
			Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.OVERWORLD, "overworld"));
			assertThat(dimensionDir, is(tempDir.resolve("dimensions/minecraft/overworld")));
		}
		
		@Test
		void testGetDimensionDirNetherNewStructure() {
			Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.NETHER, "nether"));
			assertThat(dimensionDir, is(tempDir.resolve("dimensions/minecraft/the_nether")));
		}
		
		@Test
		void testGetDimensionDirEndNewStructure() {
			Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.END, "end"));
			assertThat(dimensionDir, is(tempDir.resolve("dimensions/minecraft/the_end")));
		}
		
		@Test
		void testNonVanillaDimension() {
			Path dimensionDir = DirUtils.getDimensionDir(tempDir, new DimensionInfo(Dimension.OTHER, "level10"));
			assertThat(dimensionDir, is(tempDir.resolve("dimensions/minecraft/level10")));
		}
	}
}
