/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.rasteriser.Mesh;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The 3D shape Minecraft gives flat (generated) item models: the sprite on the front and back of a one pixel thick
 * slab, with side faces around every opaque pixel that borders a transparent one.
 * Coordinates are in model space, 0 to 1, with the front facing south (+Z) like {@code item/generated}.
 */
public final class ExtrudedItemModel {
	private static final float BACK = 7.5f / 16;
	private static final float FRONT = 8.5f / 16;

	/** A face with its corners and texture coordinates (0 to 1 across the sprite), clockwise from its front. */
	public record Quad(Vector3f[] corners, float[] us, float[] vs) { }

	private final List<Quad> quads;

	public ExtrudedItemModel(BufferedImage sprite) {
		quads = build(sprite);
	}

	public List<Quad> getQuads() {
		return quads;
	}

	/** Adds the model to an alpha tested mesh using the given sprite texture. */
	public void addGeometry(Geometry geometry, SubTexture texture, Vector4f colour) {
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		float du = texture.u1 - texture.u0;
		float dv = texture.v1 - texture.v0;
		for (Quad quad : quads) {
			for (int i = 0; i < 4; i++)
				mesh.addVertex(new Vector3f(quad.corners[i]), colour, texture.u0 + du * quad.us[i], texture.v0 + dv * quad.vs[i]);
		}
	}

	private static List<Quad> build(BufferedImage sprite) {
		List<Quad> result = new ArrayList<>();
		result.add(new Quad(new Vector3f[] {v(0, 1, FRONT), v(1, 1, FRONT), v(1, 0, FRONT), v(0, 0, FRONT)},
				new float[] {0, 1, 1, 0}, new float[] {0, 0, 1, 1}));
		result.add(new Quad(new Vector3f[] {v(1, 1, BACK), v(0, 1, BACK), v(0, 0, BACK), v(1, 0, BACK)},
				new float[] {1, 0, 0, 1}, new float[] {0, 0, 1, 1}));

		int width = sprite.getWidth();
		int height = sprite.getHeight();
		for (int py = 0; py < height; py++) {
			for (int px = 0; px < width; px++) {
				if (!opaque(sprite, px, py))
					continue;
				float x0 = (float) px / width, x1 = (float) (px + 1) / width;
				float top = 1 - (float) py / height, bottom = 1 - (float) (py + 1) / height;
				// Sides sample the centre of their pixel, so neighbouring texels never bleed in.
				float u = (px + 0.5f) / width, v = (py + 0.5f) / height;
				if (!opaque(sprite, px, py - 1))
					result.add(side(u, v, new Vector3f(0, 1, 0), v(x0, top, BACK), v(x1, top, BACK), v(x1, top, FRONT), v(x0, top, FRONT)));
				if (!opaque(sprite, px, py + 1))
					result.add(side(u, v, new Vector3f(0, -1, 0), v(x0, bottom, BACK), v(x1, bottom, BACK), v(x1, bottom, FRONT), v(x0, bottom, FRONT)));
				if (!opaque(sprite, px - 1, py))
					result.add(side(u, v, new Vector3f(-1, 0, 0), v(x0, top, FRONT), v(x0, top, BACK), v(x0, bottom, BACK), v(x0, bottom, FRONT)));
				if (!opaque(sprite, px + 1, py))
					result.add(side(u, v, new Vector3f(1, 0, 0), v(x1, top, FRONT), v(x1, top, BACK), v(x1, bottom, BACK), v(x1, bottom, FRONT)));
			}
		}
		return List.copyOf(result);
	}

	private static boolean opaque(BufferedImage sprite, int x, int y) {
		return x >= 0 && y >= 0 && x < sprite.getWidth() && y < sprite.getHeight() && (sprite.getRGB(x, y) >>> 24) != 0;
	}

	/** Orders the corners clockwise as seen from outside the face. */
	private static Quad side(float u, float v, Vector3f outward, Vector3f... corners) {
		Vector3f normal = new Vector3f(corners[2]).sub(corners[0]).cross(new Vector3f(corners[1]).sub(corners[0]));
		if (normal.dot(outward) < 0)
			corners = new Vector3f[] {corners[0], corners[3], corners[2], corners[1]};
		return new Quad(corners, new float[] {u, u, u, u}, new float[] {v, v, v, v});
	}

	private static Vector3f v(float x, float y, float z) {
		return new Vector3f(x, y, z);
	}
}
