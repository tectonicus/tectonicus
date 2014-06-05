/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.configuration.ImageFormat;

public interface Rasteriser
{
	public void destroy();
	
	public void printInfo();
	
	// Windowing access
	public void sync();
	public boolean isCloseRequested();
	public boolean isKeyDown(final int vkKey);
	public boolean isKeyJustDown(final int vkKey);
	
	public int getDisplayWidth();
	public int getDisplayHeight();
	
	public void setViewport(final int x, final int y, final int width, final int height);
	
	public void beginFrame();
	
	public void resetState();
	public void clear(Color clearColour);
	public void clearDepthBuffer();

	/** Captures a portion of the current display and return it as a buffered image */
	public BufferedImage takeScreenshot(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat);
	
	// Texturing
	public Texture createTexture(BufferedImage image, TextureFilter filter);
	public Texture createTexture(BufferedImage[] mips, TextureFilter filter);
	
	public void bindTexture(Texture texture);
	
	public Mesh createMesh(Texture texture);
	
	// Matricies
	public void setProjectionMatrix(Matrix4f matrix);
	public void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up);
	
//	public void pushMatrix();
//	public void popMatrix();
//	public void loadIdentityMatrix();
	
//	public void selectCameraMatrix(); // hmmm?
//	public void selectWorldMatrix();
	
	// Immediate mode
	public void beginShape(PrimativeType type);
	public void colour(final float r, final float g, final float b, final float a);
	public void texCoord(final float u, final float v);
	public void vertex(final float x, final float y, final float z);
	public void endShape();
	
	// Draw state
	public void enableBlending(final boolean enable);
	public void enableDepthTest(final boolean enable);
	public void enableAlphaTest(final boolean enable);
	public void enableColourWriting(final boolean colourMask, final boolean alphaMask);
	public void enableDepthWriting(final boolean enable);
	public void setBlendFunc(BlendFunc func);
	public void setAlphaFunc(AlphaFunc func, final float refValue);
		
}
