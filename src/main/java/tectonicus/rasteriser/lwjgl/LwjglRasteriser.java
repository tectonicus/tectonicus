/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_RGBA16;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

public class LwjglRasteriser implements Rasteriser
{
	private final DisplayType type;
	
	private long window = 0;
	
	
	private int width, height;
	
	private Map<Integer, Integer> keyCodeMap;
	
	private Map<Integer, Boolean> prevKeyStates;
	
	private long prevMillis;
	
	public LwjglRasteriser(DisplayType type, final int displayWidth, final int displayHeight, final int colourDepth, final int alphaBits, final int depthBits, final int numSamples) throws Exception
	{
		this.type = type;
		
		this.width = displayWidth;
		this.height = displayHeight;
		
		keyCodeMap = new HashMap<>();
		
		keyCodeMap.put(KeyEvent.VK_0, GLFW.GLFW_KEY_0);
		keyCodeMap.put(KeyEvent.VK_1, GLFW.GLFW_KEY_1);
		keyCodeMap.put(KeyEvent.VK_2, GLFW.GLFW_KEY_2);
		keyCodeMap.put(KeyEvent.VK_3, GLFW.GLFW_KEY_3);
		keyCodeMap.put(KeyEvent.VK_4, GLFW.GLFW_KEY_4);
		keyCodeMap.put(KeyEvent.VK_5, GLFW.GLFW_KEY_5);
		keyCodeMap.put(KeyEvent.VK_6, GLFW.GLFW_KEY_6);
		keyCodeMap.put(KeyEvent.VK_7, GLFW.GLFW_KEY_7);
		keyCodeMap.put(KeyEvent.VK_8, GLFW.GLFW_KEY_8);
		keyCodeMap.put(KeyEvent.VK_9, GLFW.GLFW_KEY_9);
		
		keyCodeMap.put(KeyEvent.VK_UP, GLFW.GLFW_KEY_UP);
		keyCodeMap.put(KeyEvent.VK_DOWN, GLFW.GLFW_KEY_DOWN);
		keyCodeMap.put(KeyEvent.VK_LEFT, GLFW.GLFW_KEY_LEFT);
		keyCodeMap.put(KeyEvent.VK_RIGHT, GLFW.GLFW_KEY_RIGHT);
		
		keyCodeMap.put(KeyEvent.VK_SPACE, GLFW.GLFW_KEY_SPACE);
		keyCodeMap.put(KeyEvent.VK_MINUS, GLFW.GLFW_KEY_MINUS);
		keyCodeMap.put(KeyEvent.VK_EQUALS, GLFW.GLFW_KEY_EQUAL);
		keyCodeMap.put(KeyEvent.VK_BACK_SPACE, GLFW.GLFW_KEY_BACKSPACE);
		
		keyCodeMap.put(KeyEvent.VK_A, GLFW.GLFW_KEY_A);
		keyCodeMap.put(KeyEvent.VK_B, GLFW.GLFW_KEY_B);
		keyCodeMap.put(KeyEvent.VK_C, GLFW.GLFW_KEY_C);
		keyCodeMap.put(KeyEvent.VK_D, GLFW.GLFW_KEY_D);
		keyCodeMap.put(KeyEvent.VK_E, GLFW.GLFW_KEY_E);
		keyCodeMap.put(KeyEvent.VK_F, GLFW.GLFW_KEY_F);
		keyCodeMap.put(KeyEvent.VK_G, GLFW.GLFW_KEY_G);
		keyCodeMap.put(KeyEvent.VK_H, GLFW.GLFW_KEY_H);
		keyCodeMap.put(KeyEvent.VK_I, GLFW.GLFW_KEY_I);
		keyCodeMap.put(KeyEvent.VK_J, GLFW.GLFW_KEY_J);
		keyCodeMap.put(KeyEvent.VK_K, GLFW.GLFW_KEY_K);
		keyCodeMap.put(KeyEvent.VK_L, GLFW.GLFW_KEY_L);
		keyCodeMap.put(KeyEvent.VK_M, GLFW.GLFW_KEY_M);
		keyCodeMap.put(KeyEvent.VK_N, GLFW.GLFW_KEY_N);
		keyCodeMap.put(KeyEvent.VK_O, GLFW.GLFW_KEY_O);
		keyCodeMap.put(KeyEvent.VK_P, GLFW.GLFW_KEY_P);
		keyCodeMap.put(KeyEvent.VK_Q, GLFW.GLFW_KEY_Q);
		keyCodeMap.put(KeyEvent.VK_R, GLFW.GLFW_KEY_R);
		keyCodeMap.put(KeyEvent.VK_S, GLFW.GLFW_KEY_S);
		keyCodeMap.put(KeyEvent.VK_T, GLFW.GLFW_KEY_T);
		keyCodeMap.put(KeyEvent.VK_U, GLFW.GLFW_KEY_U);
		keyCodeMap.put(KeyEvent.VK_V, GLFW.GLFW_KEY_V);
		keyCodeMap.put(KeyEvent.VK_W, GLFW.GLFW_KEY_W);
		keyCodeMap.put(KeyEvent.VK_X, GLFW.GLFW_KEY_X);
		keyCodeMap.put(KeyEvent.VK_Y, GLFW.GLFW_KEY_Y);
		keyCodeMap.put(KeyEvent.VK_Z, GLFW.GLFW_KEY_Z);
		
		prevKeyStates = new HashMap<>();
		
		// Make a list of pixel formats to try (in preferance order)
		ArrayList<LwjglPixelFormat> pixelFormats = new ArrayList<>();
		
		// As requested
		pixelFormats.add( new LwjglPixelFormat(colourDepth, alphaBits, depthBits, 0, numSamples) );
		
		// No anti-aliasing
		pixelFormats.add( new LwjglPixelFormat(colourDepth, alphaBits, depthBits, 0, 0) );
		
		// No anti-aliasing or alpha buffer
		pixelFormats.add( new LwjglPixelFormat(colourDepth, 0, depthBits, 0, 0) );
		
		// No anti-aliasing, no alpha buffer, 16bit colour
		pixelFormats.add( new LwjglPixelFormat(16, 0, depthBits, 0, 0) );
		
		// No anti-aliasing, no alpha buffer, 16bit colour, 16bit depth
		pixelFormats.add( new LwjglPixelFormat(16, 0, 16, 0, 0) );
		
		// Ugh. Anything with a depth buffer.
		pixelFormats.add( new LwjglPixelFormat(0, 0, 1, 0, 0) );


		GLFW.glfwSetErrorCallback((arg0, arg1) -> System.out.println("GLFW error: " + String.format("0x%08X", arg0)));
		
		if (!GLFW.glfwInit()) {
			throw new RuntimeException("Failed to init GLFW");
		}

		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);

		if (type == DisplayType.Offscreen) {
			GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
			window = GLFW.glfwCreateWindow(displayWidth, displayHeight, "", 0, 0);
		} else if (type == DisplayType.Window) {
			GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);

			for (LwjglPixelFormat pf : pixelFormats) {
				GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, pf.depth);
				GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, pf.stencil);
				GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, pf.alpha);
				GLFW.glfwWindowHint(GLFW.GLFW_RED_BITS, pf.bpp);
				GLFW.glfwWindowHint(GLFW.GLFW_GREEN_BITS, pf.bpp);
				GLFW.glfwWindowHint(GLFW.GLFW_BLUE_BITS, pf.bpp);
				GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, pf.samples);
				window = GLFW.glfwCreateWindow(displayWidth, displayHeight, "Tectonicus", 0, 0);
				if(window != 0) {
					break;
				}
			}
		} else {
			throw new RuntimeException("Unknown display type: "+type);
		}
		
		if(window == 0) {
		    throw new RuntimeException("Failed to create window!");
		}
		
		GLFW.glfwMakeContextCurrent(window);
		GL.createCapabilities();

		if (type == DisplayType.Offscreen) {
			int fbo = glGenFramebuffers();
			glBindFramebuffer(GL_FRAMEBUFFER, fbo);
			int rbo = glGenRenderbuffers();
			int rboDepth = glGenRenderbuffers();
			glBindRenderbuffer(GL_RENDERBUFFER, rbo);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA16, displayWidth, displayHeight);
			glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rbo);
			glBindRenderbuffer(GL_RENDERBUFFER, rboDepth);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, displayWidth, displayHeight);
			glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboDepth);
		}
		
		System.out.println("\tdisplay created ok");
	}
	
	@Override
	public void destroy()
	{
		GLFW.glfwDestroyWindow(window);
		GLFW.glfwTerminate();
	}
	
	@Override
	public void printInfo()
	{
		System.out.println(" -- Lwjgl Rasteriser -- ");
		System.out.println("\tLWJGL version: " + Version.getVersion());
		System.out.println("\ttype: "+type);
		System.out.println("\twidth: "+width);
		System.out.println("\theigth: "+height);
		
		System.out.println("\tOpenGL Vendor: "+GL11.glGetString(GL11.GL_VENDOR));
		System.out.println("\tOpenGL Renderer: "+GL11.glGetString(GL11.GL_RENDERER));
		System.out.println("\tOpenGL Version: "+GL11.glGetString(GL11.GL_VERSION));
		
	//	System.out.println();
		
	//	GL11.glGetString(GL11.GL_EXTENSIONS);
	}

	@Override
	public long getWindowId() {
		return window;
	}
	
	@Override
	public void sync()
	{
		long currMillis = System.currentTimeMillis();
		long delta = currMillis - prevMillis;
		long fps = 1000 / (delta > 0 ? delta : 1);
		if (fps > 60) {
			return;
		}
		prevMillis = currMillis;
		
		prevKeyStates.clear();
		for (Integer i : keyCodeMap.values())
		{
			prevKeyStates.put(i, GLFW.glfwGetKey(window, i) == GLFW.GLFW_PRESS);
		}
		
		GLFW.glfwPollEvents();
		GLFW.glfwSwapBuffers(window);
	}
	
	@Override
	public boolean isCloseRequested()
	{
		return GLFW.glfwWindowShouldClose(window);
	}
	
	@Override
	public void setViewport(final int x, final int y, final int width, final int height)
	{
		GL11.glViewport(x, y, width, height);
	}
	
	@Override
	public boolean isKeyDown(final int vkKey)
	{
		if (!keyCodeMap.containsKey(vkKey))
			throw new RuntimeException("No mapping for :"+vkKey);
		
		Integer lwjglKey = keyCodeMap.get(vkKey);
		return GLFW.glfwGetKey(window, lwjglKey) == GLFW.GLFW_PRESS;
	}
	
	public boolean isKeyJustDown(final int vkKey)
	{
		if (!keyCodeMap.containsKey(vkKey))
			throw new RuntimeException("No mapping for :"+vkKey);
		
		Integer lwjglKey = keyCodeMap.get(vkKey);
		
		return GLFW.glfwGetKey(window, lwjglKey) == GLFW.GLFW_PRESS && !prevKeyStates.get(lwjglKey);
	}
	
	public int getDisplayWidth()
	{
		return width;
	}
	
	public int getDisplayHeight()
	{
		return height;
	}
	
	public Texture createTexture(BufferedImage image, TextureFilter filter)
	{
		final int id = LwjglTextureUtils.createTexture(image, filter);
		Texture texture = new LwjglTexture(id, image.getWidth(), image.getHeight());
		return texture;
	}
	
	public Texture createTexture(BufferedImage[] mips, TextureFilter filter)
	{
		final int id = LwjglTextureUtils.createTexture(mips, filter);
		Texture texture = new LwjglTexture(id, mips[0].getWidth(), mips[0].getHeight());
		return texture;
	}
	
	public Mesh createMesh(Texture texture)
	{
		return new LwjglMesh((LwjglTexture)texture);
	}
	
	@Override
	public void beginFrame()
	{
		
	}
	
	@Override
	public void resetState()
	{
		GL11.glColorMask(true, true, true, true);
		
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glFrontFace(GL11.GL_CW);

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
	}
	
	@Override
	public void clear(Color clearColour)
	{
		GL11.glClearColor(clearColour.getRed()/255.0f, clearColour.getGreen()/255.0f, clearColour.getBlue()/255.0f, 0);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
	}
	
	@Override
	public void clearDepthBuffer()
	{
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
	}
	
	// Slow, simple version. Pull framebuffer and convert to BufferedImage via setRGB()
	public BufferedImage takeScreenshot2(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat)
	{
		ByteBuffer screenContentsBytes = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN); 
		IntBuffer screenContents = screenContentsBytes.asIntBuffer();
		
		GL11.glReadPixels(startX, startY, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenContents);
		
		int[] pixels = new int[width * height];
		screenContents.get(pixels);
		
		final int pixelFormat = imageFormat.hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
		BufferedImage img = new BufferedImage(width, height, pixelFormat);
		for (int x=0; x<width; x++)
		{
			for (int y=0; y<height; y++)
			{
				final int rgba = pixels[x + y * width];
				
				img.setRGB(x, height - 1 - y, rgba);
			}
		}
		
		return img;
	}
	
	public BufferedImage takeScreenshot(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat)
	{
		BufferedImage img = null;
		
		ByteBuffer screenContentsBytes = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN); 
		IntBuffer screenContents = screenContentsBytes.asIntBuffer();
		
		if (imageFormat.hasAlpha())
		{
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			int[] pixels = ((DataBufferInt)(img.getRaster().getDataBuffer())).getData();
			
			GL11.glReadPixels(startX, startY, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenContents);
			
			for (int y=startY; y<startY+height; y++)
			{
				screenContents.position(y*width);
				screenContents.get(pixels, (height-y-1)*width, width);
			}
		}
		else
		{
			img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			byte[] pixels = ((DataBufferByte)(img.getRaster().getDataBuffer())).getData();
			
			GL11.glReadPixels(startX, startY, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, screenContents);
			
			for (int y=startY; y<startY+height; y++)
			{
				screenContentsBytes.position(y*width*3);
				screenContentsBytes.get(pixels, (height-y-1)*width*3, width*3);
			}
		}
		
		return img;
	}
	
	public void bindTexture(Texture texture)
	{
		LwjglTexture tex = (LwjglTexture)texture;
		
		if (tex != null)
		{
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getId());
		}
		else
		{
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}
	}
	
	public void beginShape(PrimativeType type)
	{
		int glType = GL11.GL_TRIANGLES;
		switch (type)
		{
			case Points:
				glType = GL11.GL_POINTS;
				break;
			case Lines:
				glType = GL11.GL_LINES;
				break;
			case Triangles:
				glType = GL11.GL_TRIANGLES;
				break;
			case Quads:
				glType = GL11.GL_QUADS;
				break;
			default:
				assert false;
		}
		GL11.glBegin(glType);
	}
	public void colour(final float r, final float g, final float b, final float a)
	{
		GL11.glColor4f(r, g, b, a);
	}
	@Override
	public void texCoord(final float u, final float v)
	{
		GL11.glTexCoord2f(u, v);
	}
	public void vertex(final float x, final float y, final float z)
	{
		GL11.glVertex3f(x, y, z);
	}
	public void endShape()
	{
		GL11.glEnd();
	}
	
	public void setProjectionMatrix(Matrix4f matrix)
	{
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix.get(buffer);
		GL11.glLoadMatrixf(buffer);
	}
	
	public void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up)
	{
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix.get(buffer);
		GL11.glLoadMatrixf(buffer);
	}
	
	@Override
	public void enableBlending(final boolean enable)
	{
		if (enable)
			GL11.glEnable(GL11.GL_BLEND);
		else
			GL11.glDisable(GL11.GL_BLEND);
	}
	
	@Override
	public void enableDepthTest(boolean enable)
	{
		if (enable)
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		else
			GL11.glDisable(GL11.GL_DEPTH_TEST);
	}
	
	@Override
	public void enableAlphaTest(boolean enable)
	{
		if (enable)
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		else
			GL11.glDisable(GL11.GL_ALPHA_TEST);	
	}
	
	@Override
	public void enableColourWriting(final boolean colourMask, final boolean alphaMask)
	{
		GL11.glColorMask(colourMask, colourMask, colourMask, alphaMask);
	}
	
	@Override
	public void enableDepthWriting(final boolean enable)
	{
		GL11.glDepthMask(enable);
	}
	
	@Override
	public void setBlendFunc(BlendFunc func)
	{
		switch (func)
		{
			case Regular:
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				break;
			case Additive:
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				break;
			default:
				assert false;
		}
	}
	
	@Override
	public void setAlphaFunc(AlphaFunc func, final float refValue)
	{
		switch (func)
		{
			case Greater:
				GL11.glAlphaFunc(GL11.GL_GREATER, refValue);
			break;
			case GreaterOrEqual:
				GL11.glAlphaFunc(GL11.GL_GEQUAL, refValue);
			break;
			case Equal:
				GL11.glAlphaFunc(GL11.GL_EQUAL, refValue);
			break;
			case Less:
				GL11.glAlphaFunc(GL11.GL_LESS, refValue);
			break;
			case LessOrEqual:
				GL11.glAlphaFunc(GL11.GL_LEQUAL, refValue);
			break;
			default:
				assert false;
		}
	}
}
