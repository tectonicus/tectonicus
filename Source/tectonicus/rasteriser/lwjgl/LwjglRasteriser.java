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
package tectonicus.rasteriser.lwjgl;

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

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;

public class LwjglRasteriser implements Rasteriser
{
	private final DisplayType type;
	
	private Pbuffer pbuffer;
	
	private int width, height;
	
	private Map<Integer, Integer> keyCodeMap;
	
	private Map<Integer, Boolean> prevKeyStates;
	
	public LwjglRasteriser(DisplayType type, final int displayWidth, final int displayHeight, final int colourDepth, final int alphaBits, final int depthBits, final int numSamples) throws Exception
	{
		this.type = type;
		
		this.width = displayWidth;
		this.height = displayHeight;
		
		keyCodeMap = new HashMap<Integer, Integer>();
		
		keyCodeMap.put(KeyEvent.VK_0, Keyboard.KEY_0);
		keyCodeMap.put(KeyEvent.VK_1, Keyboard.KEY_1);
		keyCodeMap.put(KeyEvent.VK_2, Keyboard.KEY_2);
		keyCodeMap.put(KeyEvent.VK_3, Keyboard.KEY_3);
		keyCodeMap.put(KeyEvent.VK_4, Keyboard.KEY_4);
		keyCodeMap.put(KeyEvent.VK_5, Keyboard.KEY_5);
		keyCodeMap.put(KeyEvent.VK_6, Keyboard.KEY_6);
		keyCodeMap.put(KeyEvent.VK_7, Keyboard.KEY_7);
		keyCodeMap.put(KeyEvent.VK_8, Keyboard.KEY_8);
		keyCodeMap.put(KeyEvent.VK_9, Keyboard.KEY_9);
		
		keyCodeMap.put(KeyEvent.VK_UP, Keyboard.KEY_UP);
		keyCodeMap.put(KeyEvent.VK_DOWN, Keyboard.KEY_DOWN);
		keyCodeMap.put(KeyEvent.VK_LEFT, Keyboard.KEY_LEFT);
		keyCodeMap.put(KeyEvent.VK_RIGHT, Keyboard.KEY_RIGHT);
		
		keyCodeMap.put(KeyEvent.VK_SPACE, Keyboard.KEY_SPACE);
		keyCodeMap.put(KeyEvent.VK_MINUS, Keyboard.KEY_MINUS);
		keyCodeMap.put(KeyEvent.VK_EQUALS, Keyboard.KEY_EQUALS);
		keyCodeMap.put(KeyEvent.VK_BACK_SPACE, Keyboard.KEY_BACK);
		
		keyCodeMap.put(KeyEvent.VK_A, Keyboard.KEY_A);
		keyCodeMap.put(KeyEvent.VK_B, Keyboard.KEY_B);
		keyCodeMap.put(KeyEvent.VK_C, Keyboard.KEY_C);
		keyCodeMap.put(KeyEvent.VK_D, Keyboard.KEY_D);
		keyCodeMap.put(KeyEvent.VK_E, Keyboard.KEY_E);
		keyCodeMap.put(KeyEvent.VK_F, Keyboard.KEY_F);
		keyCodeMap.put(KeyEvent.VK_G, Keyboard.KEY_G);
		keyCodeMap.put(KeyEvent.VK_H, Keyboard.KEY_H);
		keyCodeMap.put(KeyEvent.VK_I, Keyboard.KEY_I);
		keyCodeMap.put(KeyEvent.VK_J, Keyboard.KEY_J);
		keyCodeMap.put(KeyEvent.VK_K, Keyboard.KEY_K);
		keyCodeMap.put(KeyEvent.VK_L, Keyboard.KEY_L);
		keyCodeMap.put(KeyEvent.VK_M, Keyboard.KEY_M);
		keyCodeMap.put(KeyEvent.VK_N, Keyboard.KEY_N);
		keyCodeMap.put(KeyEvent.VK_O, Keyboard.KEY_O);
		keyCodeMap.put(KeyEvent.VK_P, Keyboard.KEY_P);
		keyCodeMap.put(KeyEvent.VK_Q, Keyboard.KEY_Q);
		keyCodeMap.put(KeyEvent.VK_R, Keyboard.KEY_R);
		keyCodeMap.put(KeyEvent.VK_S, Keyboard.KEY_S);
		keyCodeMap.put(KeyEvent.VK_T, Keyboard.KEY_T);
		keyCodeMap.put(KeyEvent.VK_U, Keyboard.KEY_U);
		keyCodeMap.put(KeyEvent.VK_V, Keyboard.KEY_V);
		keyCodeMap.put(KeyEvent.VK_W, Keyboard.KEY_W);
		keyCodeMap.put(KeyEvent.VK_X, Keyboard.KEY_X);
		keyCodeMap.put(KeyEvent.VK_Y, Keyboard.KEY_Y);
		keyCodeMap.put(KeyEvent.VK_Z, Keyboard.KEY_Z);
		
		prevKeyStates = new HashMap<Integer, Boolean>();
		
		Drawable drawable = Display.getDrawable();
		System.out.println("\tDrawable: "+drawable);
	
		// Make a list of pixel formats to try (in preferance order)
		ArrayList<PixelFormat> pixelFormats = new ArrayList<PixelFormat>();
		
		// As requested
		pixelFormats.add( new PixelFormat(colourDepth, alphaBits, depthBits, 0, numSamples) );
		
		// No anti-aliasing
		pixelFormats.add( new PixelFormat(colourDepth, alphaBits, depthBits, 0, 0) );
		
		// No anti-aliasing or alpha buffer
		pixelFormats.add( new PixelFormat(colourDepth, 0, depthBits, 0, 0) );
		
		// No anti-aliasing, no alpha buffer, 16bit colour
		pixelFormats.add( new PixelFormat(16, 0, depthBits, 0, 0) );
		
		// No anti-aliasing, no alpha buffer, 16bit colour, 16bit depth
		pixelFormats.add( new PixelFormat(16, 0, 16, 0, 0) );
		
		// Ugh. Anything with a depth buffer.
		pixelFormats.add( new PixelFormat(0, 0, 1, 0, 0) );
		
		PixelFormat usedPixelFormat = null;
		LWJGLException pbufferException = null;
		
		if (type == DisplayType.Offscreen)
		{
			for (PixelFormat pf : pixelFormats)
			{
				try
				{
					pbuffer = new Pbuffer(displayWidth, displayHeight, pf, drawable);
					usedPixelFormat = pf;
					break;
				}
				catch (LWJGLException e)
				{
					pbufferException = e;
				}
			}
			
			if (pbuffer != null)
			{
				// Ok!
				System.out.println("\tcreated pbuffer: "+pbuffer);
				System.out.println("\tused pixel format:   colour:"+usedPixelFormat.getBitsPerPixel()
														+" depth:"+usedPixelFormat.getDepthBits()
														+" alpha:"+usedPixelFormat.getAlphaBits()
														+" stencil:"+usedPixelFormat.getStencilBits()
														+" samples:"+usedPixelFormat.getSamples());
			}
			else
			{
				System.err.println("Could not create pbuffer! (colour:"+colourDepth+", alpha:"+alphaBits+", depth:"+depthBits+", samples:"+numSamples+")");
				throw pbufferException;
			}
			
			pbuffer.makeCurrent();
			
			// Issue a few gl commands so we fail early if the pbuffer is actually a bogus one
			resetState();
			
		}
		else if (type == DisplayType.Window)
		{
			Display.setDisplayMode(new DisplayMode(displayWidth, displayHeight));
			Display.setLocation( (Display.getDisplayMode().getWidth() - displayWidth) / 2, (Display.getDisplayMode().getHeight() - displayHeight)/2 );
			Display.setTitle("Tectonicus");
			
			// TODO: Use same pixel formats from above here
			Display.create( new PixelFormat(32, 0, 16, 0, 4) );
		}
		else
		{
			throw new RuntimeException("Unknown display type: "+type);
		}
		
		System.out.println("\tdisplay created ok");
	}
	
	@Override
	public void destroy()
	{
		if (type == DisplayType.Window)
		{
			Display.destroy();
		}
		else if (type == DisplayType.Offscreen)
		{
			pbuffer.destroy();
		}
	}
	
	@Override
	public void printInfo()
	{
		System.out.println(" -- Lwjgl Rasteriser -- ");
		System.out.println("\ttype: "+type);
		
		System.out.println("\twidth: "+width);
		System.out.println("\theigth: "+height);
		System.out.println("\tpBuffer: "+pbuffer);
		
		System.out.println("\tOpenGL Vendor: "+GL11.glGetString(GL11.GL_VENDOR));
		System.out.println("\tOpenGL Renderer: "+GL11.glGetString(GL11.GL_RENDERER));
		System.out.println("\tOpenGL Version: "+GL11.glGetString(GL11.GL_VERSION));
		
	//	System.out.println();
		
	//	GL11.glGetString(GL11.GL_EXTENSIONS);
	}
	
	@Override
	public void sync()
	{
		prevKeyStates.clear();
		for (Integer i : keyCodeMap.values())
		{
			prevKeyStates.put(i, Keyboard.isKeyDown(i));
		}
		
		Display.update();
		Display.sync(60);
	}
	
	@Override
	public boolean isCloseRequested()
	{
		return Display.isCloseRequested();
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
		return Keyboard.isKeyDown(lwjglKey);
	}
	
	public boolean isKeyJustDown(final int vkKey)
	{
		if (!keyCodeMap.containsKey(vkKey))
			throw new RuntimeException("No mapping for :"+vkKey);
		
		Integer lwjglKey = keyCodeMap.get(vkKey);
		
		return Keyboard.isKeyDown(lwjglKey) && !prevKeyStates.get(lwjglKey);
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
		matrix.store(buffer);
		buffer.flip();
		GL11.glLoadMatrix(buffer);
	}
	
	public void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up)
	{
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix.store(buffer);
		buffer.flip();
		GL11.glLoadMatrix(buffer);
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
