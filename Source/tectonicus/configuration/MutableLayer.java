/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.configuration;

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
	
	private String customBlockConfig;
	private boolean useDefaultBlockConfig;
	
	public MutableLayer(String id, String mapId)
	{
		this.id = id;
		this.mapId = mapId;
		this.name = "Unnamed layer";
		this.lightStyle = LightStyle.Day;
		this.renderStyle = RenderStyle.Regular;
		this.imageFormat = ImageFormat.Png;
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
}
