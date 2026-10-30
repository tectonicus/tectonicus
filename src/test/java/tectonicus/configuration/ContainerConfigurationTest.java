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
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;
import tectonicus.configuration.filter.ContainerFilter;
import tectonicus.configuration.filter.ContainerFilterType;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.ContainerKind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContainerConfigurationTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(strings = {"containers", "chests"})
	void acceptsTypeListsAndLootFiltersWithEitherElementName(String tag) throws Exception {
		var map = parse("<map worldDir='.'><" + tag + " filter='player' types=' BARREL, shulker_box,barrel '/></map>").getDimensionConfig(0);
		for (ContainerKind kind : ContainerKind.values()) {
			assertEquals(kind == ContainerKind.BARREL || kind == ContainerKind.SHULKER_BOX, map.getContainerFilter().passesFilter(entity(kind, false)));
			assertFalse(map.getContainerFilter().passesFilter(entity(kind, true)));
		}
		var all = parse("<map worldDir='.'><" + tag + " filter='all'/></map>").getDimensionConfig(0);
		for (ContainerKind kind : ContainerKind.values()) assertTrue(all.getContainerFilter().passesFilter(entity(kind, true)));
		assertFalse(parse("<map worldDir='.'/>").getDimensionConfig(0).getContainerFilter().passesFilter(entity(ContainerKind.CHEST, false)));
		assertFalse(new ContainerFilter(ContainerFilterType.NONE, "barrel").passesFilter(entity(ContainerKind.BARREL, false)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "barrel,", ",chest", "chest,,barrel", "hopper", "white_shulker_box"})
	void rejectsInvalidListsWithSupportedTypes(String types) {
		var error = assertThrows(IllegalArgumentException.class, () -> parse("<map worldDir='.'><containers filter='all' types='" + types + "'/></map>"));
		for (ContainerKind kind : ContainerKind.values()) assertTrue(error.getMessage().contains(kind.getId()));
	}

	@Test
	void prefersNewElementAndPreservesAliasInheritance() throws Exception {
		Configuration config = parse("""
				<config containersInitiallyVisible='true'><containers filter='all' types='chest'/></config>
				<world worldDir='.' chestsInitiallyVisible='false'>
				  <chests types='barrel'/>
				  <dimension><layer/></dimension>
				  <dimension containersInitiallyVisible='true' chestsInitiallyVisible='false'>
				    <chests filter='none'/><containers filter='player' types='shulker_box'/><layer/>
				  </dimension>
				</world>
				<map worldDir='.'><layer/></map>
				""");
		assertTrue(config.isContainersInitiallyVisible());
		var first = config.getDimensionConfig(0);
		assertFalse(first.getMarkerVisibility().isContainers());
		assertTrue(first.getContainerFilter().passesFilter(entity(ContainerKind.BARREL, true)));
		assertFalse(first.getContainerFilter().passesFilter(entity(ContainerKind.CHEST, false)));
		var second = config.getDimensionConfig(1);
		assertTrue(second.getMarkerVisibility().isContainers());
		assertTrue(second.getContainerFilter().passesFilter(entity(ContainerKind.SHULKER_BOX, false)));
		assertFalse(second.getContainerFilter().passesFilter(entity(ContainerKind.SHULKER_BOX, true)));
		assertTrue(config.getDimensionConfig(2).getMarkerVisibility().isContainers());
		assertTrue(config.getDimensionConfig(2).getContainerFilter().passesFilter(entity(ContainerKind.CHEST, false)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"--containersInitiallyVisible", "--chestsInitiallyVisible", "containersInitiallyVisible", "chestsInitiallyVisible"})
	void commandLineAliasesOverrideXmlVisibility(String option) throws Exception {
		Configuration config = parse("<map worldDir='.'/>");
		config.applyCommandLineOverrides(new CommandLine(config).parseArgs(option + "=true"));
		assertTrue(config.getDimensionConfig(0).getMarkerVisibility().isContainers());
	}

	private ContainerEntity entity(ContainerKind kind, boolean unopened) {
		return new ContainerEntity(0, 64, 0, 0, 64, 0, kind.getDisplayName(), "", unopened, "south", "single", List.of(), kind, null);
	}

	@Test
	void exportsContainerNamesThroughoutTheViewerPipeline() throws Exception {
		Configuration config = parse("<config containersInitiallyVisible='true'/><map worldDir='.'><containers filter='all'/><layer/></map>");
		config.setOutputDir(directory.toFile());
		DimensionConfig map = config.getDimensionConfig(0);
		Path contents = directory.resolve("contents.js");
		tectonicus.util.OutputResourcesUtil.outputContents(contents.toFile(), config);
		String data = Files.readString(contents);
		assertTrue(data.contains("containers: " + map.getJavascriptId() + "_containerData"));
		assertTrue(data.contains("containerControlChecked: true"));
		assertFalse(data.contains("chests:"));
		assertFalse(data.contains("chestControlChecked:"));
		String html = Files.readString(tectonicus.util.OutputResourcesUtil.outputHtml(directory.toFile(), config).toPath());
		assertTrue(html.contains("Scripts/" + map.getId() + "/containers.js"));
		assertFalse(html.contains("/chests.js"));
		Path markers = directory.resolve("containers.js");
		tectonicus.util.OutputResourcesUtil.outputContainers(markers.toFile(), map,
				new java.util.concurrent.ConcurrentLinkedQueue<>(List.of(entity(ContainerKind.BARREL, false))));
		assertTrue(Files.readString(markers).contains("var " + map.getJavascriptId() + "_containerData"));
		tectonicus.util.OutputResourcesUtil.outputHtmlResources(config, directory.toFile(), 4, 512, 512);
		String viewer = Files.readString(directory.resolve("Scripts/tectonicus.js"));
		assertTrue(viewer.contains("var containersInitiallyVisible = true;"));
		assertFalse(viewer.contains("{{containersInitiallyVisible}}"));
		assertTrue(viewer.contains("function refreshContainerMarkers("));
		assertTrue(viewer.contains("function createContainerPopup("));
		assertFalse(viewer.contains("function refreshChestMarkers("));
	}

	private Configuration parse(String body) throws Exception {
		Path file = directory.resolve("containers.xml");
		Files.writeString(file, "<tectonicus version='2'>" + body + "</tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}
}
