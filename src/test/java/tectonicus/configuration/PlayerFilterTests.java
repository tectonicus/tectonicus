/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.*;

import java.nio.file.Paths;

import org.junit.Test;

import tectonicus.raw.Player;

public class PlayerFilterTests
{
	@Test
	public void testCreatePlayerFilter() throws Exception
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.Whitelist, Paths.get("src/test/resources/whitelist.json"), Paths.get("src/test/resources/Canned.basic"));
		assertThat(pf.toString(), is(equalTo("Whitelist: src\\test\\resources\\whitelist.json")));
	}
	
	@Test
	public void passesFilterWithFilterTypeAll() throws Exception
	{
		PlayerFilter pf = new PlayerFilter();
		boolean pass = pf.passesFilter(new Player("", "", ""));
		assertThat(pass, is(equalTo(true)));
	}
	
	@Test
	public void passesFilterWithJsonWhitelist() throws Exception
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.Whitelist, Paths.get("src/test/resources/whitelist.json"), Paths.get("src/test/resources/Canned.basic"));
		boolean pass = pf.passesFilter(new Player("androidz", "", ""));
		assertThat(pass, is(equalTo(true)));
	}
	
	@Test
	public void passesFilterWithTxtWhitelist() throws Exception
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.Whitelist, Paths.get("src/test/resources/whitelist.txt"), Paths.get("src/test/resources/Canned.basic"));
		boolean pass = pf.passesFilter(new Player("androidz", "", ""));
		assertThat(pass, is(equalTo(true)));
	}
	
	@Test
	public void createPlayerFilterWithDefaultWhitelist() throws Exception
	{
		PlayerFilter pf = new PlayerFilter(PlayerFilterType.Whitelist, Paths.get("."), Paths.get("src/test/resources/Canned.basic"));
		assertThat(pf.toString(), is(equalTo("Whitelist: .")));
		assertThat(pf.passesFilter(new Player("androidz", "", "")), is(equalTo(true)));
	}
}
