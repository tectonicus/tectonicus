/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tectonicus.BlockContext;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Geometry baked for one blockstate model entry. World lighting and visibility are deliberately deferred. */
@Getter
public final class BakedBlockStateModel {
	private final BlockModel sourceModel;
	private final int xRotation;
	private final int yRotation;
	private final int zRotation;
	private final boolean uvlock;
	private final List<BakedQuad> quads;

	public BakedBlockStateModel(BlockModel sourceModel, int xRotation, int yRotation, int zRotation, boolean uvlock) {
		this.sourceModel = sourceModel;
		this.xRotation = xRotation;
		this.yRotation = yRotation;
		this.zRotation = zRotation;
		this.uvlock = uvlock;
		this.quads = sourceModel == null ? Collections.emptyList() : bakeQuads(sourceModel, xRotation, yRotation, zRotation, uvlock);
	}

	public void createGeometry(int x, int y, int z, BlockContext world, RawChunk rawChunk, Geometry geometry) {
		MeshUtil.addBakedBlock(world, rawChunk, x, y, z, this, geometry);
	}

	private static List<BakedQuad> bakeQuads(BlockModel model, int xRotation, int yRotation, int zRotation, boolean uvlock) {
		Matrix4f blockRotation = new Matrix4f()
				.translate(0.5f, 0.5f, 0.5f)
				.rotateZ((float) Math.toRadians(-zRotation))
				.rotateY((float) Math.toRadians(-yRotation))
				.rotateX((float) Math.toRadians(-xRotation))
				.translate(-0.5f, -0.5f, -0.5f);

		List<BakedQuad> baked = new ArrayList<>();
		for (BlockElement element : model.getElements()) {
			Matrix4f elementRotation = elementRotation(element);
			float x1 = element.getFrom().x() / 16.0f;
			float y1 = element.getFrom().y() / 16.0f;
			float z1 = element.getFrom().z() / 16.0f;
			float x2 = element.getTo().x() / 16.0f;
			float y2 = element.getTo().y() / 16.0f;
			float z2 = element.getTo().z() / 16.0f;

			addFace(baked, model, element, elementRotation, blockRotation, uvlock, "up", new float[][]{{x1,y2,z1},{x2,y2,z1},{x2,y2,z2},{x1,y2,z2}});
			addFace(baked, model, element, elementRotation, blockRotation, uvlock, "down", new float[][]{{x1,y1,z2},{x2,y1,z2},{x2,y1,z1},{x1,y1,z1}});
			addFace(baked, model, element, elementRotation, blockRotation, uvlock, "north", new float[][]{{x2,y2,z1},{x1,y2,z1},{x1,y1,z1},{x2,y1,z1}});
			addFace(baked, model, element, elementRotation, blockRotation, uvlock, "south", new float[][]{{x1,y2,z2},{x2,y2,z2},{x2,y1,z2},{x1,y1,z2}});
			addFace(baked, model, element, elementRotation, blockRotation, uvlock, "east", new float[][]{{x2,y2,z2},{x2,y2,z1},{x2,y1,z1},{x2,y1,z2}});
			addFace(baked, model, element, elementRotation, blockRotation, uvlock, "west", new float[][]{{x1,y2,z1},{x1,y2,z2},{x1,y1,z2},{x1,y1,z1}});
		}
		return List.copyOf(baked);
	}

	private static Matrix4f elementRotation(BlockElement element) {
		if (element.getRotationX() == 0 && element.getRotationY() == 0 && element.getRotationZ() == 0)
			return new Matrix4f();

		Vector3f origin = new Vector3f(element.getRotationOrigin().x() / 16.0f,
				element.getRotationOrigin().y() / 16.0f, element.getRotationOrigin().z() / 16.0f);
		Matrix4f rotation = new Matrix4f().translate(origin)
				.rotateZ((float) Math.toRadians(element.getRotationZ()))
				.rotateY((float) Math.toRadians(element.getRotationY()))
				.rotateX((float) Math.toRadians(element.getRotationX()));
		if (element.isScaled()) {
			rotation.scale(rescaleFactor(element.getRotationY()) * rescaleFactor(element.getRotationZ()),
					rescaleFactor(element.getRotationX()) * rescaleFactor(element.getRotationZ()),
					rescaleFactor(element.getRotationX()) * rescaleFactor(element.getRotationY()));
		}
		return rotation.translate(-origin.x, -origin.y, -origin.z);
	}

	private static float rescaleFactor(float angle) {
		return 1.0f / (float) Math.cos(Math.toRadians(angle));
	}

	private static void addFace(List<BakedQuad> result, BlockModel model, BlockElement element,
			Matrix4f elementRotation, Matrix4f blockRotation, boolean uvlock, String faceName, float[][] positions) {
		ElementFace face = element.getFaces().get(faceName);
		if (face == null)
			return;

		float[] xyz = new float[12];
		float fudge = model.getName().contains("grass_block") && element.getFaces().size() == 4 ? 0.0001f : 0;
		for (int i = 0; i < 4; i++) {
			float px = positions[i][0], py = positions[i][1], pz = positions[i][2];
			switch (faceName) {
				case "north" -> pz -= fudge;
				case "south" -> pz += fudge;
				case "east" -> px += fudge;
				case "west" -> px -= fudge;
				default -> { }
			}
			Vector3f position = new Vector3f(px, py, pz);
			elementRotation.transformPosition(position);
			blockRotation.transformPosition(position);
			xyz[i * 3] = position.x;
			xyz[i * 3 + 1] = position.y;
			xyz[i * 3 + 2] = position.z;
		}

		float[][] uv = bakeUv(face);
		if (uvlock)
			uv = lockUv(uv, xyz, faceName, blockRotation, face.isExplicitUv(), face);
		float[] uvs = new float[8];
		for (int i = 0; i < 4; i++) {
			uvs[i * 2] = uv[i][0];
			uvs[i * 2 + 1] = uv[i][1];
		}
		String cullDirection = face.getCullFace() == null ? null
				: dominantFace(blockRotation.transformDirection(faceNormal(face.getCullFace())));
		result.add(new BakedQuad(model, face, faceName, cullDirection, xyz, uvs,
				element.getFaces().size() == 4, element.isShaded() || element.getShadeDirectionOverride() != null,
				element.getShadeDirectionOverride()));
	}

	private static float[][] bakeUv(ElementFace face) {
		float u0 = face.getTexture().u0;
		float v0 = face.getTexture().v0;
		float u1 = face.getTexture().u1;
		float v1 = face.getTexture().v1;
		return switch (face.getTextureRotation()) {
			case 90 -> new float[][]{{u0,v1},{u0,v0},{u1,v0},{u1,v1}};
			case 180 -> new float[][]{{u1,v1},{u0,v1},{u0,v0},{u1,v0}};
			case 270 -> new float[][]{{u1,v0},{u1,v1},{u0,v1},{u0,v0}};
			default -> new float[][]{{u0,v0},{u1,v0},{u1,v1},{u0,v1}};
		};
	}

	private static float[][] lockUv(float[][] uv, float[] positions, String faceName, Matrix4f blockRotation,
			boolean explicitUv, ElementFace face) {
		Vector3f sourceNormal = faceNormal(faceName);
		blockRotation.transformDirection(sourceNormal);
		String targetFace = dominantFace(sourceNormal);
		Vector3f targetU = faceU(targetFace);
		Vector3f targetV = faceV(targetFace);
		Vector3f topLeft = faceTopLeft(targetFace);
		float[] uCoords = new float[4];
		float[] vCoords = new float[4];
		float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
		float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < 4; i++) {
			Vector3f delta = new Vector3f(positions[i * 3] - topLeft.x, positions[i * 3 + 1] - topLeft.y,
					positions[i * 3 + 2] - topLeft.z);
			uCoords[i] = delta.dot(targetU);
			vCoords[i] = delta.dot(targetV);
			minU = Math.min(minU, uCoords[i]);
			maxU = Math.max(maxU, uCoords[i]);
			minV = Math.min(minV, vCoords[i]);
			maxV = Math.max(maxV, vCoords[i]);
		}
		if (!explicitUv && face.getTextureRotation() == 0) {
			// Default element UVs represent a projection of the full 16x16 block face. A
			// rotated partial element must keep that texel-to-block-unit scale; mapping
			// only the source rectangle's four corners would stretch it when its width
			// and depth exchange places (as on rotated wall tops).
			float vScale = 1.0f;
			if (face.getTexture().texture != null && face.getTexture().texture.getHeight() > 0)
				vScale = (float) face.getTexture().texture.getWidth() / face.getTexture().texture.getHeight();
			float[][] projected = new float[4][2];
			for (int i = 0; i < 4; i++)
				projected[i] = new float[]{uCoords[i], face.getUvFrameOffset() + vCoords[i] * vScale};
			return projected;
		}
		float[][] adjusted = new float[4][2];
		for (int i = 0; i < 4; i++) {
			boolean highU = Math.abs(uCoords[i] - maxU) < Math.abs(uCoords[i] - minU);
			boolean highV = Math.abs(vCoords[i] - maxV) < Math.abs(vCoords[i] - minV);
			int canonicalCorner = highU ? (highV ? 2 : 1) : (highV ? 3 : 0);
			adjusted[i] = uv[canonicalCorner].clone();
		}
		return adjusted;
	}

	private static Vector3f faceTopLeft(String face) {
		return switch (face) {
			case "up" -> new Vector3f(0, 1, 0);
			case "down" -> new Vector3f(0, 0, 1);
			case "north" -> new Vector3f(1, 1, 0);
			case "south" -> new Vector3f(0, 1, 1);
			case "east" -> new Vector3f(1, 1, 1);
			default -> new Vector3f(0, 1, 0);
		};
	}

	private static Vector3f faceNormal(String face) {
		return switch (face) {
			case "up" -> new Vector3f(0,1,0);
			case "down" -> new Vector3f(0,-1,0);
			case "north" -> new Vector3f(0,0,-1);
			case "south" -> new Vector3f(0,0,1);
			case "east" -> new Vector3f(1,0,0);
			default -> new Vector3f(-1,0,0);
		};
	}

	private static String dominantFace(Vector3f normal) {
		float x = Math.abs(normal.x), y = Math.abs(normal.y), z = Math.abs(normal.z);
		if (y >= x && y >= z) return normal.y >= 0 ? "up" : "down";
		if (x >= z) return normal.x >= 0 ? "east" : "west";
		return normal.z >= 0 ? "south" : "north";
	}

	private static Vector3f faceU(String face) {
		return switch (face) {
			case "up", "down", "south" -> new Vector3f(1,0,0);
			case "north" -> new Vector3f(-1,0,0);
			case "east" -> new Vector3f(0,0,-1);
			default -> new Vector3f(0,0,1);
		};
	}

	private static Vector3f faceV(String face) {
		return switch (face) {
			case "up" -> new Vector3f(0,0,1);
			case "down" -> new Vector3f(0,0,-1);
			default -> new Vector3f(0,-1,0);
		};
	}

	@Getter
	public static final class BakedQuad {
		private final BlockModel model;
		private final ElementFace face;
		private final String sourceFace;
		private final String cullDirection;
		private final float[] positions;
		private final float[] uvs;
		private final boolean grassOverlay;
		private final boolean shaded;
		private final String shadeDirectionOverride;

		private BakedQuad(BlockModel model, ElementFace face, String sourceFace, String cullDirection,
				float[] positions, float[] uvs, boolean grassOverlay, boolean shaded, String shadeDirectionOverride) {
			this.model = model;
			this.face = face;
			this.sourceFace = sourceFace;
			this.cullDirection = cullDirection;
			this.positions = positions;
			this.uvs = uvs;
			this.grassOverlay = grassOverlay;
			this.shaded = shaded;
			this.shadeDirectionOverride = shadeDirectionOverride;
		}

		public float[] getPositions() { return positions.clone(); }
		public float[] getUvs() { return uvs.clone(); }
		public float getPosition(int vertex, int axis) { return positions[vertex * 3 + axis]; }
		public float getUv(int vertex, int axis) { return uvs[vertex * 2 + axis]; }
	}
}
