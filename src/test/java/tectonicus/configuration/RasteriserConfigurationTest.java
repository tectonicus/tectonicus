/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RasteriserConfigurationTest {
	@TempDir Path directory;

	@Test void headlessContextOptionsDefaultToOff() throws Exception {
		var config = parse("<rasteriser type=\"lwjgl\"/>");
		assertFalse(config.isUseEGL());
		assertFalse(config.isUseOSMesa());
	}

	@Test void useOSMesaIsReadFromTheRasteriserNode() throws Exception {
		var config = parse("<rasteriser type=\"lwjgl\" useOSMesa=\"true\"/>");
		assertTrue(config.isUseOSMesa());
		assertFalse(config.isUseEGL());
	}

	private Configuration parse(String xml) throws Exception {
		Path file = directory.resolve("config.xml");
		Files.writeString(file, "<tectonicus version=\"2\">" + xml + "<map worldDir=\".\"><layer/></map></tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}
}
