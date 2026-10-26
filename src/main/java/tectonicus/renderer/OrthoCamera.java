/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.renderer;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.RawChunk;
import tectonicus.util.MatrixUtil;
import tectonicus.util.Vector2f;

import java.awt.Point;
import java.awt.Rectangle;

public class OrthoCamera implements Camera {
	private final Rasteriser rasteriser;
	private final int windowWidth, windowHeight;
	private final Rectangle viewport;
	private final Frustum frustum = new Frustum();
	private final Vector3d lookAt = new Vector3d();
	private final Vector3d eye = new Vector3d();
	private final Vector3d renderOrigin = new Vector3d();
	private final Vector3d forward = new Vector3d();
	private final Vector3d right = new Vector3d();
	private final Vector3d up = new Vector3d();
	private double eyeDistance;
	private float zoom;

	// These matrices operate on positions relative to renderOrigin.
	public Matrix4f projectionMatrix, cameraMatrix;

	public OrthoCamera(Rasteriser rasteriser, final int windowWidth, final int windowHeight) {
		this.rasteriser = rasteriser;
		this.windowWidth = windowWidth;
		this.windowHeight = windowHeight;
		viewport = new Rectangle(0, 0, windowWidth, windowHeight);
	}

	private void setDirection(float angle, float elevation) {
		// Derive an orthonormal basis from the angles, never from rounded world positions.
		double sinAngle = Math.sin(angle);
		double cosAngle = Math.cos(angle);
		double sinElevation = Math.sin(elevation);
		double cosElevation = Math.cos(elevation);
		if (elevation >= (float)(Math.PI / 2)) {
			sinElevation = 1;
			cosElevation = 0;
		}
		forward.set(-cosAngle * cosElevation, -sinElevation, -sinAngle * cosElevation);
		right.set(sinAngle, 0, -cosAngle);
		right.cross(forward, up);
		eyeDistance = 1000 / sinElevation;
	}

	public void lookAt(double x, double y, double z, final float zoom, final float angle, final float elevation) {
		setDirection(angle, elevation);
		setPosition(x, y, z, zoom);
	}

	/** Position a tile independently of the camera's previous position or frustum. */
	public void lookAtTile(int tileX, int tileY, float zoom, float angle, float elevation) {
		setDirection(angle, elevation);
		double x = right.x * zoom * tileX - up.x * zoom * tileY;
		double y = right.y * zoom * tileX - up.y * zoom * tileY;
		double z = right.z * zoom * tileX - up.z * zoom * tileY;
		// Slide along the viewing direction in one double-precision displacement, keeping
		// a fixed 512-block depth margin above the ground, including shallow views.
		double targetY = -512 * forward.y;
		double displacement = (targetY - y) / forward.y;
		setPosition(x + forward.x * displacement, targetY, z + forward.z * displacement, zoom);
	}

	private void setPosition(double x, double y, double z, float zoom) {
		this.zoom = zoom;
		lookAt.set(x, y, z);
		lookAt.fma(-eyeDistance, forward, eye);
		// Chunk-aligned origins keep mesh translations small and exactly representable.
		renderOrigin.set(Math.floor(x / RawChunk.WIDTH) * RawChunk.WIDTH, 0, Math.floor(z / RawChunk.DEPTH) * RawChunk.DEPTH);
		Vector3d localTarget = new Vector3d(lookAt).sub(renderOrigin);
		Vector3d localEye = localTarget.fma(-eyeDistance, forward, new Vector3d());
		double size = zoom / 2.0;
		projectionMatrix = MatrixUtil.createOrthoMatrix((float)-size, (float)size, (float)-size, (float)size, 1000, 7000);
		cameraMatrix = new Matrix4f(new Matrix4d().lookAt(localEye, localTarget, up));
		frustum.extract(projectionMatrix, cameraMatrix, windowWidth, windowHeight, viewport);
	}

	@Override
	public void apply() {
		rasteriser.setProjectionMatrix(projectionMatrix);
		rasteriser.setCameraMatrix(cameraMatrix, toRenderPosition(lookAt.x, lookAt.y, lookAt.z), getRenderEyePosition(), getUp());
		rasteriser.setViewport(0, 0, windowWidth, windowHeight);
	}

	@Override
	public Vector3f toRenderPosition(double worldX, double worldY, double worldZ) {
		return new Vector3f((float)(worldX - renderOrigin.x), (float)(worldY - renderOrigin.y), (float)(worldZ - renderOrigin.z));
	}

	@Override
	public Vector3f getRenderEyePosition() {
		return toRenderPosition(eye.x, eye.y, eye.z);
	}

	@Override
	public Vector3f getEyePosition() {
		return new Vector3f(eye);
	}

	@Override
	public Vector3f getForward() {
		return new Vector3f(forward);
	}

	public Vector3f getUp() {
		return new Vector3f(up);
	}

	public Vector3f getRight() {
		return new Vector3f(right);
	}

	public int classify(Vector3f worldPos) {
		return classify(worldPos.x, worldPos.y, worldPos.z);
	}

	@Override
	public int classify(float worldX, float worldY, float worldZ) {
		return frustum.classify((float)(worldX - renderOrigin.x), (float)(worldY - renderOrigin.y), (float)(worldZ - renderOrigin.z));
	}

	public Point project(Vector3f worldPos) {
		Vector2f p = projectf(worldPos);
		return new Point(Math.round(p.x), Math.round(p.y));
	}

	public Vector2f projectf(Vector3f worldPos) {
		Vector2d p = projectd(worldPos.x, worldPos.y, worldPos.z);
		return new Vector2f((float)p.x, (float)p.y);
	}

	/** World-to-screen projection without losing sub-block detail at large coordinates. */
	public Vector2d projectd(double worldX, double worldY, double worldZ) {
		double x = worldX - lookAt.x;
		double y = worldY - lookAt.y;
		double z = worldZ - lookAt.z;
		return new Vector2d(windowWidth * (0.5 + (right.x * x + right.y * y + right.z * z) / zoom),
				windowHeight * (0.5 - (up.x * x + up.y * y + up.z * z) / zoom));
	}

	public Vector3f unproject(Vector2f screenPos) {
		return new Vector3f(unprojectd(screenPos.x, screenPos.y));
	}

	public Vector3d unprojectd(double screenX, double screenY) {
		return unproject(screenX, screenY, 1000);
	}

	private Vector3d unproject(double screenX, double screenY, double distance) {
		return new Vector3d(eye).fma(distance, forward)
				.fma((screenX / windowWidth - 0.5) * zoom, right)
				.fma((0.5 - screenY / windowHeight) * zoom, up);
	}

	@Override
	public Vector3f[] getClearQuad() {
		return frustum.getClearQuad(getRenderEyePosition(), getRight(), getUp());
	}

	@Override
	public Vector3d[] getWorldFrustumVertices() {
		return new Vector3d[] {
				unproject(0, 0, 1000), unproject(windowWidth, 0, 1000),
				unproject(0, windowHeight, 1000), unproject(windowWidth, windowHeight, 1000),
				unproject(0, 0, 7000), unproject(windowWidth, 0, 7000),
				unproject(0, windowHeight, 7000), unproject(windowWidth, windowHeight, 7000)
		};
	}

	@Override
	public Vector3f[] getFrustumVertices() {
		Vector3d[] world = getWorldFrustumVertices();
		Vector3f[] result = new Vector3f[world.length];
		for (int i = 0; i < world.length; i++)
			result[i] = new Vector3f(world[i]);
		return result;
	}

	public float getVisibleWorldWidth() {
		return zoom;
	}

	public float getVisibleWorldHeight() {
		return zoom;
	}
}
