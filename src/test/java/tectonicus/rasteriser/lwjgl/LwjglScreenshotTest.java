/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.PendingScreenshot;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LwjglScreenshotTest {
	@Test
	void flipsTightlyPackedRowsForWidthsThatAreNotMultiplesOfFour() {
		int width = 5, height = 3, rowLength = width * 3;
		ByteBuffer source = ByteBuffer.allocate(rowLength * height);
		for (int row = 0; row < height; row++)
			for (int i = 0; i < rowLength; i++)
				source.put((byte) row);
		byte[] dest = new byte[rowLength * height];
		LwjglRasteriser.copyRowsFlipped(source, dest, rowLength, height);
		for (int row = 0; row < height; row++) {
			byte[] expected = new byte[rowLength];
			java.util.Arrays.fill(expected, (byte) (height - 1 - row));
			assertArrayEquals(expected, java.util.Arrays.copyOfRange(dest, row * rowLength, (row + 1) * rowLength));
		}
	}

	@Test
	void screenshotBufferIsReusedWhileLargeEnough() {
		ByteBuffer first = LwjglRasteriser.reuseBuffer(null, 64);
		assertTrue(first.isDirect());
		assertEquals(64, first.limit());

		first.position(10);
		ByteBuffer smaller = LwjglRasteriser.reuseBuffer(first, 48);
		assertSame(first, smaller);
		assertEquals(0, smaller.position());
		assertEquals(48, smaller.limit());

		ByteBuffer larger = LwjglRasteriser.reuseBuffer(smaller, 100);
		assertNotSame(first, larger);
		assertEquals(100, larger.limit());
		assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, larger.order());
	}

	@Test
	void opaqueScreenshotsWithOddWidthsAreNotSkewed() throws Exception {
		Rasteriser rasteriser;
		try {
			rasteriser = RasteriserFactory.createRasteriser(RasteriserType.LWJGL, DisplayType.OFFSCREEN, 64, 64, 24, 8, 24, 0);
		} catch (Throwable e) {
			rasteriser = null;
		}
		assumeTrue(rasteriser != null, "No OpenGL context available");
		try {
			rasteriser.setViewport(0, 0, 64, 64);
			rasteriser.clear(Color.BLACK);
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			GL11.glScissor(0, 0, 1, 64);
			rasteriser.clear(Color.RED);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);

			for (int width : new int[] {13, 14, 15, 16}) {
				BufferedImage image = rasteriser.takeScreenshot(0, 0, width, 10, ImageFormat.JPG);
				for (int y = 0; y < image.getHeight(); y++) {
					assertEquals(0xFF0000, image.getRGB(0, y) & 0xFFFFFF, "width " + width + " row " + y + " left column");
					assertEquals(0x000000, image.getRGB(1, y) & 0xFFFFFF, "width " + width + " row " + y + " second column");
				}
			}
		} finally {
			rasteriser.destroy();
		}
	}

	@Test
	void pendingScreenshotsKeepThePixelsFromWhenTheyWereStarted() {
		Rasteriser rasteriser;
		try {
			rasteriser = RasteriserFactory.createRasteriser(RasteriserType.LWJGL, DisplayType.OFFSCREEN, 64, 64, 24, 8, 24, 0);
		} catch (Throwable e) {
			rasteriser = null;
		}
		assumeTrue(rasteriser != null, "No OpenGL context available");
		try {
			rasteriser.setViewport(0, 0, 64, 64);
			for (ImageFormat format : new ImageFormat[] {ImageFormat.JPG, ImageFormat.PNG}) {
				Color[] colours = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN};
				PendingScreenshot[] pending = new PendingScreenshot[colours.length];
				BufferedImage[] expected = new BufferedImage[colours.length];
				for (int i = 0; i < colours.length; i++) {
					// A different picture each time: the frame colour plus a marker column whose position changes.
					rasteriser.clear(colours[i]);
					GL11.glEnable(GL11.GL_SCISSOR_TEST);
					GL11.glScissor(i, 0, 1, 64);
					rasteriser.clear(Color.WHITE);
					GL11.glDisable(GL11.GL_SCISSOR_TEST);

					expected[i] = rasteriser.takeScreenshot(0, 0, 13, 10, format);
					pending[i] = rasteriser.startScreenshot(0, 0, 13, 10, format);
					// Collect only every other capture straight away; the rest wait while later frames are drawn,
					// including captures that are older than the number of buffers in use.
					if (i % 2 == 1)
						assertImagesEqual(expected[i], pending[i].get(), format + " capture " + i);
				}
				rasteriser.clear(Color.BLACK);
				for (int i = 0; i < colours.length; i++) {
					BufferedImage image = pending[i].get();
					assertImagesEqual(expected[i], image, format + " capture " + i);
					assertEquals(colours[i].getRGB() & 0xFFFFFF, image.getRGB(12, 5) & 0xFFFFFF, format + " capture " + i);
					assertEquals(0xFFFFFF, image.getRGB(i, 5) & 0xFFFFFF, format + " capture " + i + " marker");
					assertSame(image, pending[i].get(), "repeated get returns the same image");
				}
			}
		} finally {
			rasteriser.destroy();
		}
	}

	private static void assertImagesEqual(BufferedImage expected, BufferedImage actual, String context) {
		assertEquals(expected.getType(), actual.getType(), context);
		assertEquals(expected.getWidth(), actual.getWidth(), context);
		assertEquals(expected.getHeight(), actual.getHeight(), context);
		for (int y = 0; y < expected.getHeight(); y++)
			for (int x = 0; x < expected.getWidth(); x++)
				assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), context + " pixel " + x + "," + y);
	}
}
