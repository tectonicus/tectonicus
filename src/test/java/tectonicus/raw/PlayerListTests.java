/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.*;

import java.io.File;

import org.junit.Test;

import tectonicus.raw.PlayerList;

public class PlayerListTests 
{
	@Test
	public void testEmptyPlayerList()
	{
		PlayerList pl = new PlayerList();
		assertFalse(pl.contains(""));
	}
	
	@Test
	public void testJSONPlayerList() throws Exception
	{
		PlayerList pl = new PlayerList(new File("src/test/resources/whitelist.json"));
		assertThat(pl.contains("androidz"), is(equalTo(true)));
		assertThat(pl.contains("Ricola"), is(equalTo(true)));
	}
	
	@Test
	public void testTextPlayerList() throws Exception
	{
		PlayerList pl = new PlayerList(new File("src/test/resources/whitelist.txt"));
		assertThat(pl.contains("androidz"), is(equalTo(true)));
		assertThat(pl.contains("Ricola"), is(equalTo(true)));
	}

}
