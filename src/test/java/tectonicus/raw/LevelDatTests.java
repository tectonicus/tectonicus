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

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import tectonicus.configuration.Dimension;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

class LevelDatTests {
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
