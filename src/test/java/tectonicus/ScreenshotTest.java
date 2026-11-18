/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.configuration.ImageFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotTest {
	@Test
	void writesOpaqueAbgrTileAsRgbPng(@TempDir Path dir) throws Exception {
		BufferedImage tile = pattern(BufferedImage.TYPE_4BYTE_ABGR, false);
		File file = dir.resolve("tile.png").toFile();

		assertTrue(Screenshot.write(file, tile, ImageFormat.PNG, 1.0f));

		BufferedImage read = ImageIO.read(file);
		assertFalse(read.getColorModel().hasAlpha());
		assertSamePixels(tile, read);
	}

	@Test
	void keepsAlphaWhenAnyPixelIsTranslucent(@TempDir Path dir) throws Exception {
		BufferedImage tile = pattern(BufferedImage.TYPE_4BYTE_ABGR, true);
		File file = dir.resolve("tile.png").toFile();

		assertTrue(Screenshot.write(file, tile, ImageFormat.PNG, 1.0f));

		BufferedImage read = ImageIO.read(file);
		assertTrue(read.getColorModel().hasAlpha());
		assertSamePixels(tile, read);
	}

	@Test
	void dropsAlphaFromOpaqueIntArgb() {
		BufferedImage image = pattern(BufferedImage.TYPE_INT_ARGB, false);

		BufferedImage rgb = Screenshot.dropAlphaIfOpaque(image);

		assertEquals(BufferedImage.TYPE_INT_RGB, rgb.getType());
		assertSamePixels(image, rgb);
	}

	@Test
	void leavesTranslucentAndOtherImageTypesUnchanged() {
		BufferedImage translucent = pattern(BufferedImage.TYPE_INT_ARGB, true);
		BufferedImage rgb = pattern(BufferedImage.TYPE_3BYTE_BGR, false);

		assertSame(translucent, Screenshot.dropAlphaIfOpaque(translucent));
		assertSame(rgb, Screenshot.dropAlphaIfOpaque(rgb));
	}

	private static BufferedImage pattern(int type, boolean translucentCorner) {
		BufferedImage image = new BufferedImage(16, 16, type);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				image.setRGB(x, y, 0xff000000 | (x * 16) << 16 | (y * 16) << 8 | (x ^ y) * 16);
			}
		}
		if (translucentCorner)
			image.setRGB(0, 0, 0x80336699);
		return image;
	}

	private static void assertSamePixels(BufferedImage expected, BufferedImage actual) {
		for (int y = 0; y < expected.getHeight(); y++) {
			for (int x = 0; x < expected.getWidth(); x++) {
				assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "pixel " + x + "," + y);
			}
		}
	}
}
