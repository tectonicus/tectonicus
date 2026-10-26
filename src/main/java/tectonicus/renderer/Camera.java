/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.renderer;

import org.joml.Vector3d;
import org.joml.Vector3f;

public interface Camera
{
	public void apply();
	
	public Vector3f getEyePosition();
	public Vector3f getForward();

	/** Convert a world position to the coordinates used by the rendering matrices. */
	default Vector3f toRenderPosition(double worldX, double worldY, double worldZ) {
		return new Vector3f((float)worldX, (float)worldY, (float)worldZ);
	}

	default Vector3f getRenderEyePosition() {
		return getEyePosition();
	}
	
	public Vector3f[] getFrustumVertices();

	default Vector3d[] getWorldFrustumVertices() {
		Vector3f[] vertices = getFrustumVertices();
		Vector3d[] result = new Vector3d[vertices.length];
		for (int i = 0; i < vertices.length; i++)
			result[i] = new Vector3d(vertices[i]);
		return result;
	}
	
	/** Vertices in rendering coordinates, ready to submit to the rasteriser. */
	public Vector3f[] getClearQuad();
	
	public int classify(final float worldX, final float worldY, final float worldZ);
}
