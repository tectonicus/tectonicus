/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import lombok.extern.slf4j.Slf4j;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.rasteriser.lwjgl.LwjglRasteriser;

@Slf4j
public class RasteriserFactory {
	
	public enum DisplayType {
		WINDOW,
		OFFSCREEN,
		OFFSCREEN_EGL
	}
	
	public static Rasteriser createRasteriser(RasteriserType type, DisplayType displayType, final int displayWidth, final int displayHeight, final int colourDepth, final int alphaBits, final int depthBits, final int numSamples) {
		Rasteriser result = null;
		
		//We only have one rasterizer type at the moment, there were others meant for software rendering but they were never finished and have been removed
		if (type == RasteriserType.LWJGL) {
			try {
				result = new LwjglRasteriser(displayType, displayWidth, displayHeight, colourDepth, alphaBits, depthBits, numSamples);
			} catch (Exception e) {
				log.error("Exception: ", e);
			}
		}
		
		return result;
	}
}
