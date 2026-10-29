package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.DimensionConfig;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileIndexWriterTest {
	@TempDir
	Path tempDir;

	@Test
	void recordsExactCoverageAtEachZoomWithNegativeCoordinatesAndHoles() throws IOException {
		for (int x : new int[] {-18, -17, -16, -10, -9, 0, 1, 3})
			createTile(tempDir, 0, x, -3, "webp");
		createTile(tempDir, 0, 5, 4, "webp");
		createTile(tempDir, 2, -1, 2, "webp");
		assertEquals("[{\"-3\":[-18,-16,-10,-9,0,1,3,3],\"4\":[5,5]},{},{\"2\":[-1,-1]}]",
				index(tempDir, 2, ImageFormat.WEBP));
	}

	@Test
	void indexesOnlyFilesWithTheConfiguredFormatAndCorrectBuckets() throws IOException {
		createTile(tempDir, 0, -1, -2, "png");
		createTile(tempDir, 0, 0, 0, "webp");
		Files.createFile(tempDir.resolve("Zoom0/0/0/tile_-1_-2.webp"));
		Files.createDirectory(tempDir.resolve("Zoom0/0/0/tile_16_16.webp"));
		Files.createFile(tempDir.resolve("Zoom0/0/0/preview.webp"));
		assertEquals("[{\"0\":[0,0]}]", index(tempDir, 0, ImageFormat.WEBP));
		assertEquals("[{\"-2\":[-1,-1]}]", index(tempDir, 0, ImageFormat.PNG));
	}

	@Test
	void reflectsExistingTilesAcrossRepeatedExportsAndDeletions() throws IOException {
		assertEquals("[{},{}]", index(tempDir, 1, ImageFormat.WEBP));
		Path first = createTile(tempDir, 0, -5, -5, "webp");
		assertEquals("[{\"-5\":[-5,-5]},{}]", index(tempDir, 1, ImageFormat.WEBP));
		createTile(tempDir, 0, -4, -5, "webp");
		assertEquals("[{\"-5\":[-5,-4]},{}]", index(tempDir, 1, ImageFormat.WEBP));
		Files.delete(first);
		assertEquals("[{\"-5\":[-4,-4]},{}]", index(tempDir, 1, ImageFormat.WEBP));
	}

	@Test
	void exportsSeparateIndexesForEachLayerInContents() throws IOException {
		Configuration config = new Configuration();
		config.setOutputDir(tempDir.toFile());
		config.setNumZoomLevels(0);
		DimensionConfig map = new DimensionConfig("Map0");
		map.setNumZoomLevels(0);
		LayerConfig first = new LayerConfig("LayerA", "Map0");
		LayerConfig second = new LayerConfig("LayerB", "Map0");
		map.addLayer(first);
		map.addLayer(second);
		config.addDimension(map);
		createTile(DirUtils.getLayerDir(config.getOutputDir(), first).toPath(), 0, -5, -5, "png");
		createTile(DirUtils.getLayerDir(config.getOutputDir(), second).toPath(), 0, 2, 3, "png");
		Path contents = tempDir.resolve("contents.js");
		OutputResourcesUtil.outputContents(contents.toFile(), config);
		String output = Files.readString(contents);
		assertTrue(output.contains("tileIndex: [{\"-5\":[-5,-5]}],"));
		assertTrue(output.contains("tileIndex: [{\"3\":[2,2]}],"));
	}

	private String index(Path layerDir, int maxZoom, ImageFormat format) throws IOException {
		StringWriter writer = new StringWriter();
		TileIndexWriter.write(writer, layerDir, maxZoom, format);
		return writer.toString();
	}

	private Path createTile(Path layerDir, int zoom, int x, int y, String extension) throws IOException {
		Path tile = layerDir.resolve("Zoom" + zoom).resolve("" + (x % 16)).resolve("" + (y % 16))
				.resolve("tile_" + x + "_" + y + "." + extension);
		Files.createDirectories(tile.getParent());
		return Files.createFile(tile);
	}
}
