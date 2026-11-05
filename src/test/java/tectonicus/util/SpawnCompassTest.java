/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.util;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.texture.ZipStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SpawnCompassTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(ints = {16, 32, 64})
	void legacyAtlasGetsANeedleWhileRetainingIndexedDialAndTransparency(int size) throws Exception {
		byte[] colors = {0, 64};
		IndexColorModel palette = new IndexColorModel(8, 2, colors, colors, colors, new byte[]{0, (byte) 255});
		BufferedImage atlas = new BufferedImage(size * 16, size * 16, BufferedImage.TYPE_BYTE_INDEXED, palette);
		for (int y = 4 * size / 16; y < 12 * size / 16; y++)
			for (int x = 4 * size / 16; x < 12 * size / 16; x++)
				atlas.getRaster().setSample(6 * size + x, 3 * size + y, 0, 1);
		BufferedImage icon = export("gui/items.png", atlas);
		assertEquals(32, icon.getWidth());
		assertEquals(32, icon.getHeight());
		assertEquals(0xffff1414, icon.getRGB(16, 10), "Red needle must survive the indexed source palette");
		assertEquals(0xff646464, icon.getRGB(16, 16), "The needle has a gray tail");
		assertEquals(0xff404040, icon.getRGB(10, 18), "The dial remains unchanged");
		assertEquals(0, icon.getRGB(0, 0) >>> 24, "The exterior stays transparent");
	}

	@ParameterizedTest
	@ValueSource(strings = {"textures/items/compass.png", "assets/minecraft/textures/items/compass.png",
			"assets/minecraft/textures/items/compass_16.png", "assets/minecraft/textures/item/compass_16.png"})
	void newerCompassTexturesKeepTheirExistingNeedle(String path) throws Exception {
		boolean animation = path.endsWith("/compass.png");
		BufferedImage source = new BufferedImage(16, animation ? 32 : 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
			for (int x = 0; x < 16; x++)
				source.setRGB(x, y, y < 16 ? 0xff123456 : 0xffabcdef);
		BufferedImage icon = export(path, source);
		assertEquals(0xff123456, icon.getRGB(16, 10), "Do not draw another needle on newer sprites");
		assertEquals(0xff123456, icon.getRGB(0, 31), "Animation strips export only the first frame");
	}

	private BufferedImage export(String path, BufferedImage image) throws Exception {
		Path jar = directory.resolve("client.jar");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
			out.putNextEntry(new ZipEntry(path));
			ImageIO.write(image, "png", out);
			out.closeEntry();
		}
		ZipStack stack = new ZipStack(jar.toFile(), null, List.of());
		try {
			Path output = directory.resolve("SpawnCompass.png");
			OutputResourcesUtil.outputSpawnCompass(stack, output.toFile());
			assertTrue(Files.isRegularFile(output));
			return ImageIO.read(output.toFile());
		} finally {
			var field = ZipStack.class.getDeclaredField("base");
			field.setAccessible(true);
			((FileSystem) field.get(stack)).close();
		}
	}
}
