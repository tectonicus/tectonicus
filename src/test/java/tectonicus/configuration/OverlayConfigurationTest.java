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
import tectonicus.configuration.OverlayConfig.Visibility;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OverlayConfigurationTest {
	@TempDir Path directory;

	@Test
	void overlaysAreOffUnlessConfigured() throws Exception {
		OverlayConfig overlays = parse("<map worldDir='.'/>").getDimensionConfig(0).getOverlays();
		assertFalse(overlays.isEnabled());
		assertEquals(Visibility.OFF, overlays.getSlimeChunks());
		assertEquals(OverlayConfig.DEFAULT_ELEVATION, overlays.getElevation());
	}

	@Test
	void parsesEachOverlayAndResolvesTheFileNextToTheConfig() throws Exception {
		OverlayConfig overlays = parse("""
				<map worldDir='.'>
				  <overlays chunkGrid='hidden' regionGrid='on' slimeChunks='true' worldBorder='off' file='roads.json' y='70'/>
				</map>""").getDimensionConfig(0).getOverlays();
		assertEquals(Visibility.HIDDEN, overlays.getChunkGrid());
		assertEquals(Visibility.SHOWN, overlays.getRegionGrid());
		assertEquals(Visibility.SHOWN, overlays.getSlimeChunks());
		assertEquals(Visibility.OFF, overlays.getWorldBorder());
		assertEquals(Visibility.SHOWN, overlays.getFeatures());
		assertEquals(directory.resolve("roads.json").toAbsolutePath(), overlays.getFeaturesFile().toPath().toAbsolutePath());
		assertEquals(70, overlays.getElevation());
		assertTrue(overlays.isEnabled());
	}

	@Test
	void dimensionsInheritAndOverrideWorldOverlays() throws Exception {
		Configuration config = parse("""
				<world worldDir='.'>
				  <overlays chunkGrid='on' slimeChunks='hidden'/>
				  <dimension><layer/></dimension>
				  <dimension><overlays slimeChunks='off' fileVisible='hidden' file='nether.json'/><layer/></dimension>
				</world>""");
		OverlayConfig first = config.getDimensionConfig(0).getOverlays();
		assertEquals(Visibility.SHOWN, first.getChunkGrid());
		assertEquals(Visibility.HIDDEN, first.getSlimeChunks());
		assertNull(first.getFeaturesFile());
		OverlayConfig second = config.getDimensionConfig(1).getOverlays();
		assertEquals(Visibility.SHOWN, second.getChunkGrid());
		assertEquals(Visibility.OFF, second.getSlimeChunks());
		assertEquals(Visibility.HIDDEN, second.getFeatures());
	}

	@Test
	void invalidValuesFallBackToOff() throws Exception {
		OverlayConfig overlays = parse("<map worldDir='.'><overlays chunkGrid='sometimes' y='high'/></map>")
				.getDimensionConfig(0).getOverlays();
		assertEquals(Visibility.OFF, overlays.getChunkGrid());
		assertEquals(OverlayConfig.DEFAULT_ELEVATION, overlays.getElevation());
	}

	private Configuration parse(String body) throws Exception {
		Path file = directory.resolve("overlays.xml");
		Files.writeString(file, "<tectonicus version='2'>" + body + "</tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}
}
