/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import lombok.extern.log4j.Log4j2;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.Configuration;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.util.OsDetect;

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
import java.util.List;
import java.util.Map;

import static org.lwjgl.egl.EGL10.EGL_NONE;
import static org.lwjgl.egl.EGL10.EGL_NO_CONTEXT;
import static org.lwjgl.egl.EGL10.EGL_NO_SURFACE;
import static org.lwjgl.egl.EGL10.EGL_VENDOR;
import static org.lwjgl.egl.EGL10.EGL_VERSION;
import static org.lwjgl.egl.EGL10.eglChooseConfig;
import static org.lwjgl.egl.EGL10.eglCreateContext;
import static org.lwjgl.egl.EGL10.eglGetError;
import static org.lwjgl.egl.EGL10.eglInitialize;
import static org.lwjgl.egl.EGL10.eglMakeCurrent;
import static org.lwjgl.egl.EGL10.eglQueryString;
import static org.lwjgl.egl.EGL10.eglTerminate;
import static org.lwjgl.egl.EGL12.EGL_CLIENT_APIS;
import static org.lwjgl.egl.EGL12.eglBindAPI;
import static org.lwjgl.egl.EGL12.eglReleaseThread;
import static org.lwjgl.egl.EGL14.EGL_DEFAULT_DISPLAY;
import static org.lwjgl.egl.EGL14.EGL_OPENGL_API;
import static org.lwjgl.egl.EGL14.eglGetDisplay;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_MAJOR_VERSION;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_MINOR_VERSION;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT;
import static org.lwjgl.egl.EGL15.EGL_CONTEXT_OPENGL_PROFILE_MASK;
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_OSMESA_CONTEXT_API;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_RENDERBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindRenderbufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glFramebufferRenderbufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenFramebuffersEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenRenderbuffersEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glRenderbufferStorageEXT;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_RGBA16;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.glReadPixels;
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

@Log4j2
public class LwjglRasteriser implements Rasteriser
{
	private final DisplayType type;
	
	private long window = 0;
	private long eglDisplay = 0;

	private final int width, height;
	
	private final Map<Integer, Integer> keyCodeMap;
	
	private final Map<Integer, Boolean> prevKeyStates;
	
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
		
		// Make a list of pixel formats to try (in preference order)
		List<LwjglPixelFormat> pixelFormats = new ArrayList<>();
		
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

//		checkOpenGLCompatability(3, 1);
		boolean isMac = OsDetect.isMac();

		if(type == DisplayType.WINDOW || type == DisplayType.OFFSCREEN) {
			if (isMac) {
				Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
			}

			log.debug("GLFW version: {}", GLFW::glfwGetVersionString);
			glfwSetErrorCallback((error, description) ->
					log.error("GLFW error [{}]: {}", String.format("0x%08X", error), GLFWErrorCallback.getDescription(description)));

			if (!glfwInit()) {
				throw new RuntimeException("Failed to init GLFW");
			}

			if (isMac) {
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2);
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
			} else {
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
				glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
			}
		}

		if (type == DisplayType.OFFSCREEN) {
			glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
			window = glfwCreateWindow(displayWidth, displayHeight, "", 0, 0);
		} else if (type == DisplayType.WINDOW) {
			glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);

			for (LwjglPixelFormat pf : pixelFormats) {
				glfwWindowHint(GLFW.GLFW_DEPTH_BITS, pf.getDepth());
				glfwWindowHint(GLFW.GLFW_STENCIL_BITS, pf.getStencil());
				glfwWindowHint(GLFW.GLFW_ALPHA_BITS, pf.getAlpha());
				glfwWindowHint(GLFW.GLFW_RED_BITS, pf.getBitsPerColor());
				glfwWindowHint(GLFW.GLFW_GREEN_BITS, pf.getBitsPerColor());
				glfwWindowHint(GLFW.GLFW_BLUE_BITS, pf.getBitsPerColor());
				glfwWindowHint(GLFW.GLFW_SAMPLES, pf.getSamples());
				window = glfwCreateWindow(displayWidth, displayHeight, "Tectonicus", 0, 0);
				if(window != 0) {
					break;
				}
			}
		} else if (type == DisplayType.OFFSCREEN_EGL) {
			eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
			IntBuffer major = BufferUtils.createIntBuffer(1);
			IntBuffer minor = BufferUtils.createIntBuffer(1);
			eglInitialize(eglDisplay, major, minor);
			log.debug("Using EGL for context creation.");

			int[] configCount = new int[1];

			int[] attributes = pixelFormats.get(0).getEGLAttributes();

			//TODO: we should be smarter about choosing config here see https://github.com/LWJGL/lwjgl3/issues/336
			if (!eglChooseConfig(eglDisplay, attributes, null, configCount)) {
				throw new RuntimeException("EGL error: " + eglGetError());
			}

			PointerBuffer configs = BufferUtils.createPointerBuffer(configCount[0]);

			eglChooseConfig(eglDisplay, attributes, configs, configCount);
			eglBindAPI(EGL_OPENGL_API);

			int[] contextAttributes = {
					EGL_CONTEXT_MAJOR_VERSION, 3,
					EGL_CONTEXT_MINOR_VERSION, 0,
					EGL_CONTEXT_OPENGL_PROFILE_MASK, EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT,
					EGL_NONE
			};

			long context = eglCreateContext(eglDisplay, configs.get(0), EGL_NO_CONTEXT, contextAttributes);

			if (context == 0) {
				throw new RuntimeException("Failed to create EGL context!");
			}

			eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, context);
		} else {
			throw new RuntimeException("Unknown display type: "+type);
		}

		if (type != DisplayType.OFFSCREEN_EGL) {
			if(window == 0) {
				throw new RuntimeException("Failed to create GLFW window!");
			}

			glfwMakeContextCurrent(window);
		}

		GL.createCapabilities();

		if (!isMac && (type == DisplayType.OFFSCREEN || type == DisplayType.OFFSCREEN_EGL)) {
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
		} else if (isMac && type == DisplayType.OFFSCREEN){
			int fbo = glGenFramebuffersEXT();
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbo);
			int rbo = glGenRenderbuffersEXT();
			int rboDepth = glGenRenderbuffersEXT();
			glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, rbo);
			glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_RGBA16, displayWidth, displayHeight);
			glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_RENDERBUFFER_EXT, rbo);
			glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, rboDepth);
			glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH_COMPONENT24, displayWidth, displayHeight);
			glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, rboDepth);
		}
		
		log.info("\tdisplay created ok");
	}
	
	@Override
	public void destroy()
	{
		if (type == DisplayType.OFFSCREEN_EGL) {
			eglReleaseThread();
			eglTerminate(eglDisplay);
		} else {
			if (OsDetect.isMac()) {
				glfwPollEvents();
			}
			glfwTerminate();
		}
	}
	
	@Override
	public void printInfo()
	{
		log.debug(" -- Lwjgl Rasteriser -- ");
		log.debug("\tLWJGL version: {}", Version.getVersion());
		log.debug("\ttype: {}", type);
		log.debug("\twidth: {}", width);
		log.debug("\theight: {}", height);
		if (type == DisplayType.OFFSCREEN_EGL) {
			log.debug("\tEGL version: {}", eglQueryString(eglDisplay, EGL_VERSION));
			log.debug("\tEGL vendor: {}", eglQueryString(eglDisplay, EGL_VENDOR));
			log.debug("\tSupported client apis: {}", eglQueryString(eglDisplay, EGL_CLIENT_APIS));
		}
		log.debug("\tOpenGL Vendor: {}", glGetString(GL_VENDOR));
		log.debug("\tOpenGL Renderer: {}", glGetString(GL_RENDERER));
		log.debug("\tOpenGL Version: {}", glGetString(GL_VERSION));
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
			prevKeyStates.put(i, glfwGetKey(window, i) == GLFW.GLFW_PRESS);
		}
		
		glfwPollEvents();
		glfwSwapBuffers(window);
	}
	
	@Override
	public boolean isCloseRequested()
	{
		return glfwWindowShouldClose(window);
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
		return glfwGetKey(window, lwjglKey) == GLFW.GLFW_PRESS;
	}
	
	public boolean isKeyJustDown(final int vkKey)
	{
		if (!keyCodeMap.containsKey(vkKey))
			throw new RuntimeException("No mapping for :"+vkKey);
		
		Integer lwjglKey = keyCodeMap.get(vkKey);
		
		return glfwGetKey(window, lwjglKey) == GLFW.GLFW_PRESS && !prevKeyStates.get(lwjglKey);
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
		return new LwjglTexture(id, image.getWidth(), image.getHeight());
	}
	
	public Texture createTexture(BufferedImage[] mips, TextureFilter filter)
	{
		final int id = LwjglTextureUtils.createTexture(mips, filter);
		return new LwjglTexture(id, mips[0].getWidth(), mips[0].getHeight());
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
		glColorMask(true, true, true, true);
		
		glDepthFunc(GL11.GL_LEQUAL);
		
		glEnable(GL11.GL_DEPTH_TEST);
		glEnable(GL11.GL_CULL_FACE);
		glCullFace(GL11.GL_BACK);
		glFrontFace(GL11.GL_CW);

		glDisable(GL11.GL_BLEND);
		glDisable(GL11.GL_ALPHA_TEST);
	}
	
	@Override
	public void clear(Color clearColour)
	{
		glClearColor(clearColour.getRed()/255.0f, clearColour.getGreen()/255.0f, clearColour.getBlue()/255.0f, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
	}
	
	@Override
	public void clearDepthBuffer()
	{
		glClear(GL_DEPTH_BUFFER_BIT);
	}
	
	// Slow, simple version. Pull framebuffer and convert to BufferedImage via setRGB()
	public BufferedImage takeScreenshot2(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat)
	{
		ByteBuffer screenContentsBytes = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN); 
		IntBuffer screenContents = screenContentsBytes.asIntBuffer();
		
		glReadPixels(startX, startY, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenContents);
		
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
			
			glReadPixels(startX, startY, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenContents);
			
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
			
			glReadPixels(startX, startY, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, screenContents);
			
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
			glBindTexture(GL_TEXTURE_2D, tex.getId());
		}
		else
		{
			glBindTexture(GL_TEXTURE_2D, 0);
		}
	}
	
	public void beginShape(PrimativeType type)
	{
		int glType = GL_TRIANGLES;
		switch (type)
		{
			case Points:
				glType = GL_POINTS;
				break;
			case Lines:
				glType = GL_LINES;
				break;
			case Triangles:
				glType = GL_TRIANGLES;
				break;
			case Quads:
				glType = GL_QUADS;
				break;
			default:
				assert false;
		}
		glBegin(glType);
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
			glEnable(GL11.GL_BLEND);
		else
			glDisable(GL11.GL_BLEND);
	}
	
	@Override
	public void enableDepthTest(boolean enable)
	{
		if (enable)
			glEnable(GL11.GL_DEPTH_TEST);
		else
			glDisable(GL11.GL_DEPTH_TEST);
	}
	
	@Override
	public void enableAlphaTest(boolean enable)
	{
		if (enable)
			glEnable(GL11.GL_ALPHA_TEST);
		else
			glDisable(GL11.GL_ALPHA_TEST);
	}
	
	@Override
	public void enableColourWriting(final boolean colourMask, final boolean alphaMask)
	{
		glColorMask(colourMask, colourMask, colourMask, alphaMask);
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
				glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				break;
			case Additive:
				glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
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
				glAlphaFunc(GL11.GL_GREATER, refValue);
			break;
			case GreaterOrEqual:
				glAlphaFunc(GL11.GL_GEQUAL, refValue);
			break;
			case Equal:
				glAlphaFunc(GL11.GL_EQUAL, refValue);
			break;
			case Less:
				glAlphaFunc(GL11.GL_LESS, refValue);
			break;
			case LessOrEqual:
				glAlphaFunc(GL11.GL_LEQUAL, refValue);
			break;
			default:
				assert false;
		}
	}

	private void checkOpenGLCompatability(int major, int minor) {
		log.debug("Attempting to create window for OpenGL {}.{}", major, minor);

		glfwSetErrorCallback((arg0, arg1) -> log.error("GLFW error: " + String.format("0x%08X", arg0)));

		if (!glfwInit()) {
			throw new RuntimeException("Failed to init GLFW");
		}

//		glfwWindowHint(GLFW_SAMPLES, 4);
//		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, major);
//		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, minor);
		//glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
		glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
		glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_OSMESA_CONTEXT_API);
//		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

		long windowId = glfwCreateWindow(640, 480, "Test", 0, 0);

		if(windowId == 0) {
			destroy();
			throw new RuntimeException("Failed to create window!");
		} else {
			glfwMakeContextCurrent(windowId);
			glfwMakeContextCurrent(0);
			long context = glfwGetCurrentContext();
			if (context == windowId) {
				log.debug("Found current context after make current.");
			}

			int client = glfwGetWindowAttrib(windowId, GLFW_CLIENT_API);
			int majorV = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MAJOR);
			int minorV = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MINOR);
			if (client == GLFW_OPENGL_API) {
				log.debug("client = OpenGL "+ majorV + "." + minorV);
			}
			if (GLFW_OSMESA_CONTEXT_API == glfwGetWindowAttrib(windowId, GLFW_CONTEXT_CREATION_API)) {
				log.debug("OSMesa context creation api is set");
			}
			GL.createCapabilities();
		}

		printInfo();

		destroy();
	}
}
