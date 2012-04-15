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
package tectonicus.rasteriser.processing;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PGraphics3D;
import processing.core.PMatrix3D;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;

public class ProcessingRasteriser implements Rasteriser
{
	private final DisplayType type;
	private final int displayWidth, displayHeight;
	
	private JFrame frame;
	private Canvas canvas;
	
	private PApplet processingApplet;
	private PGraphics3D graphics;
	
	private KeyHandler keyHandler;
	private WindowHandler windowHandler;
	
	public ProcessingRasteriser(DisplayType type, final int displayWidth, final int displayHeight)
	{
		this.type = type;
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		
		frame = new JFrame("Title here!");
		frame.setLayout(new BorderLayout());
		
		processingApplet = new Embedded(displayWidth, displayHeight);
	//	frame.add(processingApplet, BorderLayout.CENTER);
		processingApplet.init();
        
		graphics = (PGraphics3D)processingApplet.createGraphics(displayWidth, displayHeight, PGraphics3D.P3D);
        
/*		From docs:
		
		big = createGraphics(3000, 3000, P3D);
		big.beginDraw();
		big.background(128);
		big.line(20, 1800, 1800, 900);
		// etc..
		big.endDraw();
		// make sure the file is written to the sketch folder
		big.save("big.tif");
*/
		
		if (type == DisplayType.Window)
		{
			canvas = new Canvas();
			canvas.setPreferredSize(new Dimension(displayWidth, displayHeight));
			canvas.setMinimumSize(new Dimension(displayWidth, displayHeight));
			
			frame.add(canvas);
			
			frame.pack();
			frame.setVisible(true);
			
			keyHandler = new KeyHandler();
			frame.addKeyListener(keyHandler);
			processingApplet.addKeyListener(keyHandler);
			canvas.addKeyListener(keyHandler);
			
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			
			windowHandler = new WindowHandler();
			frame.addWindowListener(windowHandler);
		}
		else if (type == DisplayType.Offscreen)
		{
			// http://wiki.processing.org/w/Draw_to_off-screen_buffer
			
		//	buffer = createGraphics(500, 500, JAVA2D);
		}
		else
		{
			throw new RuntimeException("Unknown display type:"+type);
		}
	}
	
	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub
		
		processingApplet.destroy();
		
		frame.setVisible(false);
		frame.dispose();
	}
	
	@Override
	public void printInfo()
	{
		System.out.println(" -- Processing Rasteriser -- ");
		System.out.println("\t type:" + type);
		System.out.println("\t width: "+displayWidth);
		System.out.println("\t height: "+displayHeight);
	}
	
	public void setViewport(final int x, final int y, final int width, final int height)
	{
		throw new RuntimeException("Not implemented");
	}
	
	@Override
	public boolean isCloseRequested()
	{
		return windowHandler.isCloseRequested();
	}
	
	@Override
	public boolean isKeyDown(final int vkKey)
	{
		return keyHandler.isKeyDown(vkKey);
	}
	
	@Override
	public boolean isKeyJustDown(int vkKey)
	{
		return keyHandler.isJustDown(vkKey);
	}
	
	@Override
	public int getDisplayWidth()
	{
		return displayWidth;
	}
	
	@Override
	public int getDisplayHeight()
	{
		return displayHeight;
	}
	
	@Override
	public Texture createTexture(BufferedImage image, TextureFilter filter)
	{
		return new ProcessingTexture(image, filter);
	}
	
	public Texture createTexture(BufferedImage[] mips, TextureFilter filter)
	{
		throw new RuntimeException("Not implemented!");
	}
	
	@Override
	public Mesh createMesh(Texture texture)
	{
		ProcessingTexture tex = (ProcessingTexture)texture;
		return new ProcessingMesh(tex, graphics);
	}
	
	@Override
	public void resetState()
	{
		// TODO Auto-generated method stub
		graphics.textureMode(PGraphics3D.NORMAL);
		graphics.noLights();
		
		graphics.colorMode(PConstants.RGB, 1);
		
		graphics.hint(PConstants.ENABLE_DEPTH_TEST);
	//	graphics.hint(PConstants.ENABLE_DEPTH_SORT); // works, but eats up huge amounts of memory
	}
	
	@Override
	public void beginFrame()
	{
		// TODO Auto-generated method stub
		graphics.beginDraw();
	}
	
	@Override
	public void clear(Color clearColour)
	{
		// TODO Auto-generated method stub
		graphics.background(clearColour.getRed(), clearColour.getGreen(), clearColour.getBlue(), 255);
	}
	
	@Override
	public void clearDepthBuffer()
	{
		
	}
	
	@Override
	public void sync()
	{
		// TODO Auto-generated method stub
		
		keyHandler.sync();
		
		graphics.endDraw();
		
		Image image = graphics.getImage();
		
		// processing image coming out upside down for some reason
		// so flip it here
		
	//	canvas.getGraphics().drawImage(image, 0, 0, null);
		canvas.getGraphics().drawImage(image,
										0, 0, canvas.getWidth(), canvas.getHeight(),
										0, canvas.getHeight(), canvas.getWidth(), 0, null);
	}
	
	@Override
	public BufferedImage takeScreenshot(final int startX, final int startY, final int width, final int height, ImageFormat imageFormat)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void bindTexture(Texture tex)
	{
		ProcessingTexture texture = (ProcessingTexture)tex;
		
		graphics.texture( texture.getPImage() );
	}
	
	@Override
	public void beginShape(PrimativeType type)
	{
		int pType = PGraphics.POINTS;
		switch (type)
		{
				case Points:
					pType = PGraphics.POINTS;
					break;
				case Lines:
					pType = PGraphics.LINES;
					break;
				case Quads:
					pType = PGraphics.QUADS;
					break;
				default:
					break;
		}
		
		graphics.beginShape(pType);
	}
	
	@Override
	public void colour(final float r, final float g, final float b, final float a)
	{
		// TODO Auto-generated method stub
		
		graphics.fill(r, g, b, a);
	}
	
	@Override
	public void texCoord(final float u, final float v)
	{
		// TODO
	}
	
	@Override
	public void vertex(final float x, final float y, final float z)
	{
		graphics.vertex(x, y, z);
	}
	
	@Override
	public void endShape()
	{
		graphics.endShape();
	}
	
	@Override
	public void setProjectionMatrix(Matrix4f matrix)
	{
		PMatrix3D pMatrix = ProcessingUtil.toPMatrix(matrix);
		
		graphics.projection.set(pMatrix);
	}
	
	@Override
	public void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up)
	{
		PMatrix3D pMatrix = ProcessingUtil.toPMatrix(matrix);
		
		graphics.camera.set(pMatrix);
	}
	
	@Override
	public void enableBlending(boolean enable)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enableDepthTest(boolean enable)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enableAlphaTest(boolean enable)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enableColourWriting(final boolean colourMask, final boolean alphaMask)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enableDepthWriting(boolean enable)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setBlendFunc(BlendFunc func)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setAlphaFunc(AlphaFunc func, float refValue)
	{
		// TODO Auto-generated method stub
		
	}
	
	private class Embedded extends PApplet
	{
		private static final long serialVersionUID = 1L;
		
		private final int width, height;
		
		public Embedded(final int width, final int height)
		{
			this.width = width;
			this.height = height;
		}
		
		public void setup()
		{
			// original setup code here ...
			size(width, height);
			
			// prevent thread from starving everything else
			noLoop();
		}
		
		public void draw()
		{
			// drawing code goes here
		}
		 
		public void mousePressed()
		{
			// do something based on mouse movement
		
			// update the screen (run draw once)
			redraw();
		}
	}
	
	private class KeyHandler implements KeyListener
	{
		private Set<Integer> justDownKeys;
		private Set<Integer> downKeys;
		
		public KeyHandler()
		{
			justDownKeys = new HashSet<Integer>();
			downKeys = new HashSet<Integer>();
		}
		
		public void sync()
		{
			justDownKeys.clear();
		}
		
		@Override
		public void keyTyped(KeyEvent e)
		{
			// TODO Auto-generated method stub
			
		}
		
		public boolean isJustDown(final int keyCode)
		{
			return justDownKeys.contains(keyCode);
		}
		
		@Override
		public void keyPressed(KeyEvent e)
		{
			downKeys.add( e.getKeyCode() );
			
			justDownKeys.add( e.getKeyCode() );
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
			downKeys.remove( e.getKeyCode() );
		}
		
		public boolean isKeyDown(final int keyCode)
		{
			return downKeys.contains(keyCode);
		}
	}
	
	private class WindowHandler extends WindowAdapter
	{
		private boolean isCloseRequested;
		
		public boolean isCloseRequested()
		{
			return isCloseRequested;
		}
		
		@Override
		public void windowClosing(WindowEvent e)
		{
			super.windowClosing(e);
			
			isCloseRequested = true;
		}
	}
}
