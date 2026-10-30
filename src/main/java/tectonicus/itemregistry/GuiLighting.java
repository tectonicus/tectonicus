/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.itemregistry;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GuiLighting {
	private final Vector3f light0;
	private final Vector3f light1;

	public GuiLighting(boolean sideLit, Matrix4fc guiToWorld) {
		Matrix4f lights = sideLit
				? new Matrix4f().scaling(1, -1, 1)
						.rotateYXZ(radians(62), radians(185.5f), 0)
						.rotateYXZ(radians(-22.5f), radians(135), 0)
				: new Matrix4f().rotationY(radians(-22.5f)).rotateX(radians(135));
		// Minecraft's GUI pose flips Y for screen coordinates; our geometry stays Y-up.
		lights = new Matrix4f(guiToWorld).scale(1, -1, 1).mul(lights);
		light0 = lights.transformDirection(new Vector3f(0.2f, 1, -0.7f).normalize());
		light1 = lights.transformDirection(new Vector3f(-0.2f, 1, 0.7f).normalize());
	}

	public float brightness(Vector3fc normal) {
		float diffuse = Math.max(0, light0.dot(normal)) + Math.max(0, light1.dot(normal));
		return Math.min(1, 0.4f + 0.6f * diffuse);
	}

	private static float radians(float degrees) { return (float) Math.toRadians(degrees); }
}
