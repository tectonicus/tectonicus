/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

public class MutableViewConfig implements ViewConfig
{
	private ImageFormat imageFormat;
	private float imageCompressionLevel;
	
	private int viewDistance;
	
	public MutableViewConfig()
	{
		this.imageFormat = ImageFormat.Png;
		this.imageCompressionLevel = 0.95f;
		this.viewDistance = 100;
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
	
	public void setImageCompressionLevel(final float compression)
	{
		this.imageCompressionLevel = compression;
	}
	
	@Override
	public int getViewDistance()
	{
		return viewDistance;
	}
	
	public void setViewDistance(final int distance)
	{
		this.viewDistance = distance;
	}
}
