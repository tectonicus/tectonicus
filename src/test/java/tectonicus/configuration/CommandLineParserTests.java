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
import static org.hamcrest.core.Is.*;

import org.junit.Test;

public class CommandLineParserTests {

	@Test
	public void testMode() throws Exception
	{
		MutableConfiguration config = CommandLineParser.parseCommandLine(new String[] {"mode=cmd"});
		assertThat(config.getMode(), is(Configuration.Mode.CommandLine));
		
		config = CommandLineParser.parseCommandLine(new String[] {"mode=interactive"});
		assertThat(config.getMode(), is(Configuration.Mode.Interactive));
	}
}
