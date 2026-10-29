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
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.configuration.ImageFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WebPImageIoTest {

	@TempDir
	Path directory;

	@ParameterizedTest
	@ValueSource(ints = {BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_4BYTE_ABGR})
	void concurrentTileWritesPreserveEveryAlphaValue(int imageType) throws Exception {
		ImageWriteQueue queue = new ImageWriteQueue(4);
		try {
			for (int tile = 0; tile < 128; tile++) {
				queue.write(directory.resolve(tile + ".webp").toFile(), tileImage(imageType, tile), ImageFormat.WEBP, 0.75f);
			}
		} finally {
			queue.waitUntilFinished();
		}

		for (int tile = 0; tile < 128; tile++) {
			BufferedImage source = tileImage(imageType, tile);
			// Match downsampling's stream-based input, whose length is initially unknown.
			try (var input = new FileInputStream(directory.resolve(tile + ".webp").toFile())) {
				BufferedImage decoded = ImageIO.read(input);
				assertNotNull(decoded);
				assertEquals(source.getWidth(), decoded.getWidth());
				assertEquals(source.getHeight(), decoded.getHeight());
				for (int y = 0; y < source.getHeight(); y++) {
					for (int x = 0; x < source.getWidth(); x++) {
						assertEquals(source.getRGB(x, y) >>> 24, decoded.getRGB(x, y) >>> 24,
								"Alpha at tile " + tile + ", pixel " + x + "," + y);
					}
				}
			}
		}
	}

	private static BufferedImage tileImage(int imageType, int tile) {
		BufferedImage image = new BufferedImage(64, 64, imageType);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int alpha = (x + y * 64 + tile) & 255;
				int red = (x * 7 + tile * 13) & 255;
				int green = (y * 11 + tile * 17) & 255;
				int blue = ((x + y) * 3 + tile * 19) & 255;
				image.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
			}
		}
		return image;
	}
}
