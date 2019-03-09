/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LevelDatTests
{
	@Test
	public void createLevelDat()
	{
		LevelDat data = new LevelDat(Paths.get("src/test/resources/level.dat"), "");
		assertThat(data.getWorldName(), is("Block Gallery"));
		assertThat(data.isSnapshot(), is(false));
		assertThat(data.getVersion(), is("1.11.2"));
	}

	@Test
	public void createLevelDatFromOldLevel()
	{
		LevelDat data = new LevelDat(Paths.get("src/test/resources/oldLevel.dat"), "");
		assertThat(data.getWorldName(), is("Test18"));
		assertThat(data.getVersion(), is(IsNull.nullValue()));
	}
}
