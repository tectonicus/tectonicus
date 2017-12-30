/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.awt.Color;

import tectonicus.configuration.Configuration.RenderStyle;

public interface Layer
{
	public String getId();
	
	public String getName();
	
	public String getMapId();
	
	public LightStyle getLightStyle();
	
	public RenderStyle getRenderStyle();
	
	public ImageFormat getImageFormat();
	public float getImageCompressionLevel();
	public String getBackgroundColor();
	public Color getBackgroundColorRGB();
	
	public String getCustomBlockConfig();
	public boolean useDefaultBlockConfig();
}
