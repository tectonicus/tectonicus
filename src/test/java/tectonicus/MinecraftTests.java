/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class MinecraftTests
{
	@Test
	public void findServerPlayerFileFindsWhitelistJson()
	{
		Path p = Minecraft.findServerPlayerFile(Paths.get("src/test/resources/Canned"), "whitelist");
		assertThat(p, is(Paths.get("src/test/resources/whitelist.json")));
	}
	
	@Test
	public void testIsValidWorldDir()
	{
		assertThat(Minecraft.isValidWorldDir(Paths.get("src/test/resources")), is(true));
		assertThat(Minecraft.isValidWorldDir(Paths.get("src/test")), is(false));
		assertThat(Minecraft.isValidWorldDir(null), is(false));
	}
	
	@Test
	public void testFindLevelDat()
	{
		Path p = Minecraft.findLevelDat(Paths.get("src/test/resources"));
		assertThat(p, is(Paths.get("src/test/resources/level.dat")));
	}
}
