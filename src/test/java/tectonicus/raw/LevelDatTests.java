/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import static org.junit.Assert.assertThat;

import java.nio.file.Paths;

import static org.hamcrest.core.Is.*;

import org.junit.Test;

public class LevelDatTests
{
	@Test
	public void createLevelDat() throws Exception
	{
		LevelDat data = new LevelDat(Paths.get("src/test/resources/level.dat"), "");
		assertThat(data.getWorldName(), is("Block Gallery"));
	}
}
