/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import tectonicus.BlockContext;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.blockregistry.EmissiveBlock;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.MeshUtil.FaceDirection;
import tectonicus.raw.RawChunk;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;
import tectonicus.util.Util;

import java.util.Map;

/** Emits lit terrain quads from the XML block renderers. */
public final class LegacyBlockLighting {
	public static final LegacyBlockLighting FLAT = new LegacyBlockLighting();
	public static final Vector4fc WHITE = new Vector4f(1, 1, 1, 1);

	private final BlockContext world;
	private final RawChunk chunk;
	private final int x, y, z;
	private final LightStyle style;
	private final boolean grayscale;
	private final float nightLightAdjustment;

	private LegacyBlockLighting() {
		this(null, null, 0, 0, 0);
	}

	private LegacyBlockLighting(BlockContext world, RawChunk chunk, int x, int y, int z) {
		this.world = world;
		this.chunk = chunk;
		this.x = x;
		this.y = y;
		this.z = z;
		style = world == null ? null : world.getLightStyle();
		grayscale = world != null && world.useOldGrayscaleLighting();
		nightLightAdjustment = world == null ? 0 : world.getNightLightAdjustment();
	}

	public static LegacyBlockLighting forBlock(BlockContext world, RawChunk chunk, int x, int y, int z) {
		if (!world.isLegacySmoothLit() || emitsLight(world, chunk, x, y, z))
			return FLAT;
		return new LegacyBlockLighting(world, chunk, x, y, z);
	}

	public boolean isSmooth() {
		return world != null;
	}

	private static boolean emitsLight(BlockContext world, RawChunk chunk, int x, int y, int z) {
		String name = chunk.getBlockName(x, y, z);
		if (name != null) {
			BlockStateWrapper model = world.getBlock(chunk, x, y, z);
			var state = chunk.getBlockState(x, y, z);
			if (model != null && name.equals(model.getBlockName()))
				return model.isEmissiveRendering(state) || model.getLightEmission(state) > 0;
			EmissiveBlock emission = EmissiveBlock.forBlockName(name);
			return emission != null && emission.getLightEmission(state == null ? Map.of() : state.getProperties()) > 0;
		}
		// Numeric versions represent lit and unlit variants with separate block IDs.
		return switch (chunk.getBlockId(x, y, z)) {
			case 62, 74, 89, 91, 124, 169, 213 -> true;
			default -> false;
		};
	}

	public void addQuad(Mesh mesh, FaceDirection direction, LightFace shadeFace, Vector3f p0, Vector3f p1, Vector3f p2,
			Vector3f p3, Vector4fc tint, float flatLight, SubTexture texture) {
		if (!isSmooth()) {
			MeshUtil.addQuad(mesh, p0, p1, p2, p3, multiply(tint, flatLight), texture);
			return;
		}
		boolean outside = isBoundary(direction, p0, p1, p2, p3);
		addVertex(mesh, direction, shadeFace, outside, p0, tint, texture.u0, texture.v0);
		addVertex(mesh, direction, shadeFace, outside, p1, tint, texture.u1, texture.v0);
		addVertex(mesh, direction, shadeFace, outside, p2, tint, texture.u1, texture.v1);
		addVertex(mesh, direction, shadeFace, outside, p3, tint, texture.u0, texture.v1);
	}

	public void addQuad(Mesh mesh, FaceDirection direction, LightFace shadeFace, Vector3f p0, Vector3f p1, Vector3f p2,
			Vector3f p3, Vector4fc tint, float flatLight, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3) {
		if (!isSmooth()) {
			MeshUtil.addQuad(mesh, p0, p1, p2, p3, multiply(tint, flatLight), uv0, uv1, uv2, uv3);
			return;
		}
		boolean outside = isBoundary(direction, p0, p1, p2, p3);
		addVertex(mesh, direction, shadeFace, outside, p0, tint, uv0.x, uv0.y);
		addVertex(mesh, direction, shadeFace, outside, p1, tint, uv1.x, uv1.y);
		addVertex(mesh, direction, shadeFace, outside, p2, tint, uv2.x, uv2.y);
		addVertex(mesh, direction, shadeFace, outside, p3, tint, uv3.x, uv3.y);
	}

	private boolean isBoundary(FaceDirection direction, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
		int axis = direction.axis;
		float boundary = blockCoordinate(axis) + (direction.dx + direction.dy + direction.dz > 0 ? 1 : 0);
		return Math.abs(p0.get(axis) - boundary) < 0.0001f && Math.abs(p1.get(axis) - boundary) < 0.0001f
				&& Math.abs(p2.get(axis) - boundary) < 0.0001f && Math.abs(p3.get(axis) - boundary) < 0.0001f;
	}

	private void addVertex(Mesh mesh, FaceDirection direction, LightFace shadeFace, boolean outside, Vector3f position,
			Vector4fc tint, float u, float v) {
		float a = Util.clamp(position.get(direction.tangentA) - blockCoordinate(direction.tangentA), 0, 1);
		float b = Util.clamp(position.get(direction.tangentB) - blockCoordinate(direction.tangentB), 0, 1);
		LightSample light = MeshUtil.getSmoothVertexLight(world, chunk, x, y, z, direction, outside, a, b,
				LightFace.Top, nightLightAdjustment, true);
		Colour4f colour = MeshUtil.getShadedLightColour(light, style, shadeFace, grayscale);
		float ao = MeshUtil.getAmbientOcclusion(world, chunk, x, y, z, direction, outside, a, b, true);
		mesh.addVertex(position, new Vector4f(tint.x() * colour.r * ao, tint.y() * colour.g * ao, tint.z() * colour.b * ao, tint.w()), u, v);
	}

	private int blockCoordinate(int axis) {
		return switch (axis) {
			case 0 -> x;
			case 1 -> y;
			default -> z;
		};
	}

	private static Vector4f multiply(Vector4fc tint, float light) {
		return new Vector4f(tint.x() * light, tint.y() * light, tint.z() * light, tint.w());
	}
}
