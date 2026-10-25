/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class BannerTest {
	@ParameterizedTest
	@ValueSource(ints = {64, 128})
	void modernBaseRetainsClothShadingAtDifferentTextureResolutions(int resolution) {
		BufferedImage base = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);
		base.setRGB(5, 5, 0xff808080);
		base.setRGB(5, 10, 0xffcccccc);
		BufferedImage result = tint(base, new Color(200, 100, 50));

		assertEquals(new Color(100, 50, 25).getRGB(), result.getRGB(5, 5));
		assertEquals(new Color(160, 80, 40).getRGB(), result.getRGB(5, 10));
		assertEquals(0, result.getRGB(0, 0));
	}

	@Test
	void whiteDyePreservesOriginalTextureColors() {
		BufferedImage pattern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		pattern.setRGB(5, 5, 0xffb4c8dc);
		assertEquals(0xffb4c8dc, tint(pattern, Color.WHITE).getRGB(5, 5));
	}

	@Test
	void layeredPatternsPreserveBaseThroughTransparentAreasAndBlendPartialAlpha() {
		BufferedImage base = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		base.setRGB(5, 5, 0xff808080);
		base.setRGB(5, 10, 0xff808080);
		base.setRGB(5, 15, 0xff808080);
		BufferedImage result = tint(base, new Color(200, 100, 50));
		BufferedImage pattern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		pattern.setRGB(5, 10, 0xff808080);
		pattern.setRGB(5, 15, 0x80808080);
		Graphics2D graphics = result.createGraphics();
		try {
			Banner.addPattern(pattern, new Color(20, 80, 200), graphics);
		} finally {
			graphics.dispose();
		}

		assertEquals(new Color(100, 50, 25).getRGB(), result.getRGB(5, 5));
		assertEquals(new Color(10, 40, 100).getRGB(), result.getRGB(5, 10));
		Color blended = new Color(result.getRGB(5, 15), true);
		assertEquals(55, blended.getRed(), 1);
		assertEquals(45, blended.getGreen(), 1);
		assertEquals(63, blended.getBlue(), 1);
		assertEquals(255, blended.getAlpha());
	}

	@Test
	void sharedPatternsCanBeRenderedWithDifferentDyesWithoutLosingShading() {
		BufferedImage pattern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		pattern.setRGB(5, 5, 0xff808080);
		pattern.setRGB(5, 10, 0x80808080);
		int[] original = pattern.getRGB(0, 0, 64, 64, null, 0, 64);
		BufferedImage red = tint(pattern, Color.RED);
		BufferedImage blue = tint(pattern, Color.BLUE);

		assertEquals(0xff800000, red.getRGB(5, 5));
		assertEquals(0xff000080, blue.getRGB(5, 5));
		assertEquals(128, blue.getRGB(5, 10) >>> 24);
		assertArrayEquals(original, pattern.getRGB(0, 0, 64, 64, null, 0, 64));
		assertArrayEquals(red.getRGB(0, 0, 64, 64, null, 0, 64),
				tint(pattern, Color.RED).getRGB(0, 0, 64, 64, null, 0, 64));
	}

	private BufferedImage tint(BufferedImage pattern, Color color) {
		BufferedImage result = new BufferedImage(pattern.getWidth(), pattern.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics();
		try {
			Banner.addPattern(pattern, color, graphics);
		} finally {
			graphics.dispose();
		}
		return result;
	}
}
