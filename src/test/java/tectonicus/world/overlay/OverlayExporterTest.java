/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.OverlayConfig.Visibility;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.CircularWorldSubset;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OverlayExporterTest {
	private static final long SEED = 7_355_608_123_456_789L;

	@TempDir Path directory;

	@Test
	void slimeChunkTestMatchesJavaEditionsFormula() {
		// Reference values computed with long arithmetic and explicit 32-bit wrapping, as Java's int math does.
		for (int x = -40; x <= 40; x += 7) {
			for (int z = -40; z <= 40; z += 9) {
				long wrapped = SEED + (int) ((long) x * x * 0x4c1906) + (int) ((long) x * 0x5ac0db)
						+ (long) (int) ((long) z * z) * 0x4307a7L + (int) ((long) z * 0x5f24f) ^ 0x3ad8025fL;
				assertEquals(new Random(wrapped).nextInt(10) == 0, OverlayExporter.isSlimeChunk(SEED, x, z), x + "," + z);
			}
		}
		// Coordinates whose squares overflow an int must still give one in ten.
		int count = 0;
		for (int x = 1_000_000; x < 1_000_100; x++)
			for (int z = -1_000_100; z < -1_000_000; z++)
				if (OverlayExporter.isSlimeChunk(SEED, x, z)) count++;
		assertTrue(count > 800 && count < 1200, "slime chunks: " + count);
	}

	@Test
	void exportsSlimeChunksButNeverTheSeed() throws Exception {
		DimensionConfig map = map(Dimension.OVERWORLD);
		map.getOverlays().setSlimeChunks(Visibility.HIDDEN);
		map.getOverlays().setChunkGrid(Visibility.SHOWN);
		OverlayExporter exporter = new OverlayExporter(map, SEED);
		int expected = 0;
		for (int x = -8; x < 8; x++) {
			for (int z = -8; z < 8; z++) {
				exporter.addRenderedChunk(new ChunkCoord(x, z));
				if (OverlayExporter.isSlimeChunk(SEED, x, z)) expected++;
			}
		}
		File file = directory.resolve("overlays.js").toFile();
		exporter.write(file, directory, directory);
		String script = Files.readString(file.toPath());
		assertTrue(script.startsWith("var " + map.getJavascriptId() + "_overlays = {"));
		assertTrue(script.contains("\"chunkGrid\":{\"visible\":true}"));
		assertTrue(script.contains("\"slimeChunks\":{\"visible\":false,\"chunks\":["));
		assertFalse(script.contains(Long.toString(SEED)));
		int listed = script.substring(script.indexOf("\"chunks\":[") + 10, script.indexOf(']')).split(",").length / 2;
		assertEquals(expected, listed);
		assertFalse(script.contains("worldBorder"));
	}

	@Test
	void slimeChunksNeedAColumnInsideTheSubsetNotJustTheEdgeBuffer() throws Exception {
		var subset = new CircularWorldSubset(new Vector3l(0, 64, 0), 40);
		// Chunk 3 starts at x=48, outside the circle, but the renderer loads it for edge geometry.
		assertTrue(subset.contains(new ChunkCoord(3, 0)));
		assertFalse(OverlayExporter.hasColumnInSubset(subset, new ChunkCoord(3, 0)));
		assertTrue(OverlayExporter.hasColumnInSubset(subset, new ChunkCoord(2, 0)));
		assertTrue(OverlayExporter.hasColumnInSubset(subset, new ChunkCoord(-3, 0)));

		DimensionConfig map = slimeMap(Dimension.OVERWORLD);
		map.setWorldSubset(subset);
		OverlayExporter exporter = new OverlayExporter(map, SEED);
		int expected = 0;
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				ChunkCoord coord = new ChunkCoord(x, z);
				if (!subset.contains(coord)) continue;
				exporter.addRenderedChunk(coord);
				if (OverlayExporter.isSlimeChunk(SEED, x, z) && OverlayExporter.hasColumnInSubset(subset, coord)) expected++;
			}
		}
		File file = directory.resolve("overlays.js").toFile();
		exporter.write(file, directory, directory);
		String script = Files.readString(file.toPath());
		String chunks = script.substring(script.indexOf("\"chunks\":[") + 10, script.indexOf(']'));
		assertEquals(expected, chunks.isEmpty() ? 0 : chunks.split(",").length / 2);
	}

	@Test
	void skipsSlimeChunksOutsideTheOverworldOrWithoutASeed() throws Exception {
		for (OverlayExporter exporter : new OverlayExporter[] {
				new OverlayExporter(slimeMap(Dimension.NETHER), SEED), new OverlayExporter(slimeMap(Dimension.OVERWORLD), null)}) {
			exporter.addRenderedChunk(new ChunkCoord(0, 0));
			File file = directory.resolve("overlays.js").toFile();
			exporter.write(file, directory, directory);
			assertFalse(Files.readString(file.toPath()).contains("slimeChunks"));
		}
	}

	@Test
	void removesStaleOutputWhenDisabled() throws Exception {
		File file = directory.resolve("overlays.js").toFile();
		Files.writeString(file.toPath(), "stale");
		new OverlayExporter(map(Dimension.OVERWORLD), SEED).write(file, directory, directory);
		assertFalse(file.exists());
	}

	@Test
	void writesFeaturesAndReportsBadFilesWithoutFailing() throws Exception {
		DimensionConfig map = map(Dimension.OVERWORLD);
		Path features = directory.resolve("features.json");
		Files.writeString(features, "[{\"type\": \"line\", \"points\": [[0, 0], [5, 5]]}]");
		map.getOverlays().setFeaturesFile(features.toFile());
		map.getOverlays().setFeatures(Visibility.SHOWN);
		File file = directory.resolve("overlays.js").toFile();
		new OverlayExporter(map, null).write(file, directory, directory);
		assertTrue(Files.readString(file.toPath()).contains("\"features\":{\"visible\":true,\"items\":[{\"type\":\"line\""));

		Files.writeString(features, "[{\"type\": \"line\"}]");
		new OverlayExporter(map, null).write(file, directory, directory);
		assertFalse(Files.readString(file.toPath()).contains("features"));
	}

	private static DimensionConfig slimeMap(Dimension dimension) {
		DimensionConfig map = map(dimension);
		map.getOverlays().setSlimeChunks(Visibility.SHOWN);
		return map;
	}

	private static DimensionConfig map(Dimension dimension) {
		DimensionConfig map = new DimensionConfig("Map0");
		map.setDimension(dimension);
		return map;
	}
}
