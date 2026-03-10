/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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

class MinecraftTests
{
	@Test
	 void findServerPlayerFileFindsWhitelistJson()
	{
		Path p = Minecraft.findServerPlayerFile(Paths.get("src/test/resources/Canned"), "whitelist");
		assertThat(p, is(Paths.get("src/test/resources/whitelist.json")));
	}
	
	@Test
	void testIsValidWorldDir()
	{
		assertThat(Minecraft.isValidWorldDir(Paths.get("src/test/resources")), is(true));
		assertThat(Minecraft.isValidWorldDir(Paths.get("src/test")), is(false));
		assertThat(Minecraft.isValidWorldDir(null), is(false));
	}
	
	@Test
	void testFindLevelDat()
	{
		Path p = Minecraft.findLevelDat(Paths.get("src/test/resources"));
		assertThat(p, is(Paths.get("src/test/resources/level.dat")));
	}
	
	@Test
	void testGetVersion()
	{
		assertThat(Minecraft.getVersion("1.12"), is(Version.VERSION_12));
		assertThat(Minecraft.getVersion("1.21 Pre-Release 1"), is(Version.VERSION_21));
		assertThat(Minecraft.getVersion("1.21.8"), is(Version.VERSION_21));
		assertThat(Minecraft.getVersion("1.21.9"), is(Version.VERSION_21_9_PLUS));
		assertThat(Minecraft.getVersion("25w10a"), is(Version.VERSION_UNKNOWN));
		assertThat(Minecraft.getVersion("26.1 Snapshot 11"), is(Version.VERSION_26_1));
		assertThat(Minecraft.getVersion("26.1 Pre-Release 1"), is(Version.VERSION_26_1));
		assertThat(Minecraft.getVersion("26.1"), is(Version.VERSION_26_1));
	}
	
}
