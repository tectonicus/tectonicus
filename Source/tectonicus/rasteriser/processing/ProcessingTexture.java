/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.processing;

import java.awt.image.BufferedImage;

import processing.core.PImage;

import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;

public class ProcessingTexture implements Texture
{
	private PImage image;

	public ProcessingTexture(BufferedImage image, TextureFilter filter)
	{
		this.image = new PImage(image);
		
		// TODO: Pay attention to filter?
	}
	
	public PImage getPImage()
	{
		return image;
	}
}
