/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

public class MutableViewConfig implements ViewConfig
{
	public static final int DEFAULT_WIDTH = 1920;
	public static final int DEFAULT_HEIGHT = 1080;
        
	private ImageFormat imageFormat;
	private float imageCompressionLevel;
	
	private int viewDistance;
	private int fov;
        
        private int width;
        private int height;
	
	public MutableViewConfig()
	{
		this.imageFormat = ImageFormat.PNG;
		this.imageCompressionLevel = 0.75f;
		this.viewDistance = 100;
		this.fov = 70;
                this.width = DEFAULT_WIDTH;
                this.height = DEFAULT_HEIGHT;
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
	
	@Override
	public int getFOV()
	{
		return fov;
	}
	
	@Override
	public void setFOV(int fov) 
	{
		this.fov = fov;
	}

        @Override
        public int getWidth() {
                return width;
        }
        
        public void setWidth(int width) {
                this.width = width;
        }

        @Override
        public int getHeight() {
                return height;
        }

        public void setHeight(int height) {
                this.height = height;
        }
}
