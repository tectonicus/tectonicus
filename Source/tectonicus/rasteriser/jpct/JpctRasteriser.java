/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.jpct;

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;


import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.threed.jpct.FrameBuffer;
import com.threed.jpct.IRenderer;
import com.threed.jpct.SimpleVector;

import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;

public class JpctRasteriser implements Rasteriser
{
	private com.threed.jpct.World world;
	private FrameBuffer frameBuffer;
	private JFrame jframe;
	
	private boolean isClosing;
	
	public JpctRasteriser(DisplayType displayType, final int displayWidth, final int displayHeight)
	{
		if (displayType == DisplayType.Window)
		{
			jframe = new JFrame("Hello world");
			jframe.setSize(displayWidth, displayHeight);
			jframe.setVisible(true);
			jframe.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			jframe.addWindowListener(new WindowHandler());
		}
		else
		{
			throw new RuntimeException("Not implemented");
		}
		
		world = new com.threed.jpct.World();
		world.setAmbientLight(255, 255, 255);
		
		frameBuffer = new FrameBuffer(displayWidth, displayHeight, FrameBuffer.SAMPLINGMODE_NORMAL);
		frameBuffer.enableRenderer(IRenderer.RENDERER_SOFTWARE);
	}
	
	@Override
	public void destroy()
	{
		frameBuffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
		frameBuffer.dispose();
		
		jframe.setVisible(false);
		jframe.dispose();
	}
	
	@Override
	public void printInfo()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void sync()
	{
		world.renderScene(frameBuffer);
		world.draw(frameBuffer);
		
		frameBuffer.update();
		frameBuffer.display(jframe.getGraphics());
	}
	
	@Override
	public boolean isCloseRequested()
	{
		return isClosing;
	}
	
	@Override
	public boolean isKeyDown(int vkKey)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isKeyJustDown(int vkKey)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getWindowId() {
		return 0;
	}

	@Override
	public int getDisplayWidth()
	{
		return frameBuffer.getOutputWidth();
	}
	
	@Override
	public int getDisplayHeight()
	{
		return frameBuffer.getOutputHeight();
	}
	
	@Override
	public void setViewport(int x, int y, int width, int height)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void beginFrame()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void resetState()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void clear(Color clearColour)
	{
		frameBuffer.clear(clearColour);
	}
	
	@Override
	public void clearDepthBuffer()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public BufferedImage takeScreenshot(int startX, int startY, int width, int height, ImageFormat imageFormat)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Texture createTexture(BufferedImage image, TextureFilter filter)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Texture createTexture(BufferedImage[] mips, TextureFilter filter)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void bindTexture(Texture texture)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Mesh createMesh(Texture texture)
	{
		JpctMesh mesh = new JpctMesh( world, (JpctTexture)texture );
		return mesh;
	}
	
	@Override
	public void setProjectionMatrix(Matrix4f matrix)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setCameraMatrix(Matrix4f matrix, Vector3f lookAt, Vector3f eye, Vector3f up)
	{
		// TODO: OrthoCamera sets the position *way* far out and we look everything
		// behind the far plane.
		// Figure out how to adjust jpct's near/far planes
		
		world.setClippingPlanes(1.0f, 2000);
		
		float fov = (float)Math.PI/180f;
		world.getCamera().setFOV(fov);
		world.getCamera().setFOVLimits(fov, fov);
		
		world.getCamera().setPosition(eye.x, eye.y, eye.z);
		world.getCamera().lookAt(new SimpleVector(lookAt.x, lookAt.y, lookAt.z));
		
/*		
		eye = new Vector3f(50, -120, 100);
		Vector3f target = new Vector3f(50, 0, 50);
		
		world.getCamera().setPosition(eye.x, eye.y, eye.z);
		world.getCamera().lookAt(new SimpleVector(target.x, target.y, target.z));
*/
	}
	
	@Override
	public void beginShape(PrimativeType type)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void colour(float r, float g, float b, float a)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void texCoord(float u, float v)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void vertex(float x, float y, float z)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void endShape()
	{
		// TODO Auto-generated method stub
		
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
	public void enableColourWriting(boolean colourMask, boolean alphaMask)
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
	
	private class WindowHandler implements WindowListener
	{
		@Override
		public void windowClosing(WindowEvent arg)
		{
			isClosing = true;			
		}
		
		@Override
		public void windowClosed(WindowEvent arg) {}
		
		@Override
		public void windowActivated(WindowEvent arg) {}
		
		@Override
		public void windowDeactivated(WindowEvent arg) {}

		@Override
		public void windowDeiconified(WindowEvent arg) {}

		@Override
		public void windowIconified(WindowEvent arg) {}

		@Override
		public void windowOpened(WindowEvent arg) {}
		
	}
}
