/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Locale;

/** Vector overlays the viewer draws over the tiles of one dimension. Every overlay is off unless configured. */
@Getter
@Setter
public class OverlayConfig {
	public static final int DEFAULT_ELEVATION = 64;

	public enum Visibility {
		OFF, HIDDEN, SHOWN;

		public boolean isEnabled() {
			return this != OFF;
		}

		/** Accepts off/false, hidden, and on/true/shown. Anything else is reported by the caller. */
		public static Visibility parse(String value, Visibility defaultValue) {
			if (value == null || value.isBlank())
				return defaultValue;
			return switch (value.trim().toLowerCase(Locale.ROOT)) {
				case "off", "false", "none" -> OFF;
				case "hidden" -> HIDDEN;
				case "on", "true", "shown", "visible" -> SHOWN;
				default -> throw new IllegalArgumentException(value);
			};
		}
	}

	private Visibility chunkGrid = Visibility.OFF;
	private Visibility regionGrid = Visibility.OFF;
	private Visibility slimeChunks = Visibility.OFF;
	private Visibility worldBorder = Visibility.OFF;
	private Visibility features = Visibility.OFF;
	/** JSON file of lines, polygons and markers in Minecraft coordinates. */
	private File featuresFile;
	/** Block height at which flat overlays are drawn. */
	private int elevation = DEFAULT_ELEVATION;

	public boolean isEnabled() {
		return chunkGrid.isEnabled() || regionGrid.isEnabled() || slimeChunks.isEnabled() || worldBorder.isEnabled()
				|| (features.isEnabled() && featuresFile != null);
	}
}
