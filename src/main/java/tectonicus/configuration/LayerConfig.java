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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import tectonicus.configuration.Configuration.RenderStyle;

import java.awt.Color;

@Getter
@Setter
@RequiredArgsConstructor
public class LayerConfig extends RenderOptions {
	private final String id;
	private final String dimensionId;
	private WorldConfig world;
	private String name = "Unnamed layer";
	private LightStyle lightStyle = LightStyle.Day;
	private RenderStyle renderStyle = RenderStyle.NORMAL;
	private ImageFormat imageFormat = ImageFormat.PNG;
	private float imageCompressionLevel = 1.0f;
	private String backgroundColor = "#e5e3df";
	private String customBlockConfig;
	private boolean useDefaultBlockConfig = true;

	public String getWorldId() {
		return world != null ? world.getId() : dimensionId;
	}

	public String getDirectoryName() {
		String prefix = getWorldId() + ".";
		return id.startsWith(prefix) ? id.substring(prefix.length()) : id;
	}

	public boolean useDefaultBlockConfig() {
		return useDefaultBlockConfig;
	}

	public Color getBackgroundColorRGB() {
		return Color.decode(backgroundColor);
	}
}
