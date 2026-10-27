/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tectonicus.cache.NullTileCache;
import tectonicus.cache.swap.HddTileList;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.MutableLayer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DownsamplerTest {
	@TempDir
	Path directory;
	private final TileCoord parent = new TileCoord(-2, 3);
	private final AtomicInteger completed = new AtomicInteger();
	private final ProgressListener progress = new ProgressListener() {
		public void onTaskStarted(String taskName) {}
		public void onTaskUpdate(int num, int total) {}
	};

	@ParameterizedTest
	@EnumSource(ImageFormat.class)
	void preservesEncodedOutputWithAllChildren(ImageFormat format) throws Exception {
		assertMatchesBicubicReference(format, false);
	}

	@ParameterizedTest
	@EnumSource(ImageFormat.class)
	void preservesTransparencyAndBackgroundWithMissingChildren(ImageFormat format) throws Exception {
		assertMatchesBicubicReference(format, true);
	}

	private void assertMatchesBicubicReference(ImageFormat format, boolean sparse) throws Exception {
		MutableLayer layer = layer(format);
		BufferedImage expected = new BufferedImage(64, 64,
				format.hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D graphics = expected.createGraphics();
		try {
			if (!format.hasAlpha()) {
				graphics.setColor(layer.getBackgroundColorRGB());
				graphics.fillRect(0, 0, 64, 64);
			}
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for (int i = 0; i < 4; i++) {
				if (sparse && (i == 1 || i == 2))
					continue;
				BufferedImage source = new BufferedImage(64, 64,
						format.hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
				for (int y = 0; y < 64; y++) {
					for (int x = 0; x < 64; x++) {
						int alpha = (x * 17 + y * 29 + i * 43) & 255;
						int rgb = ((x * 7 + i * 53) & 255) << 16 | ((y * 11) & 255) << 8 | ((x + y) * 13 & 255);
						source.setRGB(x, y, alpha << 24 | rgb);
					}
				}
				Screenshot.write(child(i, format), source, format, 0.75f);
				// Use ImageIO's standard decode path as the reference, including lossy input decoding.
				try (var input = new FileInputStream(child(i, format))) {
					BufferedImage decoded = ImageIO.read(input);
					assertNotNull(decoded);
					graphics.drawImage(decoded, i % 2 * 32, i / 2 * 32, 32, 32, null);
				}
			}
		} finally {
			graphics.dispose();
		}
		File reference = directory.resolve("reference." + format.getExtension()).toFile();
		Screenshot.write(reference, expected, format, 0.75f);
		run(layer);
		assertArrayEquals(Files.readAllBytes(reference.toPath()), Files.readAllBytes(output(format).toPath()));
		assertEquals(1, completed.get());
		assertEquals(List.of(output(format).getAbsolutePath()), Files.readAllLines(directory.resolve("changes.txt")));
	}

	private MutableLayer layer(ImageFormat format) {
		MutableLayer layer = new MutableLayer("day", "map");
		layer.setImageFormat(format);
		layer.setImageCompressionLevel(0.75f);
		layer.setBackgroundColor("#123456");
		return layer;
	}

	private File child(int quadrant, ImageFormat format) {
		return TileRenderer.getImageFile(directory.resolve("input").toFile(),
				parent.x * 2 + quadrant % 2, parent.y * 2 + quadrant / 2, format);
	}

	private File output(ImageFormat format) {
		return TileRenderer.getImageFile(directory.resolve("output").toFile(), parent.x, parent.y, format);
	}

	private void run(MutableLayer layer) {
		HddTileList tiles = new HddTileList(directory.resolve("tiles").toFile());
		tiles.add(parent);
		ChangeFile changes = new ChangeFile(directory.resolve("changes.txt").toFile());
		try {
			new Downsampler(2, changes).downsample(directory.resolve("input").toFile(), directory.resolve("output").toFile(),
					tiles, layer, 64, 64, progress, new NullTileCache() {
						@Override
						public void updateTileDownsampleStatus(TileCoord coord, int zoomLevel, boolean empty) {
							assertEquals(parent, coord);
							assertEquals(3, zoomLevel);
							assertFalse(empty);
							completed.incrementAndGet();
						}
					}, 3);
		} finally {
			changes.close();
		}
	}
}
