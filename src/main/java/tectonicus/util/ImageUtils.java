/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.image.BufferedImage;

@UtilityClass
public class ImageUtils {

	/** Makes a copy of the input image. Also converts to INT_ARGB so that we're always
	 *  working in the same colour space.
	 */
	public static BufferedImage copy(BufferedImage in) {
		if (in == null)
			return null;

		BufferedImage img = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		copy(img, in, 0, 0);

		return img;
	}

	public static void copy(BufferedImage dest, BufferedImage src, int destX, int destY) {
		for (int x=0; x<src.getWidth(); x++) {
			for (int y=0; y<src.getHeight(); y++) {
				final int rgb = src.getRGB(x, y);
				dest.setRGB(destX + x, destY + y, rgb);
			}
		}
	}

	public BufferedImage convertSimpleTransparencyToARGB(BufferedImage image, Color transparentColor) {
		BufferedImage newImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				int pixel = image.getRGB(j, i);
				if (pixel == transparentColor.getRGB()){
					newImg.setRGB(j, i, 0);
				} else {
					newImg.setRGB(j, i, pixel);
				}
			}
		}

		return newImg;
	}

	public void normalizeAlpha(BufferedImage image) {
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				int pixel = image.getRGB(x, y);
				int alpha = (pixel & 0xff000000) >>> 24;

				if (alpha < 25.5) {
					pixel &= 0x00ffffff; // set alpha to 0
				} else if (alpha >= 252) { //Kind of hacky
					pixel |= (255 << 24);
				}
				image.setRGB(x, y, pixel);
			}
		}
	}

	public Opacity testOpacity(BufferedImage image){
		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				if (isTranslucent(image, j, i)) {
					return Opacity.TRANSLUCENT;
				} else if (isTransparent(image, j, i)) {
					return Opacity.TRANSPARENT;
				}
			}
		}
		return Opacity.OPAQUE;
	}

	private boolean isTranslucent(BufferedImage image, int x, int y ) {
		int pixel = image.getRGB(x, y);
		int alpha = (pixel & 0xff000000) >>> 24;
		return alpha > 0 && alpha < 255;
	}

	private boolean isTransparent(BufferedImage image, int x, int y ) {
		int pixel = image.getRGB(x, y);
		return (pixel & 0xff000000) >>> 24 == 0;
	}

	public enum Opacity {
		OPAQUE,
		TRANSPARENT,
		TRANSLUCENT
	}
}
