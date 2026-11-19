/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWNativeOSMesa;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGLCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.JNI;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimitiveType;
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

import static org.lwjgl.egl.EGL10.EGL_EXTENSIONS;
import static org.lwjgl.egl.EGL10.EGL_NONE;
import static org.lwjgl.egl.EGL10.EGL_NO_CONTEXT;
import static org.lwjgl.egl.EGL10.EGL_NO_DISPLAY;
import static org.lwjgl.egl.EGL10.EGL_NO_SURFACE;
import static org.lwjgl.egl.EGL10.EGL_VENDOR;
import static org.lwjgl.egl.EGL10.EGL_VERSION;
import static org.lwjgl.egl.EGL10.eglChooseConfig;
import static org.lwjgl.egl.EGL10.eglCreateContext;
import static org.lwjgl.egl.EGL10.eglDestroyContext;
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
import static org.lwjgl.egl.EXTDeviceDRM.EGL_DRM_DEVICE_FILE_EXT;
import static org.lwjgl.egl.EXTDeviceEnumeration.eglQueryDevicesEXT;
import static org.lwjgl.egl.EXTDeviceQuery.eglQueryDeviceStringEXT;
import static org.lwjgl.egl.EXTPlatformDevice.EGL_PLATFORM_DEVICE_EXT;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetVersionString;
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
import static org.lwjgl.opengl.GL11.glDeleteTextures;
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

@Slf4j
public class LwjglRasteriser implements Rasteriser {
	private static final int EGL_PLATFORM_SURFACELESS_MESA = 0x31DD;
	
	private final DisplayType type;
	
	private long window = 0;
	private long eglDisplay = 0;
	private long eglContext = 0;
	private String eglDisplaySource = "";
	
	private final int width, height;
	
	private final Map<Integer, Integer> keyCodeMap;
	
	private final Map<Integer, Boolean> prevKeyStates;
	
	private long prevMillis;

	// Reused for every screenshot: a tile render takes thousands, and each direct buffer
	// would otherwise stay allocated until the garbage collector happened to free it.
	private ByteBuffer screenshotBuffer;
	
	public LwjglRasteriser(DisplayType type, final int displayWidth, final int displayHeight, final int colourDepth, final int alphaBits, final int depthBits, final int numSamples) throws Exception {
		this(type, displayWidth, displayHeight, colourDepth, alphaBits, depthBits, numSamples, -1);
	}
	
	/**
	 * @param eglDevice index of the EGL device (GPU) to render on when using EGL, or -1 to choose automatically
	 */
	public LwjglRasteriser(DisplayType type, final int displayWidth, final int displayHeight, final int colourDepth, final int alphaBits, final int depthBits, final int numSamples, final int eglDevice) throws Exception {
		if (type == DisplayType.OFFSCREEN_EGL && !OsDetect.isUnix()) {
			log.warn("EGL is only supported on Linux, using the default offscreen context instead");
			type = DisplayType.OFFSCREEN;
		}
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
		pixelFormats.add(new LwjglPixelFormat(colourDepth, alphaBits, depthBits, 0, numSamples));
		
		// No anti-aliasing
		pixelFormats.add(new LwjglPixelFormat(colourDepth, alphaBits, depthBits, 0, 0));
		
		// No anti-aliasing or alpha buffer
		pixelFormats.add(new LwjglPixelFormat(colourDepth, 0, depthBits, 0, 0));
		
		// No anti-aliasing, no alpha buffer, 16bit colour
		pixelFormats.add(new LwjglPixelFormat(16, 0, depthBits, 0, 0));
		
		// No anti-aliasing, no alpha buffer, 16bit colour, 16bit depth
		pixelFormats.add(new LwjglPixelFormat(16, 0, 16, 0, 0));
		
		// Ugh. Anything with a depth buffer.
		pixelFormats.add(new LwjglPixelFormat(0, 0, 1, 0, 0));

		boolean isMac = OsDetect.isMac();
		
		boolean isOSMesa = type == DisplayType.OFFSCREEN_OSMESA;
		
		if (type == DisplayType.WINDOW || type == DisplayType.OFFSCREEN || isOSMesa) {
			if (isMac) {
				Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
			}
			
			log.debug("GLFW version: {}", glfwGetVersionString());
			glfwSetErrorCallback((error, description) ->
					log.error("GLFW error [{}]: {}", String.format("0x%08X", error), GLFWErrorCallback.getDescription(description)));
			
			if (isOSMesa) {
				configureOSMesa();
			}
			
			if (!glfwInit()) {
				throw new RuntimeException("Failed to init GLFW");
			}
			
			if (isMac && !isOSMesa) {
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2);
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
			} else {
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
				glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
				glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
			}
		}
		
		if (type == DisplayType.OFFSCREEN || isOSMesa) {
			glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
			if (isOSMesa) {
				glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_OSMESA_CONTEXT_API);
			}
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
				if (window != 0) {
					break;
				}
			}
		} else if (type == DisplayType.OFFSCREEN_EGL) {
			createEglContext(pixelFormats, eglDevice);
		} else {
			throw new RuntimeException("Unknown display type: " + type);
		}
		
		if (type != DisplayType.OFFSCREEN_EGL) {
			if (window == 0) {
				throw new RuntimeException("Failed to create GLFW window!");
			}
			
			glfwMakeContextCurrent(window);
		}
		
		GL.createCapabilities();
		
		if ((!isMac && (type == DisplayType.OFFSCREEN || type == DisplayType.OFFSCREEN_EGL)) || isOSMesa) {
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
		} else if (isMac && type == DisplayType.OFFSCREEN) {
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
	public void destroy() {
		screenshotBuffer = null;
		if (type == DisplayType.OFFSCREEN_EGL) {
			eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
			if (eglContext != EGL_NO_CONTEXT) {
				eglDestroyContext(eglDisplay, eglContext);
			}
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
	public void printInfo() {
		log.debug(" -- Lwjgl Rasteriser -- ");
		log.debug("\tLWJGL version: {}", Version.getVersion());
		log.debug("\ttype: {}", type);
		log.debug("\twidth: {}", width);
		log.debug("\theight: {}", height);
		if (type == DisplayType.OFFSCREEN_EGL) {
			log.debug("\tEGL display: {}", eglDisplaySource);
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
	public void sync() {
		long currMillis = System.currentTimeMillis();
		long delta = currMillis - prevMillis;
		long fps = 1000 / (delta > 0 ? delta : 1);
		if (fps > 60) {
			return;
		}
		prevMillis = currMillis;
		
		prevKeyStates.clear();
		for (Integer i : keyCodeMap.values()) {
			prevKeyStates.put(i, glfwGetKey(window, i) == GLFW.GLFW_PRESS);
		}
		
		glfwPollEvents();
		glfwSwapBuffers(window);
	}
	
	@Override
	public boolean isCloseRequested() {
		return glfwWindowShouldClose(window);
	}
	
	@Override
	public void setViewport(final int x, final int y, final int width, final int height) {
		GL11.glViewport(x, y, width, height);
	}
	
	@Override
	public boolean isKeyDown(final int vkKey) {
		if (!keyCodeMap.containsKey(vkKey))
			throw new RuntimeException("No mapping for :" + vkKey);
		
		Integer lwjglKey = keyCodeMap.get(vkKey);
		return glfwGetKey(window, lwjglKey) == GLFW.GLFW_PRESS;
	}
	
	public boolean isKeyJustDown(final int vkKey) {
		if (!keyCodeMap.containsKey(vkKey))
			throw new RuntimeException("No mapping for :" + vkKey);
		
		Integer lwjglKey = keyCodeMap.get(vkKey);
		
		return glfwGetKey(window, lwjglKey) == GLFW.GLFW_PRESS && !prevKeyStates.get(lwjglKey);
	}
	
	public int getDisplayWidth() {
		return width;
	}
	
	public int getDisplayHeight() {
		return height;
	}
	
	@Override
	public int getMaxTextureSize() {
		return GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
	}

	public Texture createTexture(BufferedImage image, TextureFilter filter) {
		final int id = LwjglTextureUtils.createTexture(image, filter);
		return new LwjglTexture(id, image.getWidth(), image.getHeight());
	}
	
	public Texture createTexture(BufferedImage[] mips, TextureFilter filter) {
		final int id = LwjglTextureUtils.createTexture(mips, filter);
		return new LwjglTexture(id, mips[0].getWidth(), mips[0].getHeight());
	}

	@Override
	public void destroyTexture(Texture texture) {
		glDeleteTextures(((LwjglTexture) texture).getId());
	}
	
	public Mesh createMesh(Texture texture) {
		return new LwjglMesh((LwjglTexture) texture);
	}
	
	@Override
	public void beginFrame() {
		
	}
	
	@Override
	public void resetState() {
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
	public void clear(Color clearColour) {
		glClearColor(clearColour.getRed() / 255.0f, clearColour.getGreen() / 255.0f, clearColour.getBlue() / 255.0f, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
	}
	
	@Override
	public void clearDepthBuffer() {
		glClear(GL_DEPTH_BUFFER_BIT);
	}
	
	// Slow, simple version. Pull framebuffer and convert to BufferedImage via setRGB()
	public BufferedImage takeScreenshot2(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat) {
		ByteBuffer screenContentsBytes = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
		IntBuffer screenContents = screenContentsBytes.asIntBuffer();
		
		glReadPixels(startX, startY, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenContents);
		
		int[] pixels = new int[width * height];
		screenContents.get(pixels);
		
		final int pixelFormat = imageFormat.hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
		BufferedImage img = new BufferedImage(width, height, pixelFormat);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final int rgba = pixels[x + y * width];
				
				img.setRGB(x, height - 1 - y, rgba);
			}
		}
		
		return img;
	}
	
	public BufferedImage takeScreenshot(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat) {
		// The image itself is not reused: the write queue still holds earlier tiles while they are saved.
		final int bytesPerPixel = imageFormat.hasAlpha() ? 4 : 3;
		screenshotBuffer = reuseBuffer(screenshotBuffer, width * height * bytesPerPixel);
		ByteBuffer screenContentsBytes = screenshotBuffer;
		// Rows must be tightly packed: the default alignment of 4 pads 3-byte BGR rows whose width isn't a multiple of 4.
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);

		if (imageFormat.hasAlpha()) {
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			int[] pixels = ((DataBufferInt) (img.getRaster().getDataBuffer())).getData();
			IntBuffer screenContents = screenContentsBytes.asIntBuffer();
			glReadPixels(startX, startY, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, screenContents);
			copyRowsFlipped(screenContents, pixels, width, height);
			return img;
		}

		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		byte[] pixels = ((DataBufferByte) (img.getRaster().getDataBuffer())).getData();
		glReadPixels(startX, startY, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, screenContentsBytes);
		copyRowsFlipped(screenContentsBytes, pixels, width * 3, height);
		return img;
	}

	/**
	 * Returns {@code buffer} cleared and limited to {@code bytes}, or a new direct buffer when it is too small.
	 * A larger buffer is kept for smaller screenshots, so alternating sizes do not reallocate.
	 */
	static ByteBuffer reuseBuffer(ByteBuffer buffer, int bytes) {
		if (buffer == null || buffer.capacity() < bytes)
			buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
		buffer.clear().limit(bytes);
		return buffer;
	}

	/** Copies bottom-up OpenGL rows (relative to the read origin) into a top-down image buffer. */
	static void copyRowsFlipped(IntBuffer source, int[] dest, int rowLength, int height) {
		for (int row = 0; row < height; row++) {
			source.position(row * rowLength);
			source.get(dest, (height - row - 1) * rowLength, rowLength);
		}
	}

	static void copyRowsFlipped(ByteBuffer source, byte[] dest, int rowLength, int height) {
		for (int row = 0; row < height; row++) {
			source.position(row * rowLength);
			source.get(dest, (height - row - 1) * rowLength, rowLength);
		}
	}
	
	public void bindTexture(Texture texture) {
		LwjglTexture tex = (LwjglTexture) texture;
		
		if (tex != null) {
			glBindTexture(GL_TEXTURE_2D, tex.getId());
		} else {
			glBindTexture(GL_TEXTURE_2D, 0);
		}
	}
	
	public void beginShape(PrimitiveType type) {
		int glType = GL_TRIANGLES;
		switch (type) {
			case POINTS:
				glType = GL_POINTS;
				break;
			case LINES:
				glType = GL_LINES;
				break;
			case QUADS:
				glType = GL_QUADS;
				break;
			default:
				assert false;
		}
		glBegin(glType);
	}
	
	public void colour(final float r, final float g, final float b, final float a) {
		GL11.glColor4f(r, g, b, a);
	}
	
	@Override
	public void texCoord(final float u, final float v) {
		GL11.glTexCoord2f(u, v);
	}
	
	public void vertex(final float x, final float y, final float z) {
		GL11.glVertex3f(x, y, z);
	}
	
	public void endShape() {
		GL11.glEnd();
	}
	
	public void setProjectionMatrix(Matrix4f matrix) {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix.get(buffer);
		GL11.glLoadMatrixf(buffer);
	}
	
	public void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up) {
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix.get(buffer);
		GL11.glLoadMatrixf(buffer);
	}
	
	@Override
	public void enableBlending(final boolean enable) {
		if (enable)
			glEnable(GL11.GL_BLEND);
		else
			glDisable(GL11.GL_BLEND);
	}
	
	@Override
	public void enableDepthTest(boolean enable) {
		if (enable)
			glEnable(GL11.GL_DEPTH_TEST);
		else
			glDisable(GL11.GL_DEPTH_TEST);
	}
	
	@Override
	public void enableAlphaTest(boolean enable) {
		if (enable)
			glEnable(GL11.GL_ALPHA_TEST);
		else
			glDisable(GL11.GL_ALPHA_TEST);
	}
	
	@Override
	public void enableColourWriting(final boolean colourMask, final boolean alphaMask) {
		glColorMask(colourMask, colourMask, colourMask, alphaMask);
	}
	
	@Override
	public void enableDepthWriting(final boolean enable) {
		GL11.glDepthMask(enable);
	}
	
	@Override
	public void setBlendFunc(BlendFunc func) {
		switch (func) {
			case REGULAR:
				glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				break;
			case ADDITIVE:
				glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				break;
			default:
				assert false;
		}
	}
	
	@Override
	public void setAlphaFunc(AlphaFunc func, final float refValue) {
		switch (func) {
			case GREATER:
				glAlphaFunc(GL11.GL_GREATER, refValue);
				break;
			case GREATER_OR_EQUAL:
				glAlphaFunc(GL11.GL_GEQUAL, refValue);
				break;
			case EQUAL:
				glAlphaFunc(GL11.GL_EQUAL, refValue);
				break;
			case LESS:
				glAlphaFunc(GL11.GL_LESS, refValue);
				break;
			case LESS_OR_EQUAL:
				glAlphaFunc(GL11.GL_LEQUAL, refValue);
				break;
			default:
				assert false;
		}
	}
	
	/**
	 * Points LWJGL and GLFW at the OSMesa library and selects GLFW's null platform, so that a context can be
	 * created without any window system. Must run before anything else touches {@link GL}, because LWJGL loads
	 * the OpenGL library named by {@link Configuration#OPENGL_LIBRARY_NAME} when that class is first used.
	 */
	private static void configureOSMesa() {
		if (Configuration.OPENGL_LIBRARY_NAME.get() == null) {
			Configuration.OPENGL_LIBRARY_NAME.set(defaultOSMesaLibraryName());
		}
		String libraryName = Configuration.OPENGL_LIBRARY_NAME.get();
		log.debug("Using OSMesa library: {}", libraryName);
		
		FunctionProvider provider;
		try {
			provider = GL.getFunctionProvider();
		} catch (Throwable t) {
			throw new RuntimeException(osMesaMissingMessage(libraryName), t);
		}
		if (provider == null || provider.getFunctionAddress("OSMesaGetProcAddress") == 0) {
			// LWJGL falls back to the system OpenGL library when the named one can't be loaded
			throw new RuntimeException(osMesaMissingMessage(libraryName));
		}
		
		// Make GLFW use the same OSMesa library as LWJGL, otherwise function pointers come from different libraries
		GLFWNativeOSMesa.setPath(provider);
		GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_NULL);
	}
	
	private static String defaultOSMesaLibraryName() {
		if (OsDetect.isWindows()) {
			return "osmesa.dll";
		} else if (OsDetect.isMac()) {
			return "libOSMesa.8.dylib";
		}
		return "libOSMesa.so.8";
	}
	
	private static String osMesaMissingMessage(String libraryName) {
		return "Unable to load the OSMesa library '" + libraryName + "'. Install it (for example libosmesa6 on Debian/Ubuntu"
				+ " or mesa-compat-libOSMesa on Fedora) or point Tectonicus at it with -Dorg.lwjgl.opengl.libname=<path>."
				+ " Mesa 25.1 and later no longer build OSMesa; useEGL is the alternative for headless software rendering.";
	}
	
	/**
	 * Creates an OpenGL compatibility context with EGL and makes it current without a surface; rendering goes to the
	 * framebuffer object created afterwards. With no eglDevice set, the display is chosen in this order: the default
	 * display (the running X11 or Wayland session), the first EGL device (a GPU, no window system needed), and Mesa's
	 * surfaceless platform (software rendering through llvmpipe).
	 */
	private void createEglContext(List<LwjglPixelFormat> pixelFormats, int eglDevice) {
		IntBuffer major = BufferUtils.createIntBuffer(1);
		IntBuffer minor = BufferUtils.createIntBuffer(1);
		
		if (eglDevice >= 0) {
			eglDisplay = getDeviceDisplay(eglDevice);
			if (eglDisplay == EGL_NO_DISPLAY) {
				throw new RuntimeException("EGL device " + eglDevice + " is not available. The devices found are listed in the debug log.");
			}
			if (!eglInitialize(eglDisplay, major, minor)) {
				throw new RuntimeException("Failed to initialize EGL device " + eglDevice + ". EGL error: " + eglErrorString());
			}
		} else {
			eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
			eglDisplaySource = "default display";
			if (eglDisplay == EGL_NO_DISPLAY || !eglInitialize(eglDisplay, major, minor)) {
				log.debug("Default EGL display unavailable, trying EGL devices.");
				eglDisplay = getDeviceDisplay(0);
				if (eglDisplay == EGL_NO_DISPLAY || !eglInitialize(eglDisplay, major, minor)) {
					// Mesa can still render offscreen through its surfaceless platform, which is what EGL_PLATFORM=surfaceless selects
					log.debug("No usable EGL device, trying the Mesa surfaceless platform.");
					eglDisplay = getPlatformDisplay(EGL_PLATFORM_SURFACELESS_MESA, EGL_DEFAULT_DISPLAY);
					eglDisplaySource = "Mesa surfaceless platform";
					if (eglDisplay == EGL_NO_DISPLAY || !eglInitialize(eglDisplay, major, minor)) {
						throw new RuntimeException("Failed to initialize EGL. EGL error: " + eglErrorString());
					}
				}
			}
		}
		log.debug("Using EGL for context creation: {}", eglDisplaySource);
		
		long config = chooseEglConfig(pixelFormats);
		
		if (!eglBindAPI(EGL_OPENGL_API)) {
			throw new RuntimeException("EGL display does not support desktop OpenGL. EGL error: " + eglErrorString());
		}
		
		int[] contextAttributes = {
				EGL_CONTEXT_MAJOR_VERSION, 3,
				EGL_CONTEXT_MINOR_VERSION, 0,
				EGL_CONTEXT_OPENGL_PROFILE_MASK, EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT,
				EGL_NONE
		};
		
		eglContext = eglCreateContext(eglDisplay, config, EGL_NO_CONTEXT, contextAttributes);
		if (eglContext == EGL_NO_CONTEXT) {
			throw new RuntimeException("Failed to create EGL context. EGL error: " + eglErrorString());
		}
		
		if (!eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, eglContext)) {
			throw new RuntimeException("Failed to make EGL context current. EGL error: " + eglErrorString());
		}
	}
	
	/** Returns the first EGL config that matches one of the pixel formats, trying them in order of preference. */
	private long chooseEglConfig(List<LwjglPixelFormat> pixelFormats) {
		PointerBuffer configs = BufferUtils.createPointerBuffer(1);
		int[] configCount = new int[1];
		for (LwjglPixelFormat pf : pixelFormats) {
			if (!eglChooseConfig(eglDisplay, pf.getEGLAttributes(), configs, configCount)) {
				throw new RuntimeException("eglChooseConfig failed. EGL error: " + eglErrorString());
			}
			if (configCount[0] > 0) {
				return configs.get(0);
			}
		}
		throw new RuntimeException("No EGL config supports OpenGL rendering with any of the requested pixel formats");
	}
	
	/** Gets a display for the EGL device at the given index, or EGL_NO_DISPLAY if there is no such device. */
	private long getDeviceDisplay(int index) {
		// LWJGL's client capability flags don't reliably reflect the client extension string, so check it directly
		String clientExtensions = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS);
		boolean canEnumerate = clientExtensions != null
				&& (clientExtensions.contains("EGL_EXT_device_enumeration") || clientExtensions.contains("EGL_EXT_device_base"))
				&& clientExtensions.contains("EGL_EXT_platform_device");
		if (!canEnumerate || EGL.getCapabilities().eglQueryDevicesEXT == 0) {
			log.debug("EGL device enumeration is not supported");
			return EGL_NO_DISPLAY;
		}
		
		int[] count = new int[1];
		if (!eglQueryDevicesEXT(null, count) || count[0] == 0) {
			log.debug("No EGL devices found");
			return EGL_NO_DISPLAY;
		}
		PointerBuffer devices = BufferUtils.createPointerBuffer(count[0]);
		eglQueryDevicesEXT(devices, count);
		
		for (int i = 0; i < count[0]; i++) {
			log.debug("EGL device {}: {}", i, describeDevice(devices.get(i)));
		}
		if (index >= count[0]) {
			log.error("EGL device {} requested but only {} device(s) found", index, count[0]);
			return EGL_NO_DISPLAY;
		}
		
		long device = devices.get(index);
		eglDisplaySource = "EGL device " + index + " (" + describeDevice(device) + ")";
		return getPlatformDisplay(EGL_PLATFORM_DEVICE_EXT, device);
	}
	
	private static String describeDevice(long device) {
		String extensions = eglQueryDeviceStringEXT(device, EGL_EXTENSIONS);
		if (extensions != null && extensions.contains("EGL_EXT_device_drm")) {
			String drmFile = eglQueryDeviceStringEXT(device, EGL_DRM_DEVICE_FILE_EXT);
			if (drmFile != null) {
				return drmFile;
			}
		}
		if (extensions != null && extensions.contains("EGL_MESA_device_software")) {
			return "Mesa software renderer";
		}
		return "unknown device";
	}
	
	/**
	 * Calls eglGetPlatformDisplay directly. LWJGL's binding rejects a null native display, but
	 * EGL_MESA_platform_surfaceless requires EGL_DEFAULT_DISPLAY.
	 */
	private static long getPlatformDisplay(int platform, long nativeDisplay) {
		EGLCapabilities caps = EGL.getCapabilities();
		long function = caps.eglGetPlatformDisplay != 0 ? caps.eglGetPlatformDisplay : caps.eglGetPlatformDisplayEXT;
		if (function == 0) {
			return EGL_NO_DISPLAY;
		}
		// eglGetPlatformDisplay takes an EGLAttrib list and the EXT variant an EGLint list; null works for both
		return JNI.callPPP(platform, nativeDisplay, 0L, function);
	}
	
	private static String eglErrorString() {
		return String.format("0x%04X", eglGetError());
	}
}
