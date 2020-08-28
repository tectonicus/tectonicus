/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;

import java.awt.image.BufferedImage;

@UtilityClass
public class ImageUtils {

	public boolean hasAlphaChannel(BufferedImage image) {
		return image.getColorModel().hasAlpha();
	}

	public boolean containsTransparency(BufferedImage image){
		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				if (isTransparent(image, j, i)){
					return true;
				}
			}
		}
		return false;
	}

	private boolean isTransparent(BufferedImage image, int x, int y ) {
		int pixel = image.getRGB(x, y);
		return (pixel>>24) == 0x00;
	}
}
