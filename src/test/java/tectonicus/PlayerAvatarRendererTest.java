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

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerAvatarRendererTest {
	private static final int SKIN = 0xffc89670;
	private static final int RED = 0xffff0000;
	private static final int GREEN = 0xff00ff00;
	private static final int BLUE = 0xff0000ff;

	@Test
	void legacySkinsMirrorTheRightLimbsAndKeepAnOpaqueBase() {
		var skin = solid(64, 32, SKIN);
		skin.setRGB(4, 20, RED); // outer column of the right leg's front
		skin.setRGB(40, 20, 0); // a hole in the base layer
		BufferedImage texture = PlayerAvatarRenderer.normalize(skin);

		assertEquals(64, texture.getHeight());
		// The left leg's front (20, 52) is the right leg's front mirrored.
		assertEquals(RED, texture.getRGB(23, 52));
		assertEquals(0xff000000, texture.getRGB(40, 20) & 0xff000000);
		// An old skin's hat without any transparency is hidden.
		assertEquals(0, texture.getRGB(40, 8) >>> 24);
	}

	@Test
	void rejectsImagesWithoutSkinProportions() {
		assertNull(PlayerAvatarRenderer.renderBody(solid(16, 16, SKIN), false));
		assertNull(PlayerAvatarRenderer.renderBody(solid(64, 48, SKIN), false));
	}

	@Test
	void detectsSlimSkinsFromTheirUnusedArmColumns() {
		var classic = solid(64, 64, SKIN);
		var slim = solid(64, 64, SKIN);
		for (int y = 20; y < 32; y++) {
			slim.setRGB(54, y, 0);
			slim.setRGB(55, y, 0);
		}
		assertFalse(PlayerAvatarRenderer.looksSlim(classic));
		assertTrue(PlayerAvatarRenderer.looksSlim(slim));
	}

	@Test
	void drawsEachLimbFromItsOwnTextureOnTheRightSide() {
		var skin = baseOnly();
		fillFront(skin, 0, 16, 4, RED); // right leg
		fillFront(skin, 16, 48, 4, GREEN); // left leg
		BufferedImage body = PlayerAvatarRenderer.renderBody(skin, false);

		// The player's right side is on the viewer's left.
		assertTrue(meanX(body, RED) < meanX(body, GREEN));
	}

	@Test
	void slimArmsAreNarrowerButUseTheSameCanvas() {
		var skin = baseOnly();
		fillFront(skin, 40, 16, 4, BLUE);
		BufferedImage classic = PlayerAvatarRenderer.renderBody(skin, false);
		var slimSkin = baseOnly();
		fillFront(slimSkin, 40, 16, 3, BLUE);
		BufferedImage slim = PlayerAvatarRenderer.renderBody(slimSkin, true);

		assertEquals(classic.getWidth(), slim.getWidth());
		assertEquals(classic.getHeight(), slim.getHeight());
		assertTrue(count(slim, BLUE) < count(classic, BLUE) * 0.9);
	}

	@Test
	void drawsTheOuterLayerOverTheBaseLayer() {
		var skin = solid(64, 64, SKIN);
		for (int y = 32; y < 48; y++)
			for (int x = 16; x < 40; x++)
				skin.setRGB(x, y, 0);
		fillFront(skin, 16, 32, 8, RED, 12); // jacket front
		assertTrue(count(PlayerAvatarRenderer.renderBody(skin, false), RED) > 0);

		var withoutJacket = solid(64, 64, SKIN);
		for (int y = 32; y < 48; y++)
			for (int x = 16; x < 40; x++)
				withoutJacket.setRGB(x, y, 0);
		assertEquals(0, count(PlayerAvatarRenderer.renderBody(withoutJacket, false), RED));
	}

	@Test
	void headIconIsSquareAndKeepsTheLitTopColour() {
		BufferedImage head = PlayerAvatarRenderer.renderHead(solid(64, 64, SKIN), 32);
		assertEquals(32, head.getWidth());
		assertEquals(32, head.getHeight());
		boolean litTop = false;
		for (int y = 0; y < head.getHeight(); y++)
			for (int x = 0; x < head.getWidth(); x++)
				litTop |= head.getRGB(x, y) == SKIN;
		assertTrue(litTop);
	}

	private static BufferedImage solid(int width, int height, int color) {
		var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				image.setRGB(x, y, color);
		return image;
	}

	/** A plain skin with an empty outer layer, so the base layer shows. */
	private static BufferedImage baseOnly() {
		var skin = solid(64, 64, SKIN);
		int[][] overlays = {{32, 0, 64, 16}, {0, 32, 56, 48}, {0, 48, 16, 64}, {48, 48, 64, 64}};
		for (int[] area : overlays)
			for (int y = area[1]; y < area[3]; y++)
				for (int x = area[0]; x < area[2]; x++)
					skin.setRGB(x, y, 0);
		return skin;
	}

	private static void fillFront(BufferedImage skin, int u, int v, int width, int color) {
		fillFront(skin, u, v, width, color, 12);
	}

	/** Fills the front face of a box whose texture starts at (u, v) and that is 4 pixels deep. */
	private static void fillFront(BufferedImage skin, int u, int v, int width, int color, int height) {
		for (int y = v + 4; y < v + 4 + height; y++)
			for (int x = u + 4; x < u + 4 + width; x++)
				skin.setRGB(x, y, color);
	}

	/** Counts fully covered pixels whose colour is {@code color} after lighting (any brightness). */
	private static int count(BufferedImage image, int color) {
		int n = 0;
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				if (sameHue(image.getRGB(x, y), color)) n++;
		return n;
	}

	private static double meanX(BufferedImage image, int color) {
		long sum = 0;
		int n = 0;
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				if (sameHue(image.getRGB(x, y), color)) {
					sum += x;
					n++;
				}
		return (double) sum / n;
	}

	/** The colour at some brightness: shading scales all channels, so the zero channels stay zero. */
	private static boolean sameHue(int argb, int color) {
		if ((argb >>> 24) != 0xff) return false;
		for (int shift = 0; shift <= 16; shift += 8) {
			int pixel = (argb >> shift) & 0xff, wanted = (color >> shift) & 0xff;
			if ((pixel == 0) != (wanted == 0)) return false;
			if (wanted != 0 && pixel < 0x40) return false;
		}
		return true;
	}
}
