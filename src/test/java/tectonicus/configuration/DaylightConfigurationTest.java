/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.chunk.NetherLightmap;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DaylightConfigurationTest {
	@TempDir Path directory;

	@Test void omittedBrightnessUsesTheLightingDefaultAndZeroOverridesIt() throws Exception {
		var config = parse("""
			<world worldDir=".">
			  <dimension><layer/></dimension>
			  <dimension id="minecraft:the_nether"><layer/><layer><render lightmapBrightness="0"/></layer></dimension>
			</world>
			""");
		assertEquals(0, config.getDimensionConfig(0).getLayer(0).getLightmapBrightness());
		var nether = config.getDimensionConfig(1);
		float defaultBrightness = NetherLightmap.DEFAULT_BRIGHTNESS;
		assertEquals(defaultBrightness, nether.getLayer(0).getLightmapBrightness(defaultBrightness));
		assertEquals(0, nether.getLayer(1).getLightmapBrightness(defaultBrightness));
	}

	@Test void brightnessInheritsAndProfilesAllowExplicitMoodyOverrides() throws Exception {
		var config = parse("""
			<renderProfiles><profile name="bright" lightmapBrightness="1"/></renderProfiles>
			<config><render lightmapBrightness="0.2"/></config>
			<world worldDir="."><render lightmapBrightness="0.4"/>
			  <dimension><render lightmapBrightness="0.6"/>
			    <layer/><layer><render lightmapBrightness="0"/></layer>
			    <layer><render profile="bright"/></layer>
			    <layer><render profile="bright" lightmapBrightness="0.5"/></layer>
			  </dimension>
			  <dimension><layer/></dimension>
			</world>
			<map worldDir="."><layer/></map>
			""");
		var map = config.getDimensionConfig(0);
		assertEquals(0.6f, map.getLayer(0).getLightmapBrightness());
		assertEquals(0, map.getLayer(1).getLightmapBrightness());
		assertEquals(1, map.getLayer(2).getLightmapBrightness());
		assertEquals(0.5f, map.getLayer(3).getLightmapBrightness());
		assertEquals(0.4f, config.getDimensionConfig(1).getLayer(0).getLightmapBrightness());
		assertEquals(0.2f, config.getDimensionConfig(2).getLayer(0).getLightmapBrightness());
	}

	@Test void invalidBrightnessFailsWithAnActionableError() {
		for (String value : new String[]{"-0.1", "1.1", "NaN", "Infinity", "bright"}) {
			var error = assertThrows(IllegalArgumentException.class, () -> parse(
					"<map worldDir=\".\"><layer><render lightmapBrightness=\"" + value + "\"/></layer></map>"));
			assertTrue(error.getMessage().contains("lightmapBrightness"));
			assertTrue(error.getMessage().contains("between 0 and 1"));
		}
	}

	private Configuration parse(String xml) throws Exception {
		Path file = directory.resolve("config.xml");
		Files.writeString(file, "<tectonicus version=\"2\">" + xml + "</tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}
}
