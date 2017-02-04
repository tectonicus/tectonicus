/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class MinecraftTests
{
	@Test
	public void findServerPlayerFileFindsWhitelistJson()
	{
		Path p = Minecraft.findServerPlayerFile(Paths.get("src/test/resources/Canned.basic"), "whitelist");
		assertThat(p, is(equalTo(Paths.get("src/test/resources/whitelist.json"))));
	}
}
