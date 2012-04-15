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
package tectonicus;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.ViewUtil.Viewpoint;
import tectonicus.configuration.Configuration;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.raw.RawChunk;
import tectonicus.raw.RawSign;
import tectonicus.renderer.Camera;
import tectonicus.renderer.OrthoCamera;
import tectonicus.renderer.PerspectiveCamera;
import tectonicus.world.World;

public class InteractiveRenderer
{
	private enum ViewMode
	{
		OrthoView,
		PerspectiveView
	}
	
	private OrthoCamera orthoCamera;
	private PerspectiveCamera perspectiveCamera;
	
	private Rasteriser rasteriser;
	
	private ViewMode viewMode;
	
	private Vector3f orthoCamPosition;
	private float orthoZoom;
	private float orthoAngleOffset;
	
	private RawSign perspectiveSign;
	
	private ArrayList<RawSign> views;
	private int currentViewIndex;
	
	public InteractiveRenderer(Configuration args, final int displayWidth, final int displayHeight) throws LWJGLException
	{
		rasteriser = RasteriserFactory.createRasteriser(args.getRasteriserType(), DisplayType.Window, displayWidth, displayHeight, 24, 8, 8, 4);
		System.out.println("Using rasteriser: "+rasteriser);
		rasteriser.printInfo();
		
		viewMode = ViewMode.OrthoView;
		
		orthoCamPosition = new Vector3f();
		
		orthoCamera = new OrthoCamera(rasteriser, displayWidth, displayHeight);
		perspectiveCamera = new PerspectiveCamera(rasteriser, displayWidth, displayHeight);
		
		views = new ArrayList<RawSign>();
	}
	
	public void destroy()
	{
		rasteriser.destroy();
	}
	
	public Rasteriser getRasteriser()
	{
		return rasteriser;
	}
	
	private void updateOrthoCamera(BlockContext world)
	{
		final float zoomInc = 0.5f;
		
		if (rasteriser.isKeyDown(KeyEvent.VK_LEFT))
		{
			Vector3f right = orthoCamera.getRight();
			orthoCamPosition.x -= right.x * orthoCamera.getVisibleWorldWidth() / 8;
			orthoCamPosition.z -= right.z * orthoCamera.getVisibleWorldWidth() / 8;
		}
		if (rasteriser.isKeyDown(KeyEvent.VK_RIGHT))
		{
			Vector3f right = orthoCamera.getRight();
			orthoCamPosition.x += right.x * orthoCamera.getVisibleWorldWidth() / 8;
			orthoCamPosition.z += right.z * orthoCamera.getVisibleWorldWidth() / 8;
			
		}
		if (rasteriser.isKeyDown(KeyEvent.VK_UP))
		{
			Vector3f up = orthoCamera.getUp();
			orthoCamPosition.x += up.x * orthoCamera.getVisibleWorldHeight() / 8;
			orthoCamPosition.y += up.y * orthoCamera.getVisibleWorldHeight() / 8;
			orthoCamPosition.z += up.z * orthoCamera.getVisibleWorldHeight() / 8;
		}
		if (rasteriser.isKeyDown(KeyEvent.VK_DOWN))
		{
			Vector3f up = orthoCamera.getUp();
			orthoCamPosition.x -= up.x * orthoCamera.getVisibleWorldHeight() / 8;
			orthoCamPosition.y -= up.y * orthoCamera.getVisibleWorldHeight() / 8;
			orthoCamPosition.z -= up.z * orthoCamera.getVisibleWorldHeight() / 8;
		}
		if (rasteriser.isKeyDown(KeyEvent.VK_MINUS))
		{
			orthoZoom += zoomInc;
			if (orthoZoom > 300)
				orthoZoom = 300;
		}
		if (rasteriser.isKeyDown(KeyEvent.VK_EQUALS))
		{
			orthoZoom -= zoomInc;
			if (orthoZoom < 0.1f)
				orthoZoom = 0.1f;
		}
		
		if (rasteriser.isKeyDown(KeyEvent.VK_Q))
		{
			orthoAngleOffset += 0.1f;
		}
		if (rasteriser.isKeyDown(KeyEvent.VK_E))
		{
			orthoAngleOffset -= 0.1f;
		}
	}
	private void updatePerspectiveCamera()
	{
		if (rasteriser.isKeyJustDown(KeyEvent.VK_EQUALS))
		{
			currentViewIndex++;
			if (currentViewIndex >= views.size())
				currentViewIndex = 0;
			
			perspectiveSign = views.get(currentViewIndex);
		}
	}
	
	private void update(World world)
	{
		if (viewMode == ViewMode.OrthoView)
		{
			updateOrthoCamera(world);
		}
		else if (viewMode == ViewMode.PerspectiveView)
		{
			updatePerspectiveCamera();
		}
		
		if (rasteriser.isKeyJustDown(KeyEvent.VK_P))
		{
			System.out.println("Toggling view mode");
		
			if (viewMode == ViewMode.OrthoView)
			{
				// Find all views
				views.clear();
				RawSign[] signs = world.getLoadedSigns();
				for (RawSign s : signs)
				{
					if (s.text1.trim().startsWith("#view"))
					{
						views.add(s);
					}
				}
				
				RawSign nearest = null;
				float currentDist = Float.MAX_VALUE;
				
				if (views.size() > 0)
				{
					nearest = views.get(0);
					currentViewIndex = 0;
				}
				for (int i=0; i<views.size(); i++)
				{
					RawSign s = views.get(i);
					
					Vector3f eye = orthoCamera.getEyePosition();
					
					final float dx = eye.x - s.x;
					final float dy = eye.y - s.y;
					final float dz = eye.z - s.z;
					final float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
					
					if (dist < currentDist)
					{
						nearest = s;
						currentDist = dist;
						currentViewIndex = i;
					}
				}
				
				if (nearest != null)
				{
					System.out.println("now viewing from "+nearest);
					
					viewMode = ViewMode.PerspectiveView;
					perspectiveSign = nearest;
				}
			}
			else if (viewMode == ViewMode.PerspectiveView)
			{
				viewMode = ViewMode.OrthoView;
			}
		}
		
		if (rasteriser.isKeyDown(KeyEvent.VK_SPACE))
		{
			world.dumpMemStats();
		}
		
		if (rasteriser.isKeyDown(KeyEvent.VK_F))
		{
			world.flushGeometryCache();
		}
	}
	
	public void display(World world)
	{
		orthoCamPosition.x = world.getSpawnPosition().x;
		orthoCamPosition.y = world.getSpawnPosition().y;
		orthoCamPosition.z = world.getSpawnPosition().z;
		
		orthoZoom = 32;
		orthoAngleOffset = (float)Math.PI / 4.0f;
		
		while (!rasteriser.isCloseRequested())
		{
			rasteriser.beginFrame();
			
			update(world);
			
			rasteriser.resetState();
			rasteriser.clear(new Color(100, 128, 255, 0));
			rasteriser.clear(new Color(50, 50, 50, 0));
			
			Camera activeCamera = null;
			if (viewMode == ViewMode.OrthoView)
			{
				float cameraElevation = (float)Math.PI / 4.0f; // todo: should come from config (first layer?)
				
				orthoCamera.lookAt(orthoCamPosition.x, orthoCamPosition.y, orthoCamPosition.z, orthoZoom, orthoAngleOffset, cameraElevation);
				
				activeCamera = orthoCamera;
			}
			else if (viewMode == ViewMode.PerspectiveView)
			{
			//	Vector3f eye = new Vector3f(perspectiveSign.x, perspectiveSign.y + 4.0f, perspectiveSign.z);
			//	Vector3f up = new Vector3f(0, 1, 0);
			//	Vector3f lookAt = new Vector3f(eye.x + 1.0f, eye.y, eye.z + 1.0f);
				
				// TODO: Some duplication between here and TileRenderer
				// Should commanalise extracting a view pos + angle from a sign
			/*	final float angleDeg = 90 / 4.0f * perspectiveSign.data - 90 + perspectiveAngleOffset;
				final float angleRad = angleDeg / 360f * 2.0f * (float)Math.PI;
				
				Vector3f eye = new Vector3f(perspectiveSign.x + 0.5f, perspectiveSign.y + 0.5f, perspectiveSign.z + 0.5f);
				Vector3f up = new Vector3f(0, 1, 0);
				Vector3f forward = new Vector3f((float)Math.cos(angleRad), 0, (float)Math.sin(angleRad));
				Vector3f lookAt = new Vector3f(eye.x + forward.x, eye.y + forward.y, eye.z + forward.z);
			*/	
				Viewpoint view = ViewUtil.findView(new tectonicus.world.Sign(perspectiveSign));
				perspectiveCamera = ViewUtil.createCamera(rasteriser, view, 300);
				
			//	perspectiveCamera.lookAt(eye, lookAt, up, 90.0f, 1.0f, 0.1f, 100f);
				
				activeCamera = perspectiveCamera;
			}
			
			activeCamera.apply();
			
			drawAxies(rasteriser);
			
			drawChunkCheckerboard(rasteriser);
			
			world.draw(activeCamera, true, false);
			
			rasteriser.sync();
		}
	}
	
	public static void drawAxies(Rasteriser rasteriser)
	{
		rasteriser.beginShape(PrimativeType.Lines);
		{
			// Red x axis

			rasteriser.colour(1, 0, 0, 1);
			rasteriser.vertex(0, 0, 0);
			rasteriser.vertex(16, 0, 0);
			rasteriser.vertex(0, 128, 0);
			rasteriser.vertex(16, 128, 0);
			
			// Green y axis
			rasteriser.colour(0, 1, 0, 1);
			rasteriser.vertex(0, 0, 0);
			rasteriser.vertex(0, 128, 0);
			
			// Blue z axis
			rasteriser.colour(0, 0, 1, 1);
			rasteriser.vertex(0, 0, 0);
			rasteriser.vertex(0, 0, 16);
			rasteriser.vertex(0, 128, 0);
			rasteriser.vertex(0, 128, 16);			
		}
		rasteriser.endShape();
	}
	
	public static void drawChunkCheckerboard(Rasteriser rasteriser)
	{
		Mesh mesh = rasteriser.createMesh(null);
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				
				float r, g, b;
				r = g = b = 1;
				if (x == 0 && z == 0)
				{
					r = 1;
					g = 1;
					b = 0;
				}
				else if (x == 0)
				{
					r = 1;
					g = 0;
					b = 0;
				}
				else if (z == 0)
				{
					r = 0;
					g = 0;
					b = 1;
				}
				else
				{
					r = g = b = (x + z) % 2 == 0? 1 : 0;
				}
				
				Vector4f colour = new Vector4f(r, g, b, 1);
				
				mesh.addVertex(new Vector3f(x,   0, z),   colour, 0, 0);
				mesh.addVertex(new Vector3f(x+1, 0, z),   colour, 0, 0);
				mesh.addVertex(new Vector3f(x+1, 0, z+1), colour, 0, 0);
				mesh.addVertex(new Vector3f(x,   0, z+1), colour, 0, 0);
			}
		}
		
		mesh.finalise();
		mesh.bind();
		mesh.draw(0, 0, 0);
		
	//	mesh.destroy();
	}
}
