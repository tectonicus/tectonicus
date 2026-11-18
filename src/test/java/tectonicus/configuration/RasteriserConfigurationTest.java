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
		assertEquals(-1, config.getEglDevice());
	}

	@Test void eglDeviceIsReadFromTheRasteriserNode() throws Exception {
		var config = parse("<rasteriser type=\"lwjgl\" useEGL=\"true\" eglDevice=\"1\"/>");
		assertTrue(config.isUseEGL());
		assertEquals(1, config.getEglDevice());
	}

	@Test void invalidEglDeviceFailsWithAnActionableError() {
		for (String value : new String[]{"-1", "gpu"}) {
			var error = assertThrows(IllegalArgumentException.class, () -> parse("<rasteriser type=\"lwjgl\" eglDevice=\"" + value + "\"/>"));
			assertTrue(error.getMessage().contains("eglDevice"));
		}
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
