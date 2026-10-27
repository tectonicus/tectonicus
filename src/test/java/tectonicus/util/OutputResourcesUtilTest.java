/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.util;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputResourcesUtilTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8})
	void exportsCustomGrayscaleAlphaImagesAtRequestedSize(int size) throws Exception {
		ComponentColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				new int[] {8, 8}, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		BufferedImage source = new BufferedImage(colorModel,
				Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 4, 4, 2, null), false, null);
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				source.getRaster().setSample(x, y, 0, y < 2 ? 255 : 0);
				source.getRaster().setSample(x, y, 1, x < 2 ? 255 : y < 2 ? 128 : 0);
			}
		}
		Path input = directory.resolve("font.png");
		ImageIO.write(source, "png", input.toFile());
		BufferedImage font = ImageIO.read(input.toFile());
		assertEquals(BufferedImage.TYPE_CUSTOM, font.getType());
		Path output = directory.resolve("exported.png");

		OutputResourcesUtil.writeImage(font, size, size, output.toFile());

		assertTrue(Files.isRegularFile(output), "The font must be exported even when its color model is custom");
		BufferedImage exported = ImageIO.read(output.toFile());
		assertNotNull(exported);
		assertEquals(size, exported.getWidth());
		assertEquals(size, exported.getHeight());
		assertEquals(0xffffffff, exported.getRGB(0, 0));
		assertEquals(0x80ffffff, exported.getRGB(size - 1, 0));
		assertEquals(0xff000000, exported.getRGB(0, size - 1));
		assertEquals(0, exported.getRGB(size - 1, size - 1) >>> 24);
	}
}
