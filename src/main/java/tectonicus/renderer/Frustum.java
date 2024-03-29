/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.renderer;

import java.awt.Rectangle;


import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.joml.Vector3f;

import tectonicus.util.Util;
import tectonicus.util.Plane;
import tectonicus.util.Project;

public class Frustum
{
	public static final int INSIDE_CODE	= 0;
	public static final int LEFT_CODE	= 1 << 0;
	public static final int RIGHT_CODE	= 1 << 1;
	public static final int TOP_CODE	= 1 << 2;
	public static final int BOTTOM_CODE	= 1 << 3;
	public static final int NEAR_CODE	= 1 << 4;
	public static final int FAR_CODE	= 1 << 5;
	
	private static final int LEFT_PLANE = 0;
	private static final int RIGHT_PLANE = 1;
	private static final int TOP_PLANE = 2;
	private static final int BOTTOM_PLANE = 3;
	private static final int NEAR_PLANE = 4;
	private static final int FAR_PLANE = 5;
	
	private Plane[] planes;
	
	private Vector3f[] points;
	
	private float visibleWorldWidth, visibleWorldHeight;
	
	public Frustum()
	{
		planes = new Plane[6];
		for (int i=0; i<planes.length; i++)
		{
			planes[i] = new Plane();
		}
		
		points = new Vector3f[8];
		for (int i=0; i<points.length; i++)
		{
			points[i] = new Vector3f();
		}
	}
	
	public void extract(Matrix4f projectionMatrix, Matrix4f cameraMatrix, final int windowWidth, final int windowHeight, Rectangle viewport)
	{
		// unproject to find corners of the view frustum
		Vector3f topLeft = Project.unproject(new Vector3f(0, 0, 0), projectionMatrix, cameraMatrix, viewport);
		Vector3f topRight = Project.unproject(new Vector3f(windowWidth, 0, 0), projectionMatrix, cameraMatrix, viewport);
		Vector3f bottomLeft = Project.unproject(new Vector3f(0, windowHeight, 0), projectionMatrix, cameraMatrix, viewport);
		Vector3f bottomRight = Project.unproject(new Vector3f(windowWidth, windowHeight, 0), projectionMatrix, cameraMatrix, viewport);
		
		Vector3f centerNear = Project.unproject(new Vector3f(windowWidth/2.0f, windowHeight/2.0f, 0), projectionMatrix, cameraMatrix, viewport);
		Vector3f centerFar = Project.unproject(new Vector3f(windowWidth/2.0f, windowHeight/2.0f, 1), projectionMatrix, cameraMatrix, viewport);
		
		points[0] = topLeft;
		points[1] = topRight; 
		points[2] = bottomLeft;
		points[3] = bottomRight;
		points[4] = Project.unproject(new Vector3f(0, 0, 1), projectionMatrix, cameraMatrix, viewport);
		points[5] = Project.unproject(new Vector3f(windowWidth, 0, 1), projectionMatrix, cameraMatrix, viewport);
		points[6] = Project.unproject(new Vector3f(0, windowHeight, 1), projectionMatrix, cameraMatrix, viewport);
		points[7] = Project.unproject(new Vector3f(windowWidth, windowHeight, 1), projectionMatrix, cameraMatrix, viewport);
		
		Vector3f screenUp = new Vector3f(topLeft.x-bottomLeft.x, topLeft.y-bottomLeft.y, topLeft.z-bottomLeft.z);
		Vector3f screenRight = new Vector3f(topRight.x-topLeft.x, topRight.y-topLeft.y, topRight.z-topLeft.z);
		
		// Extract planes
	//	planes[LEFT_PLANE].set(topLeft, new Vector3f(topRight.x-topLeft.x, topRight.y-topLeft.y, topRight.z-topLeft.z));
		Vector3f leftNormal = cross( diff(points[4], topLeft), screenUp );
		planes[LEFT_PLANE].set(topLeft, leftNormal);
		
	//	planes[RIGHT_PLANE].set(topRight, new Vector3f(topLeft.x-topRight.x, topLeft.y-topRight.y, topLeft.z-topRight.z));
		Vector3f rightNormal = cross( screenUp, diff(points[5], topRight) );
		planes[RIGHT_PLANE].set(topRight, rightNormal);
		
	//	planes[TOP_PLANE].set(topLeft, new Vector3f(bottomLeft.x-topLeft.x, bottomLeft.y-topLeft.y, bottomLeft.z-topLeft.z));
		Vector3f topNormal = cross( diff(points[4], topLeft), screenRight );
		planes[TOP_PLANE].set(topLeft, topNormal);
		
	//	planes[BOTTOM_PLANE].set(bottomLeft, new Vector3f(topLeft.x-bottomLeft.x, topLeft.y-bottomLeft.y, topLeft.z-bottomLeft.z));
		Vector3f bottomNormal = cross( screenRight, diff(points[6], bottomRight) );
		planes[BOTTOM_PLANE].set(bottomLeft, bottomNormal);
		
		Vector3f forwards = diff(centerFar, centerNear);
		planes[NEAR_PLANE].set(topLeft, forwards);
		
		Vector3f backwards = diff(centerNear, centerFar);
		planes[FAR_PLANE].set(points[4], backwards);
		
		// Extract view size
		visibleWorldWidth = Util.separation(topLeft, topRight);
		visibleWorldHeight = Util.separation(topLeft, bottomLeft);
	}
	
	private static Vector3f diff(Vector3f lhs, Vector3f rhs)
	{
		return new Vector3f( lhs.x - rhs.x, lhs.y - rhs.y, lhs.z - rhs.z );
	}
	
	private static Vector3f cross(Vector3f lhs, Vector3f rhs)
	{
		Vector3f normLeft = new Vector3f(lhs);
		normLeft.normalize();
		Vector3f normRight = new Vector3f(rhs);
		normRight.normalize();
		
		Vector3f res = new Vector3f();
		normLeft.cross(normRight, res);
		
		return res;
	}
	
	public boolean isVisible(Vector3f point)
	{
		for (Plane p : planes)
		{
			if (!p.isInside(point))
				return false;
		}
		return true;
	}
	
	public int classify(final float worldX, final float worldY, final float worldZ)
	{
		int code = INSIDE_CODE;
		
		final float actualX = worldX;
		final float actualY = worldY;
		final float actualZ = worldZ;
		
		if (!planes[LEFT_PLANE].isInside(actualX, actualY, actualZ))
			code |= LEFT_CODE;
		else if (!planes[RIGHT_PLANE].isInside(actualX, actualY, actualZ))
			code |= RIGHT_CODE;
		
		if (!planes[TOP_PLANE].isInside(actualX, actualY, actualZ))
			code |= TOP_CODE;
		else if (!planes[BOTTOM_PLANE].isInside(actualX, actualY, actualZ))
			code |= BOTTOM_CODE;
		
		if (!planes[NEAR_PLANE].isInside(actualX, actualY, actualZ))
			code |= NEAR_CODE;
		else if (!planes[FAR_PLANE].isInside(actualX, actualY, actualZ))
			code |= FAR_CODE;
		
		return code;
	}
	
	public float getVisibleWorldWidth() { return visibleWorldWidth; }
	public float getVisibleWorldHeight() { return visibleWorldHeight; }
	
	public Vector3f[] getFrustumVertices()
	{
		Vector3f[] result = new Vector3f[ points.length ];
		for (int i=0; i<points.length; i++)
			result[i] = new Vector3f(points[i]);
		return result;
	}
	
	public Vector3f[] getClearQuad(Vector3f eye, Vector3f right, Vector3f up)
	{
		Vector3f[] result = new Vector3f[4];
		
		result[0] = new Vector3f(eye);
		result[1] = new Vector3f(eye);
		result[2] = new Vector3f(eye);
		result[3] = new Vector3f(eye);
		
		final float rightScale = getVisibleWorldWidth();
		Vector3f rightScaled = new Vector3f(right.x * rightScale, right.y * rightScale, right.z * rightScale);
		
		final float upScale = getVisibleWorldHeight();
		Vector3f upScaled = new Vector3f(up.x * upScale, up.y * upScale, up.z * upScale);
		
		result[0].add(upScaled);
		result[0].sub(rightScaled);

		result[1].add(upScaled);
		result[1].add(rightScaled);

		result[2].sub(upScaled);
		result[2].add(rightScaled);

		result[3].sub(upScaled);
		result[3].sub(rightScaled);
		
		return result;
	}
	
	public void drawBounds()
	{
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
		GL11.glBegin(GL11.GL_LINES);
		{
			GL11.glColor4f(0f, 1f, 0f, 1f);
			
			// Top points
			lineBetween(0, 1);
			lineBetween(1, 2);
			lineBetween(2, 3);
			lineBetween(3, 0);
			
			// Bottom points
			lineBetween(4, 5);
			lineBetween(5, 6);
			lineBetween(6, 7);
			lineBetween(7, 4);
			
			// Vertical lines
			lineBetween(0, 4);
			lineBetween(1, 5);
			lineBetween(2, 6);
			lineBetween(3, 7);
		}
		GL11.glEnd();
	}
	
	private void lineBetween(int start, int end)
	{
		GL11.glVertex3f(points[start].x, points[start].y, points[start].z);
		GL11.glVertex3f(points[end].x, points[end].y, points[end].z);
	}
}
