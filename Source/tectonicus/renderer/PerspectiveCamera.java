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

import java.awt.Rectangle;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.rasteriser.Rasteriser;
import tectonicus.util.MatrixUtil;

public class PerspectiveCamera implements Camera
{
	private final Rasteriser rasteriser;
	
	private final int windowWidth, windowHeight;
	
	private Rectangle viewport;
	
	private Vector3f eye, right, up;
	private Vector3f lookAt;
	
	private Matrix4f projectionMatrix, cameraMatrix;
	
	private Frustum frustum;
	
	public PerspectiveCamera(Rasteriser rasteriser, final int windowWidth, final int windowHeight)
	{
		this.rasteriser = rasteriser;
		
		this.windowWidth = windowWidth;
		
		this.windowHeight = windowHeight;
	
		this.viewport = new Rectangle(0, 0, windowWidth, windowHeight);
		
		this.eye = new Vector3f();
		this.right = new Vector3f();
		this.up = new Vector3f();
		
		this.lookAt = new Vector3f();
		
		this.frustum = new Frustum();
	}
	
	@Override
	public void apply()
	{
		rasteriser.setProjectionMatrix(projectionMatrix);
		
		rasteriser.setCameraMatrix(cameraMatrix, lookAt, eye, up);
	}
	
	@Override
	public Vector3f getEyePosition()
	{
		return eye;
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
	
	@Override
	public Vector3f[] getFrustumVertices()
	{
		return frustum.getFrustumVertices();
	}

	@Override
	public Vector3f[] getClearQuad()
	{
		return frustum.getClearQuad(eye, right, up);
	}

	@Override
	public int classify(final float worldX, final float worldY, final float worldZ)
	{
		return frustum.classify(worldX, worldY, worldZ);
	}


	public void lookAt(Vector3f eye, Vector3f lookAt, Vector3f up, final float fovInDegs, final float aspect, final float near, final float far)
	{
		this.eye.set(eye);
		this.up.set(up);
		this.lookAt.set(lookAt);
		
	//	Vector3f.cross(eye, up, right); // TODO: Shouldn't this be 'forwards' not 'eye'?
		
		Vector3f forwards = new Vector3f(lookAt.x-eye.x, lookAt.y-eye.y, lookAt.z-eye.z);
		forwards.normalise();
		Vector3f.cross(forwards, up, right);
		
		projectionMatrix = MatrixUtil.createPerspectiveMatrix(fovInDegs, aspect, near, far);
		cameraMatrix = MatrixUtil.createLookAt(eye, lookAt, up);
		
		frustum.extract(projectionMatrix, cameraMatrix, windowWidth, windowHeight, viewport);
	}

	public void drawWireframe()
	{
		frustum.drawBounds();
	}
	
}
