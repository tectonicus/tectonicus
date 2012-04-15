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
package tectonicus.renderer;

import java.awt.Point;
import java.awt.Rectangle;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.rasteriser.Rasteriser;
import tectonicus.util.MatrixUtil;
import tectonicus.util.Project;
import tectonicus.util.Vector2f;

public class OrthoCamera implements Camera
{
	private final Rasteriser rasteriser;
	
	private final int windowWidth, windowHeight;
	
	private Vector3f eye;
	private Vector3f lookAt;
	private Vector3f up;
	private Vector3f right;
	
	private float zoom;
	
	public Matrix4f projectionMatrix, cameraMatrix;
	
	private Rectangle viewport;
	
	private Frustum frustum;
	
	public OrthoCamera(Rasteriser rasteriser, final int windowWidth, final int windowHeight)
	{
		this.rasteriser = rasteriser;
		
		this.windowWidth = windowWidth;
		this.windowHeight = windowHeight;
		
		eye = new Vector3f();
		lookAt = new Vector3f();
		up = new Vector3f(0, 1, 0);
		right = new Vector3f(0, 0, 1);
		
		viewport = new Rectangle(0, 0, windowWidth, windowHeight);
		
		frustum = new Frustum();
	}
	
	private static Vector3f calcEyePosition(Vector3f lookAt, final float angleOffsetRads, final float elevationAngleRads)
	{
		final float distance = 1000; // arbitrary, since we're in ortho it doesn't actually matter how far away our camera is
									 // Should be quite large so that our camera is 'above' our geometry though
		final float angle = angleOffsetRads;
		
		// Figure out horizontal distance based on vertical distance and elevation angle
		// Elevation angle of 0 is horizontal, so invert it so 0 is straight up
		final float deelevationAngleRads = ((float)Math.PI/2.0f) - elevationAngleRads;
		final float horizontalDistance = distance * (float)Math.tan(deelevationAngleRads);
		
		// Angle of 0 is looking North
		Vector3f rotatePoint = new Vector3f(lookAt.x, lookAt.y + distance, lookAt.z);
		float offsetX = (float)Math.cos(angle) * horizontalDistance;
		float offsetY = 0;
		float offsetZ = (float)Math.sin(angle) * horizontalDistance;
		
		return new Vector3f(rotatePoint.x + offsetX, rotatePoint.y + offsetY, rotatePoint.z + offsetZ);
	}
	
	public void lookAt(float x, float y, float z, final float zoom, final float angleOffsetRads, final float elevationAngleRads)
	{
		// Store these for later
		this.lookAt.set(x, y, z);
		this.zoom = zoom;
		
		// Recalculate eye position
		eye = calcEyePosition(lookAt, angleOffsetRads, elevationAngleRads);

		// Calculate up and right vectors
		{
			Vector3f forward = new Vector3f(lookAt.x - eye.x, lookAt.y - eye.y, lookAt.z - eye.z);
			forward.normalise();
		
			// Find right and up vectors
			if (elevationAngleRads >= Math.PI/2f)
			{
				// Looking straight down, so need to specially calculate the up vector
				
				// Calculate a dummy eye position as if we were had an elevation of 45'
				Vector3f dummyEye = calcEyePosition(lookAt, angleOffsetRads, (float)Math.PI/4.0f);
				Vector3f dummyForward = new Vector3f(lookAt.x - dummyEye.x, lookAt.y - dummyEye.y, lookAt.z - dummyEye.z);
				dummyForward.normalise();
				
				// Actual up is along forward direction but nudged up
				up = new Vector3f(dummyForward.x, dummyForward.y+1, dummyForward.z);	
			}
			else
			{
				up = new Vector3f(0, 1, 0);
			}
			Vector3f dir = new Vector3f(lookAt.x-eye.x, lookAt.y-eye.y, lookAt.z-eye.z);
			dir.normalise();
		
			Vector3f.cross(dir, up, right);
			Vector3f.cross(right, dir, up);
		
			right.normalise();
			up.normalise();
		}
		
		final float size = this.zoom / 2;
		
		// Create own ortho matrix
		projectionMatrix = MatrixUtil.createOrthoMatrix(-size, size, -size, size, -10000, 10000);
		
		// Create a lookat matrix
		cameraMatrix = MatrixUtil.createLookAt(eye, lookAt, up);
		
		// Extract frustum from created view
		frustum.extract(projectionMatrix, cameraMatrix, windowWidth, windowHeight, viewport);
	}
	
	public void apply()
	{
		rasteriser.setProjectionMatrix(projectionMatrix);
		
		rasteriser.setCameraMatrix(cameraMatrix, lookAt, eye, up);
		
		rasteriser.setViewport(0, 0, windowWidth, windowHeight);
	}
	
	public boolean isVisible(Vector3f point)
	{
		return frustum.isVisible(point);
	}
	
	public int classify(Vector3f worldPos)
	{
		return classify(worldPos.x, worldPos.y, worldPos.z);
	}
	
	
	@Override
	public Vector3f getForward()
	{
		Vector3f forward = new Vector3f();
		
		forward.x = lookAt.x - eye.x;
		forward.y = lookAt.y - eye.y;
		forward.z = lookAt.z - eye.z;
		
		forward.normalise();
		
		return forward;
	}
	
	public Vector3f getUp()
	{
		return new Vector3f(up);
	}
	
	public Vector3f getRight()
	{
		return new Vector3f(right);
	}
	
	public Point project(Vector3f worldPos)
	{
		Vector2f p = projectf(worldPos);
		return new Point(Math.round(p.x), Math.round(p.y));
	}
	
	public Vector2f projectf(Vector3f worldPos)
	{
		return Project.project(worldPos, projectionMatrix, cameraMatrix, viewport);
	}
	
	
	public Vector3f unproject(Vector2f screenPos)
	{
		return Project.unproject(new Vector3f(screenPos.x, screenPos.y, 0), projectionMatrix, cameraMatrix, viewport);
	}
	
	@Override
	public Vector3f[] getClearQuad()
	{
		return frustum.getClearQuad(eye, right, up);
	}
	
	// FIXME: Should return an immutable vector
	@Override
	public Vector3f getEyePosition()
	{
		return eye;
	}
	
	@Override
	public Vector3f[] getFrustumVertices()
	{
		return frustum.getFrustumVertices();
	}
	
	@Override
	public int classify(float worldX, float worldY, float worldZ)
	{
		return frustum.classify(worldX, worldY, worldZ);
	}
	
	public float getVisibleWorldWidth()
	{
		return frustum.getVisibleWorldWidth();
	}
	
	public float getVisibleWorldHeight()
	{
		return frustum.getVisibleWorldHeight();
	}
}
