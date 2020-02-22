/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.renderer;


import org.joml.Matrix4f;
import org.joml.Vector3f;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.util.MatrixUtil;

import java.awt.Rectangle;

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
		
		forward.normalize();
		
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
		forwards.normalize();
		forwards.cross(up, right);

		projectionMatrix = MatrixUtil.createPerspectiveMatrix(fovInDegs, aspect, near, far);
		cameraMatrix = MatrixUtil.createLookAt(eye, lookAt, up);
		
		frustum.extract(projectionMatrix, cameraMatrix, windowWidth, windowHeight, viewport);
	}

	public void drawWireframe()
	{
		frustum.drawBounds();
	}
	
}
