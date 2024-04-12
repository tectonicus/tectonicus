/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.awt.Color;

import tectonicus.configuration.Configuration.RenderStyle;

public class MutableLayer implements Layer
{
	private final String id;
	private final String mapId;
	
	private String name;
	private LightStyle lightStyle;
	private RenderStyle renderStyle;
	
	private ImageFormat imageFormat;
	private float imageCompressionLevel;
	private String backgroundColor;
	
	private String customBlockConfig;
	private boolean useDefaultBlockConfig;
	
	public MutableLayer(String id, String mapId)
	{
		this.id = id;
		this.mapId = mapId;
		this.name = "Unnamed layer";
		this.lightStyle = LightStyle.Day;
		this.renderStyle = RenderStyle.REGULAR;
		this.imageFormat = ImageFormat.PNG;
		this.imageCompressionLevel = 1.0f;
		this.useDefaultBlockConfig = true;
	}
	
	@Override
	public String getId()
	{
		return id;
	}
	
	@Override
	public String getMapId()
	{
		return mapId;
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	public void setName(final String name)
	{
		this.name = name;
	}

	@Override
	public LightStyle getLightStyle()
	{
		return lightStyle;
	}
	
	public void setLightStyle(final LightStyle style)
	{
		this.lightStyle = style;
	}

	@Override
	public RenderStyle getRenderStyle()
	{
		return renderStyle;
	}
	
	public void setRenderStyle(final RenderStyle style)
	{
		this.renderStyle = style;
	}
	
	
	@Override
	public ImageFormat getImageFormat()
	{
		return imageFormat;
	}
	public void setImageFormat(ImageFormat format)
	{
		this.imageFormat = format;
	}
	
	@Override
	public float getImageCompressionLevel()
	{
		return imageCompressionLevel;
	}
	public void setImageCompressionLevel(final float level)
	{
		this.imageCompressionLevel = level;
	}
	
	@Override
	public String getCustomBlockConfig()
	{
		return customBlockConfig;		
	}
	public void setCustomBlockConfig(String config)
	{
		this.customBlockConfig = config;
	}
	
	@Override
	public boolean useDefaultBlockConfig()
	{
		return useDefaultBlockConfig;
	}
	public void setUseDefaultBlockConfig(final boolean use)
	{
		this.useDefaultBlockConfig = use;
	}

	@Override
	public Color getBackgroundColorRGB() {
		return Color.decode(backgroundColor);
	}
	
	@Override
	public String getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
}
