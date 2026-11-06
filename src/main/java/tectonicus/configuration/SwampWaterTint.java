/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.configuration;

/** Historical multipliers for blue water textures; modern textures use biome colors. */
public enum SwampWaterTint {
	AUTO(0xE0FFAE), NONE(0xFFFFFF), MINECRAFT10(0xE0FF70), LEGACY(0xE0FFAE);

	private final int rgb;
	SwampWaterTint(int rgb) { this.rgb = rgb; }
	public int getRgb() { return rgb; }
}
