/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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

	/**
	 * Starts capturing a portion of the current display without waiting for the GPU to finish drawing it.
	 * The image is collected later with {@link PendingScreenshot#get()}, so the caller can prepare the next
	 * frame in the meantime. Later drawing does not change the captured pixels.
	 */
	default PendingScreenshot startScreenshot(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat) {
		BufferedImage image = takeScreenshot(startX, startY, width, height, imageFormat);
		return () -> image;
	}
	
	// Texturing
	default int getMaxTextureSize() { return Integer.MAX_VALUE; }

	/** Dedicated video memory in bytes as reported by the driver, or -1 when it is not known. */
	default long getVideoMemoryBytes() { return -1; }

	/** Video memory currently free, as reported by the driver, or -1 when it is not known. */
	default long getAvailableVideoMemoryBytes() { return -1; }

	/** How many times the driver has moved data out of video memory, or -1 when it is not known. */
	default long getVideoMemoryEvictionCount() { return -1; }

	/**
	 * Limits the video memory reserved for mesh vertices. Meshes that do not fit are drawn from system memory.
	 * Memory already reserved is kept for reuse.
	 */
	default void setVertexMemoryLimit(long bytes) {}

	/** Video memory reserved for mesh vertices, or -1 when not tracked. */
	default long getVertexMemoryReservedBytes() { return -1; }

	/** Part of the reserved vertex memory in use by meshes, or -1 when not tracked. */
	default long getVertexMemoryUsedBytes() { return -1; }

	/** How many meshes did not fit in the vertex memory and are drawn from system memory, or -1 when not tracked. */
	default long getVertexMemoryFailedAllocations() { return -1; }

	/** Meshes whose vertices are currently on the GPU, or -1 when not tracked. */
	default long getLiveVertexBufferCount() { return -1; }

	/** Bytes of vertex data currently on the GPU, or -1 when not tracked. */
	default long getLiveVertexBufferBytes() { return -1; }

	Texture createTexture(BufferedImage image, TextureFilter filter);
	
	Texture createTexture(BufferedImage[] mips, TextureFilter filter);

	void destroyTexture(Texture texture);
	
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
