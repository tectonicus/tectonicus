/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.DimensionInfo;
import tectonicus.configuration.WorldConfig;
import tectonicus.util.OutputResourcesUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmptyDimensionRenderTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(strings = {"missing", "data", "empty-region"})
	void skipsUngeneratedNetherAndStillIncludesLaterDimensions(String layout) throws Exception {
		Configuration config = configuration(false);
		WorldConfig world = config.getWorlds().getFirst();
		DimensionConfig overworld = add(world, Dimension.OVERWORLD);
		add(world, Dimension.NETHER);
		DimensionConfig end = add(world, Dimension.END);
		region("region/r.0.0.mca");
		region("DIM1/region/r.0.0.mcr");
		if (layout.equals("data")) {
			Files.createDirectories(directory.resolve("DIM-1/data"));
			Files.write(directory.resolve("DIM-1/data/chunks.dat"), new byte[0]);
		} else if (layout.equals("empty-region")) {
			Files.createDirectories(directory.resolve("DIM-1/region"));
		}
		assertEquals(List.of(overworld, end), TileRenderer.findRenderableDimensions(config));
		assertEquals(3, config.getDimensions().size(), "Filtering must not modify the user's configuration");
	}

	@Test
	void viewerOmitsSkippedLastDimensionAndItsScripts() throws Exception {
		Configuration config = configuration(false);
		WorldConfig world = config.getWorlds().getFirst();
		DimensionConfig overworld = add(world, Dimension.OVERWORLD);
		overworld.setNumZoomLevels(4);
		DimensionConfig nether = add(world, Dimension.NETHER);
		nether.setNumZoomLevels(9);
		region("region/r.0.0.mca");

		List<DimensionConfig> maps = TileRenderer.findRenderableDimensions(config);
		assertSame(overworld, maps.getLast(), "Shared resources must be exported from the last renderable dimension");
		Path contents = directory.resolve("contents.js");
		OutputResourcesUtil.outputContents(contents.toFile(), config, maps);
		String script = Files.readString(contents);
		assertTrue(script.contains("maxZoom = 4;"));
		assertTrue(script.contains(overworld.getJavascriptId() + "_worldVectors"));
		assertFalse(script.contains(nether.getJavascriptId()));
		String html = Files.readString(OutputResourcesUtil.outputHtml(directory.toFile(), config, maps).toPath());
		assertTrue(html.contains("Scripts/" + overworld.getId() + "/worldVectors.js"));
		assertFalse(html.contains("Scripts/" + nether.getId() + "/"));
	}

	@Test
	void allEmptyDimensionsProduceClearFailure() throws Exception {
		Configuration config = configuration(false);
		add(config.getWorlds().getFirst(), Dimension.NETHER);
		assertEquals("Unable to render: no configured dimensions contain terrain.",
				assertThrows(IllegalStateException.class, () -> TileRenderer.findRenderableDimensions(config)).getMessage());
	}

	@Test
	void invalidWorldStillFails() throws Exception {
		Configuration config = configuration(false);
		add(config.getWorlds().getFirst(), Dimension.OVERWORLD);
		Files.delete(directory.resolve("level.dat"));
		assertThrows(IllegalArgumentException.class, () -> TileRenderer.findRenderableDimensions(config));
	}

	@Test
	void alphaWorldDoesNotRequireRegionFiles() throws Exception {
		Configuration config = configuration(true);
		DimensionConfig overworld = add(config.getWorlds().getFirst(), Dimension.OVERWORLD);
		assertEquals(List.of(overworld), TileRenderer.findRenderableDimensions(config));
	}

	private Configuration configuration(boolean alpha) throws Exception {
		try (NBTOutputStream out = new NBTOutputStream(Files.newOutputStream(directory.resolve("level.dat")))) {
			out.writeTag(new CompoundTag("", Map.of("Data", new CompoundTag("Data",
					Map.of("version", new IntTag("version", alpha ? 0 : 19133))))));
		}
		Configuration config = new Configuration();
		config.setOutputDir(directory.toFile());
		config.setUseCdn("");
		config.addWorld(new WorldConfig("World", directory.toFile()));
		return config;
	}

	private DimensionConfig add(WorldConfig world, Dimension dimension) {
		DimensionConfig map = new DimensionConfig(dimension.name());
		map.setDimension(dimension);
		map.setDimensionInfo(new DimensionInfo(dimension, dimension.getId()));
		world.addDimension(map);
		return map;
	}

	private void region(String path) throws Exception {
		Path file = directory.resolve(path);
		Files.createDirectories(file.getParent());
		Files.write(file, new byte[8192]);
	}
}
