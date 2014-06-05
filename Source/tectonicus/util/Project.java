/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.awt.Rectangle;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Project
{
	private static FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
	private static FloatBuffer cameraBuffer = BufferUtils.createFloatBuffer(16);
	private static IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
	
	public static Vector2f project(org.lwjgl.util.vector.Vector3f worldPos, Matrix4f projectionMatrix, Matrix4f cameraMatrix, Rectangle viewport)
	{
		projectionBuffer.clear();
		projectionMatrix.store(projectionBuffer);
		projectionBuffer.flip();
		
		cameraBuffer.clear();
		cameraMatrix.store(cameraBuffer);
		cameraBuffer.flip();
		
		viewportBuffer.clear();
		viewportBuffer.put((int)viewport.getX()).put((int)viewport.getY()).put((int)viewport.getWidth()).put((int)viewport.getHeight());
		viewportBuffer.flip();
		
		FloatBuffer outBuffer = BufferUtils.createFloatBuffer(16);
		GLU.gluProject(worldPos.x, worldPos.y, worldPos.z, cameraBuffer, projectionBuffer, viewportBuffer, outBuffer);
		
		final float x = outBuffer.get(0);
		
		// Invert y so that origin is top-left
		final float y = (float)viewport.getHeight() - outBuffer.get(1);
		
		return new Vector2f(x, y);
	}
	
	public static Vector3f unproject(Vector3f screenPos, Matrix4f projectionMatrix, Matrix4f cameraMatrix, Rectangle viewport)
	{
		projectionBuffer.clear();
		projectionMatrix.store(projectionBuffer);
		projectionBuffer.flip();
		
		cameraBuffer.clear();
		cameraMatrix.store(cameraBuffer);
		cameraBuffer.flip();
		
		viewportBuffer.clear();
		viewportBuffer.put((int)viewport.getX()).put((int)viewport.getY()).put((int)viewport.getWidth()).put((int)viewport.getHeight());
		viewportBuffer.flip();
		
		final float projX = screenPos.x;
		
		// Invert y so that origin is top-left
		final float projY = (int)viewport.getHeight() - screenPos.y;
		
		final float projZ = screenPos.z;
		
		FloatBuffer outBuffer = BufferUtils.createFloatBuffer(16);
		GLU.gluUnProject(projX, projY, projZ, cameraBuffer, projectionBuffer, viewportBuffer, outBuffer);
		
		final float x = outBuffer.get(0);
		final float y = outBuffer.get(1);
		final float z = outBuffer.get(2);
		
		return new Vector3f(x, y, z);
	}
}
