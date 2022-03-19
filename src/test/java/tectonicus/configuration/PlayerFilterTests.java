/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import tectonicus.raw.Player;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

class PlayerFilterTests
{
	@Test
	void testCreatePlayerFilter()
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.WHITELIST, Paths.get("src/test/resources/whitelist.json"), Paths.get("src/test/resources/Canned"), true, true);
		assertThat(pf.toString(), containsString("whitelist.json"));
	}
	
	@Test
	void passesFilterWithFilterTypeAll()
	{
		PlayerFilter pf = new PlayerFilter();
		boolean pass = pf.passesFilter(new Player("", "", ""));
		assertThat(pass, is(true));
	}
	
	@Test
	void passesFilterWithJsonWhitelist()
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.WHITELIST, Paths.get("src/test/resources/whitelist.json"), Paths.get("src/test/resources/Canned"), true, true);
		boolean pass = pf.passesFilter(new Player("androidz", "", ""));
		assertThat(pass, is(true));
	}
	
	@Test
	void passesFilterWithTxtWhitelist()
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.WHITELIST, Paths.get("src/test/resources/whitelist.txt"), Paths.get("src/test/resources/Canned"), true, true);
		boolean pass = pf.passesFilter(new Player("androidz", "", ""));
		assertThat(pass, is(true));
	}
	
	@Test
	void createPlayerFilterWithDefaultWhitelist()
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.WHITELIST, Paths.get("."), Paths.get("src/test/resources/Canned"), true, true);
		assertThat(pf.toString(), is("Whitelist: ."));
		assertThat(pf.passesFilter(new Player("androidz", "", "")), is(true));
	}
}
