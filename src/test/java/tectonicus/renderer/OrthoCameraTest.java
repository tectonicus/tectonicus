/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.RegionCoord;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.util.BoundingBox;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class OrthoCameraTest {
	private static final int SIZE = 512;
	private static final Rasteriser RASTERISER = (Rasteriser)Proxy.newProxyInstance(
			Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class}, (proxy, method, args) -> null);

	@ParameterizedTest
	@CsvSource({
			"0, 0", "-100000, -100000", "-100000, -99000", "-100000, -87655",
			"100000, 112345", "-1000000, -987655", "1000000, 1012345",
			"-29999984, -29999000", "29999984, 29999000", "-29999984, 29999000"
	})
	void neighboringTilesAlignIncludingFractionalBlockVertices(double worldX, double worldZ) {
		for (int angle : new int[]{0, 30, 45, 135, 270}) {
			for (int elevation : new int[]{10, 45, 90}) {
				for (int zoom : new int[]{8, 32, 128}) {
					float a = radians(angle), e = radians(elevation);
					OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
					setup(camera, 0, 0, zoom, a, e);
					double x = worldX + 0.0625, z = worldZ + 0.1875;
					Vector2d expected = camera.projectd(x, 64.5, z);
					int tileX = (int)Math.floor(expected.x / SIZE);
					int tileY = (int)Math.floor(expected.y / SIZE);
					String context = "world=" + x + "," + z + " angle=" + angle + " elevation=" + elevation + " zoom=" + zoom;
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							int tx = tileX + dx, ty = tileY + dy;
							setup(camera, tx, ty, zoom, a, e);
							Vector2d screen = camera.projectd(x, 64.5, z);
							assertEquals(expected.x, screen.x + (double)tx * SIZE, 0.00001, context);
							assertEquals(expected.y, screen.y + (double)ty * SIZE, 0.00001, context);
							// Model the actual fixed-function draw path: local vertices, chunk translation,
							// float modelview matrix, then float projection matrix.
							Vector2d rendered = renderProjection(camera, x, 64.5, z);
							assertEquals(expected.x, rendered.x + (double)tx * SIZE, 0.01, context);
							assertEquals(expected.y, rendered.y + (double)ty * SIZE, 0.01, context);
							assertEquals(zoom, camera.getVisibleWorldWidth());
							assertEquals(zoom, camera.getVisibleWorldHeight());
						}
					}
				}
			}
		}
	}

	@Test
	void tilePlacementDoesNotDependOnPreviousTilesOrCameraSettings() {
		OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
		setup(camera, -800000, 430000, 32, radians(30), radians(45));
		Matrix4f expected = new Matrix4f(camera.cameraMatrix);
		Vector3f expectedOffset = camera.toRenderPosition(-100000, 0, 99000);
		setup(camera, 910000, -910000, 8, radians(270), radians(10));
		camera.lookAt(-29999999.25, 256, 29999999.75, 128, radians(135), radians(90));
		setup(camera, -800000, 430000, 32, radians(30), radians(45));
		assertEquals(expected, camera.cameraMatrix);
		assertEquals(expectedOffset, camera.toRenderPosition(-100000, 0, 99000));
	}

	@ParameterizedTest
	@CsvSource({"0, 0", "-100000, -99000", "29999984, 29999000", "-29999984, -29999000"})
	void cullingAndWorldFrustumUseWorldCoordinates(double x, double z) {
		OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
		setup(camera, 0, 0, 32, radians(45), radians(45));
		Vector2d screen = camera.projectd(x, 64, z);
		setup(camera, (int)Math.floor(screen.x / SIZE), (int)Math.floor(screen.y / SIZE), 32, radians(45), radians(45));
		BoundingBox chunk = new BoundingBox(new Vector3f((float)(Math.floor(x / 16) * 16), 0,
				(float)(Math.floor(z / 16) * 16)), 16, 384, 16);
		assertTrue(chunk.isVisible(camera));
		assertFalse(new BoundingBox(new Vector3f(16000, 0, -16000), 16, 384, 16).isVisible(camera));
		Vector3d[] corners = camera.getWorldFrustumVertices();
		for (int i = 0; i < corners.length; i++) {
			Vector3d corner = corners[i];
			Vector2d projected = camera.projectd(corner.x, corner.y, corner.z);
			assertEquals(i % 2 == 0 ? 0 : SIZE, projected.x, 0.00001);
			assertEquals(i % 4 < 2 ? 0 : SIZE, projected.y, 0.00001);
		}
	}

	@Test
	void configuredElevationsKeepTheTerrainBetweenClippingPlanes() {
		OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
		for (int elevation : new int[]{10, 30, 45, 60, 90}) {
			setup(camera, 0, 0, 32, radians(45), radians(elevation));
			Vector3d near = camera.unprojectd(SIZE / 2.0, SIZE / 2.0);
			Vector3f direction = camera.getForward();
			for (int height : new int[]{0, 64, 256, 384}) {
				double distance = (height - near.y) / direction.y;
				Vector3f point = new Vector3f((float)(near.x + direction.x * distance), height,
						(float)(near.z + direction.z * distance));
				assertEquals(Frustum.INSIDE_CODE, camera.classify(point), "elevation=" + elevation + " height=" + height);
			}
		}
	}

	@Test
	void regionLookupPreservesPositionsJustBelowPositiveAndNegativeBoundaries() {
		assertEquals(new RegionCoord(58592, -58594), RegionCoord.fromWorldCoord(29999616 - 0.0625, -29999616 - 0.0625));
	}

	private static void setup(OrthoCamera camera, int x, int y, int zoom, float angle, float elevation) {
		TileRenderer.setupCameraForTile(camera, new TileCoord(x, y), SIZE, SIZE, angle, elevation, zoom);
	}

	private static float radians(int degrees) {
		return (float)Math.toRadians(degrees);
	}

	private static Vector2d renderProjection(OrthoCamera camera, double x, double y, double z) {
		double chunkX = Math.floor(x / 16) * 16, chunkZ = Math.floor(z / 16) * 16;
		Vector3f offset = camera.toRenderPosition(chunkX, 0, chunkZ);
		Vector3f position = new Vector3f((float)(x - chunkX), (float)y, (float)(z - chunkZ));
		new Matrix4f(camera.cameraMatrix).translate(offset).transformPosition(position);
		camera.projectionMatrix.transformProject(position);
		return new Vector2d((position.x + 1) * SIZE / 2.0, (1 - position.y) * SIZE / 2.0);
	}
}
