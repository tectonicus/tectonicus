/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.renderer.Geometry;
import tectonicus.util.Colour4f;

import java.util.IdentityHashMap;
import java.util.Map;

/** Applies the GUI pose as vertices are emitted, so transparent sorting uses the posed geometry too. */
public class GuiGeometry extends Geometry {
	private final Matrix4f transform;
	private final GuiLighting lighting;
	private final Map<Mesh, Mesh> transformedMeshes = new IdentityHashMap<>();

	public GuiGeometry(Rasteriser rasteriser, Matrix4f transform, GuiLighting lighting) {
		super(rasteriser);
		this.transform = transform;
		this.lighting = lighting;
	}

	@Override public Mesh getBaseMesh() { return wrap(super.getBaseMesh()); }
	@Override public Mesh getMesh(Texture texture, MeshType type) { return wrap(super.getMesh(texture, type)); }

	private Mesh wrap(Mesh mesh) {
		return transformedMeshes.computeIfAbsent(mesh, m -> new PosedMesh(m, transform, lighting));
	}

	@RequiredArgsConstructor
	private static class PosedMesh implements Mesh {
		@Delegate private final Mesh mesh;
		private final Matrix4f transform;
		private final GuiLighting lighting;
		private final Vector3f[] positions = new Vector3f[4];
		private final Vector4f[] colours = new Vector4f[4];
		private final float[] us = new float[4], vs = new float[4];
		private int vertexCount;

		@Override public void addVertex(Vector3f position, Vector4f colour, float u, float v) {
			positions[vertexCount] = transform.transformPosition(position, new Vector3f());
			colours[vertexCount] = new Vector4f(colour);
			us[vertexCount] = u;
			vs[vertexCount] = v;
			if (++vertexCount == 4) {
				// Meshes use clockwise fronts. Deriving the normal from posed vertices also
				// handles element rotations and nonuniform GUI scale without cardinal snapping.
				Vector3f normal = new Vector3f(positions[2]).sub(positions[0])
						.cross(new Vector3f(positions[1]).sub(positions[0]));
				if (normal.lengthSquared() > 0)
					normal.normalize();
				float brightness = lighting.brightness(normal);
				for (int i = 0; i < 4; i++) {
					Vector4f c = colours[i];
					mesh.addVertex(positions[i], new Vector4f(c.x * brightness, c.y * brightness, c.z * brightness, c.w), us[i], vs[i]);
				}
				vertexCount = 0;
			}
		}
		@Override public void addVertex(Vector3f position, float u, float v) {
			addVertex(position, new Vector4f(1), u, v);
		}
		@Override public void addVertex(Vector3f position, Colour4f colour, float u, float v) {
			addVertex(position, new Vector4f(colour.r, colour.g, colour.b, colour.a), u, v);
		}
	}
}
