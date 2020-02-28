package tectonicus.rasteriser.lwjgl;

import org.lwjgl.opengl.GLX;
import org.lwjgl.opengl.GLX13;
import org.lwjgl.opengl.WGLARBPbuffer;
import org.lwjgl.opengl.WGLARBPixelFormat;
import org.lwjgl.system.Platform;

public class LwjglPixelFormat {
	public int bpp;
	public int alpha;
	public int depth;
	public int stencil; 
	public int samples;
	public LwjglPixelFormat(int bpp, int alpha, int depth, int stencil, int samples) {
		super();
		this.bpp = bpp;
		this.alpha = alpha;
		this.depth = depth;
		this.stencil = stencil;
		this.samples = samples;
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
					bpp,
					WGLARBPixelFormat.WGL_GREEN_BITS_ARB,
					bpp,
					WGLARBPixelFormat.WGL_BLUE_BITS_ARB,
					bpp,
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
					bpp,
					GLX.GLX_GREEN_SIZE,
					bpp,
					GLX.GLX_BLUE_SIZE,
					bpp,
					GLX.GLX_ALPHA_SIZE,
					alpha,
					GLX.GLX_STENCIL_SIZE,
					stencil				
			};			
		}
		return attribs;
	}
	public float[] getAttribFloatList() {
		return new float[] {
				0				
		};
	}
}
