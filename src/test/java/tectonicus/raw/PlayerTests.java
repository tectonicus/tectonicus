/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class PlayerTests
{
	@Test
	void testLoadPlayerFromPlayerFile() throws Exception
	{
		Player player = new Player(Paths.get("src/test/resources/8aeb40ad-a60c-429e-b239-25b7223c5fb7.dat"));
		assertThat(player.getUuid(), is("8aeb40ada60c429eb23925b7223c5fb7"));
		assertThat((int)player.getHealth(), is(20));
		assertThat(player.getXpTotal(), is(11));
	}
}
