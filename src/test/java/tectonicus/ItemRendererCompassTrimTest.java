/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemRendererCompassTrimTest {
	@Test
	void trimsToVisiblePixelsWithPadding() {
		BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(30, 40, 0xFF000000);
		image.setRGB(90, 80, 0x80FFFFFF);

		BufferedImage trimmed = ItemRenderer.trimTransparentMargin(image, 2);

		assertEquals(65, trimmed.getWidth());
		assertEquals(45, trimmed.getHeight());
		assertEquals(0xFF000000, trimmed.getRGB(2, 2));
		assertEquals(0x80FFFFFF, trimmed.getRGB(62, 42));
	}

	@Test
	void paddingStopsAtTheImageEdge() {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0xFF000000);
		image.setRGB(15, 15, 0xFF000000);

		BufferedImage trimmed = ItemRenderer.trimTransparentMargin(image, 2);

		assertEquals(16, trimmed.getWidth());
		assertEquals(16, trimmed.getHeight());
	}

	@Test
	void fullyTransparentImageBecomesOnePixel() {
		BufferedImage trimmed = ItemRenderer.trimTransparentMargin(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), 2);

		assertEquals(1, trimmed.getWidth());
		assertEquals(1, trimmed.getHeight());
	}
}
