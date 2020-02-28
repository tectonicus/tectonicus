/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Rectangle;

@UtilityClass
public class Project
{
	public static Vector2f project(Vector3f worldPos, Matrix4f projectionMatrix, Matrix4f cameraMatrix, Rectangle viewport)
	{
		Matrix4f projMatrix = new Matrix4f();
		projectionMatrix.mul(cameraMatrix, projMatrix);
		Vector3f windowCoords = new Vector3f();
		projMatrix.project(worldPos.x, worldPos.y, worldPos.z,
				new int[] {(int)viewport.getX(), (int)viewport.getY(), (int)viewport.getWidth(), (int)viewport.getHeight()}, windowCoords);

		return new Vector2f(windowCoords.x, (float)viewport.getHeight() - windowCoords.y);
	}
	
	public static Vector3f unproject(Vector3f screenPos, Matrix4f projectionMatrix, Matrix4f cameraMatrix, Rectangle viewport)
	{
		final float projX = screenPos.x;
		
		// Invert y so that origin is top-left
		final float projY = (int)viewport.getHeight() - screenPos.y;
		
		final float projZ = screenPos.z;

		Matrix4f projMatrix = new Matrix4f();
		projectionMatrix.mul(cameraMatrix, projMatrix);
		Vector3f coords = new Vector3f();
		projMatrix.unproject(projX, projY, projZ,
				new int[] {(int)viewport.getX(), (int)viewport.getY(), (int)viewport.getWidth(), (int)viewport.getHeight()}, coords);

		return coords;
	}
}
