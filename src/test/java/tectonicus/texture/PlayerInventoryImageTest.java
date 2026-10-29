/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.texture;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerInventoryImageTest {
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void preservesTextureResolutionAndAlignsEquipmentStorageAndHotbar(int scale) {
		BufferedImage sheet = new BufferedImage(256 * scale, 256 * scale, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < sheet.getHeight(); y++)
			for (int x = 0; x < sheet.getWidth(); x++)
				sheet.setRGB(x, y, 0xff000000 | x << 12 | y);

		for (boolean includeEnderChest : new boolean[]{false, true}) {
			BufferedImage gui = TexturePack.composePlayerInventoryImage(sheet, includeEnderChest);
			assertEquals(176 * scale, gui.getWidth());
			assertEquals((includeEnderChest ? 200 : 132) * scale, gui.getHeight());
			int armorY = includeEnderChest ? 85 : 17;
			int storageY = includeEnderChest ? 117 : 49;
			int armorSourceY = includeEnderChest ? 139 : 17;
			assertPixel(sheet, 7, armorSourceY, gui, 7, armorY, scale);
			assertPixel(sheet, 151, armorSourceY, gui, 151, armorY, scale);
			assertPixel(sheet, 79, 125, gui, 79, armorY, scale);
			assertPixel(sheet, 7, 139, gui, 7, storageY, scale);
			assertPixel(sheet, 151, 175, gui, 151, storageY + 36, scale);
			assertPixel(sheet, 7, 197, gui, 7, storageY + 58, scale);
			assertPixel(sheet, 0, 221, gui, 0, gui.getHeight() / scale - 1, scale);
			if (includeEnderChest) {
				assertPixel(sheet, 7, 17, gui, 7, 17, scale);
				assertPixel(sheet, 151, 53, gui, 151, 53, scale);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2})
	void equipmentGapRetainsTransparentResourcePackBackground(int scale) {
		BufferedImage sheet = new BufferedImage(256 * scale, 256 * scale, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < sheet.getHeight(); y++)
			for (int x = 0; x < sheet.getWidth(); x++)
				sheet.setRGB(x, y, y >= 125 * scale && y < 139 * scale ? 0x00000000 : 0xffabcdef);
		BufferedImage gui = TexturePack.composePlayerInventoryImage(sheet, true);
		assertEquals(0x00000000, gui.getRGB(80 * scale, 90 * scale));
		assertEquals(0xffabcdef, gui.getRGB(8 * scale, 90 * scale));
	}

	private void assertPixel(BufferedImage source, int sourceX, int sourceY, BufferedImage target, int targetX, int targetY, int scale) {
		assertEquals(source.getRGB(sourceX * scale, sourceY * scale), target.getRGB(targetX * scale, targetY * scale));
	}
}
