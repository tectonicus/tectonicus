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
