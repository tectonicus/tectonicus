/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tectonicus.configuration.ImageFormat;

import java.awt.Color;
import java.awt.image.BufferedImage;

public interface Rasteriser {
	void destroy();
	
	void printInfo();
	
	// Windowing access
	void sync();
	
	boolean isCloseRequested();
	
	boolean isKeyDown(final int vkKey);
	
	boolean isKeyJustDown(final int vkKey);
	
	long getWindowId();
	
	int getDisplayWidth();
	
	int getDisplayHeight();
	
	void setViewport(final int x, final int y, final int width, final int height);
	
	void beginFrame();
	
	void resetState();
	
	void clear(Color clearColour);
	
	void clearDepthBuffer();
	
	/**
	 * Captures a portion of the current display and return it as a buffered image
	 */
	BufferedImage takeScreenshot(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat);
	
	// Texturing
	Texture createTexture(BufferedImage image, TextureFilter filter);
	
	Texture createTexture(BufferedImage[] mips, TextureFilter filter);
	
	void bindTexture(Texture texture);
	
	Mesh createMesh(Texture texture);
	
	// Matrices
	void setProjectionMatrix(Matrix4f matrix);
	
	void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up);

//	void pushMatrix();
//	void popMatrix();
//	void loadIdentityMatrix();

//	void selectCameraMatrix(); // hmmm?
//	void selectWorldMatrix();
	
	// Immediate mode
	void beginShape(PrimitiveType type);
	
	void colour(final float r, final float g, final float b, final float a);
	
	void texCoord(final float u, final float v);
	
	void vertex(final float x, final float y, final float z);
	
	void endShape();
	
	// Draw state
	void enableBlending(final boolean enable);
	
	void enableDepthTest(final boolean enable);
	
	void enableAlphaTest(final boolean enable);
	
	void enableColourWriting(final boolean colourMask, final boolean alphaMask);
	
	void enableDepthWriting(final boolean enable);
	
	void setBlendFunc(BlendFunc func);
	
	void setAlphaFunc(AlphaFunc func, final float refValue);
}
