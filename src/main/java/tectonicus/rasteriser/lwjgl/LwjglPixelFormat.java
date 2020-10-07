/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import lombok.Getter;
import org.lwjgl.opengl.GLX;
import org.lwjgl.opengl.GLX13;
import org.lwjgl.opengl.WGLARBPbuffer;
import org.lwjgl.opengl.WGLARBPixelFormat;
import org.lwjgl.system.Platform;

import static org.lwjgl.egl.EGL10.EGL_BLUE_SIZE;
import static org.lwjgl.egl.EGL10.EGL_DEPTH_SIZE;
import static org.lwjgl.egl.EGL10.EGL_GREEN_SIZE;
import static org.lwjgl.egl.EGL10.EGL_NONE;
import static org.lwjgl.egl.EGL10.EGL_PBUFFER_BIT;
import static org.lwjgl.egl.EGL10.EGL_RED_SIZE;
import static org.lwjgl.egl.EGL10.EGL_STENCIL_SIZE;
import static org.lwjgl.egl.EGL10.EGL_SURFACE_TYPE;
import static org.lwjgl.egl.EGL12.EGL_RENDERABLE_TYPE;
import static org.lwjgl.egl.EGL14.EGL_OPENGL_BIT;

@Getter
public class LwjglPixelFormat {
	private final int bitsPerPixel;
	private final int bitsPerColor;
	private final int alpha;
	private final int depth;
	private final int stencil;
	private final int samples;

	public LwjglPixelFormat(int bitsPerPixel, int alpha, int depth, int stencil, int samples) {
		this.bitsPerPixel = bitsPerPixel;
		this.alpha = alpha;
		this.depth = depth;
		this.stencil = stencil;
		this.samples = samples;
		this.bitsPerColor = convertToBPC(bitsPerPixel);
	}
	
	public int[] getAttribIntList() {
		int[] attribs;
		if (Platform.get() == Platform.WINDOWS) {
			attribs = new int[] {
					WGLARBPbuffer.WGL_DRAW_TO_PBUFFER_ARB,
					1,
					WGLARBPixelFormat.WGL_DEPTH_BITS_ARB,
					depth,
					WGLARBPixelFormat.WGL_RED_BITS_ARB,
					bitsPerColor,
					WGLARBPixelFormat.WGL_GREEN_BITS_ARB,
					bitsPerColor,
					WGLARBPixelFormat.WGL_BLUE_BITS_ARB,
					bitsPerColor,
					WGLARBPixelFormat.WGL_ALPHA_BITS_ARB,
					alpha,
					WGLARBPixelFormat.WGL_STENCIL_BITS_ARB,
					stencil				
			};
		} else {
			attribs = new int[] {
					GLX13.GLX_RENDER_TYPE,
					GLX13.GLX_RGBA_BIT,
					GLX13.GLX_DRAWABLE_TYPE,
					GLX13.GLX_PBUFFER_BIT,
					GLX.GLX_DEPTH_SIZE,
					depth,
					GLX.GLX_RED_SIZE,
					bitsPerColor,
					GLX.GLX_GREEN_SIZE,
					bitsPerColor,
					GLX.GLX_BLUE_SIZE,
					bitsPerColor,
					GLX.GLX_ALPHA_SIZE,
					alpha,
					GLX.GLX_STENCIL_SIZE,
					stencil				
			};			
		}
		return attribs;
	}

	public int[] getEGLAttributes() {
		return new int[] {
				EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
				EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
				EGL_BLUE_SIZE, bitsPerColor,
				EGL_GREEN_SIZE, bitsPerColor,
				EGL_RED_SIZE, bitsPerColor,
				EGL_DEPTH_SIZE, depth,
				EGL_STENCIL_SIZE, 8,
				EGL_NONE};
	}

	private int convertToBPC(int bpp) {
		int bpc;
		switch (bpp) {
			case 0:
				bpc = 0;
				break;
			case 32:
			case 24:
				bpc = 8;
				break;
			case 16:
			default:
				bpc = 4;
				break;
		}
		return bpc;
	}
}
