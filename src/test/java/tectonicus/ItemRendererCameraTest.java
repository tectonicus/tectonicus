/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.renderer.OrthoCamera;
import tectonicus.util.BoundingBox;
import tectonicus.util.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRendererCameraTest {
	// Bounds of the default 32x32 compass rose: a one block thick slab.
	private static final BoundingBox COMPASS_BOUNDS = new BoundingBox(new Vector3f(-16, 0, -16), 33, 1, 33);

	@ParameterizedTest
	@ValueSource(floats = {30, 45, 60, 80, 89, 90})
	void itemStaysBetweenNearAndFarPlanes(float elevationDegrees) {
		for (float angleDegrees : new float[]{0, 45, 90, 180, 270}) {
			OrthoCamera camera = new OrthoCamera(null, 2048, 2048);
			ItemRenderer.lookAtItem(camera, COMPASS_BOUNDS, 47, (float) Math.toRadians(angleDegrees), (float) Math.toRadians(elevationDegrees));
			Matrix4f mvp = new Matrix4f(camera.projectionMatrix).mul(camera.cameraMatrix);

			for (Vector3f corner : corners(COMPASS_BOUNDS)) {
				Vector3f local = camera.toRenderPosition(corner.x, corner.y, corner.z);
				Vector4f clip = mvp.transform(new Vector4f(local, 1));
				float ndcZ = clip.z / clip.w;
				assertTrue(ndcZ > -1 && ndcZ < 1, "corner " + corner + " clipped at angle " + angleDegrees
						+ ", elevation " + elevationDegrees + " (ndc z " + ndcZ + ")");
			}
		}
	}

	@ParameterizedTest
	@ValueSource(floats = {30, 90})
	void pullingTheTargetBackDoesNotMoveTheImage(float elevationDegrees) {
		float angle = (float) Math.toRadians(90);
		float elevation = (float) Math.toRadians(elevationDegrees);
		OrthoCamera centred = new OrthoCamera(null, 2048, 2048);
		centred.lookAt(COMPASS_BOUNDS.getCenterX(), COMPASS_BOUNDS.getCenterY(), COMPASS_BOUNDS.getCenterZ(), 47, angle, elevation);
		OrthoCamera item = new OrthoCamera(null, 2048, 2048);
		ItemRenderer.lookAtItem(item, COMPASS_BOUNDS, 47, angle, elevation);

		for (Vector3f corner : corners(COMPASS_BOUNDS)) {
			Vector2f expected = centred.projectf(corner);
			Vector2f actual = item.projectf(corner);
			assertEquals(expected.x, actual.x, 0.01f);
			assertEquals(expected.y, actual.y, 0.01f);
		}
	}

	private static Vector3f[] corners(BoundingBox bounds) {
		Vector3f o = bounds.getOrigin();
		Vector3f[] result = new Vector3f[8];
		for (int i = 0; i < 8; i++) {
			result[i] = new Vector3f(o.x + ((i & 1) != 0 ? bounds.getWidth() : 0),
					o.y + ((i & 2) != 0 ? bounds.getHeight() : 0),
					o.z + ((i & 4) != 0 ? bounds.getDepth() : 0));
		}
		return result;
	}
}
