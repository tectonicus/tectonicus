/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BiomeBlendConfigurationTest {
	@TempDir
	Path directory;

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
	void readsEveryMinecraftRadiusFromRenderConfig(int radius) throws Exception {
		Map map = parseMap("<render biomeBlendRadius=\"" + radius + "\" />");
		assertEquals(radius, map.getBiomeBlendRadius());
		assertTrue(map.isBiomeBlendRadiusConfigured());
	}

	@Test
	void omittedOptionUsesMinecraftDefault() throws Exception {
		assertEquals(2, new MutableMap("map").getBiomeBlendRadius());
		assertEquals(2, parseMap("").getBiomeBlendRadius());
		assertEquals(2, parseMap("<render />").getBiomeBlendRadius());
		assertFalse(new MutableMap("map").isBiomeBlendRadiusConfigured());
		assertFalse(parseMap("").isBiomeBlendRadiusConfigured());
		assertFalse(parseMap("<render />").isBiomeBlendRadiusConfigured());
		assertFalse(parseMap("<render biomeBlendRadius=\" \" />").isBiomeBlendRadiusConfigured());
	}

	@ParameterizedTest
	@CsvSource({"-1,0", "8,7", "2147483647,7", "invalid,2", "2.5,2", "'',2", "' 3 ',3"})
	void handlesOutOfRangeAndMalformedXmlValues(String value, int expected) throws Exception {
		assertEquals(expected, parseMap("<render biomeBlendRadius=\"" + value + "\" />").getBiomeBlendRadius());
	}

	@Test
	void keepsMapSettingsIndependent() throws Exception {
		Path file = directory.resolve("maps.xml");
		Files.writeString(file, """
				<tectonicus version="2">
					<map name="Unblended" worldDir="."><render biomeBlendRadius="0" /></map>
					<map name="Maximum" worldDir="."><render biomeBlendRadius="7" /></map>
				</tectonicus>
				""");
		var maps = XmlConfigurationParser.parseConfiguration(file.toFile()).getMaps();
		assertEquals(0, maps.get(0).getBiomeBlendRadius());
		assertEquals(7, maps.get(1).getBiomeBlendRadius());
	}

	@Test
	void programmaticConfigRejectsUnsupportedRadii() {
		MutableMap map = new MutableMap("map");
		assertThrows(IllegalArgumentException.class, () -> map.setBiomeBlendRadius(-1));
		assertThrows(IllegalArgumentException.class, () -> map.setBiomeBlendRadius(8));
	}

	private Map parseMap(String render) throws Exception {
		Path file = directory.resolve("config.xml");
		Files.writeString(file, "<tectonicus version=\"2\"><map name=\"Test\" worldDir=\".\">"
				+ render + "</map></tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile()).getMaps().getFirst();
	}
}
