/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;


public enum TextureFilter {
	/**
	 * Linear texture filtering
	 */
	LINEAR,
	
	/**
	 * Nearest texture filtering, for that retro, pixelated look
	 */
	NEAREST;
	
	public static TextureFilter fromString(String str) {
		if (str.equalsIgnoreCase("nearest"))
			return NEAREST;
		
		return LINEAR;
	}
}
