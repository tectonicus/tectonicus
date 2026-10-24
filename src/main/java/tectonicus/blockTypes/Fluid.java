/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

/** Shared static-map geometry for Minecraft's water and lava. */
public abstract class Fluid implements BlockType {
	private static final float SURFACE_OFFSET = 0.001f;
	private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
	private final String name;
	protected final SubTexture stillTexture;
	protected final SubTexture flowTexture;

	protected Fluid(String name, SubTexture stillTexture, SubTexture flowTexture, int frame) {
		this.name = name;
		this.stillTexture = animationFrame(stillTexture, frame);
		this.flowTexture = animationFrame(flowTexture, frame);
	}

	protected static SubTexture animationFrame(SubTexture texture, int frame) {
		// Atlas tiles already describe a single image. Standalone fluids use square frames.
		if (texture.u0 != 0 || texture.u1 != 1 || texture.v0 != 0 || texture.v1 != 1)
			return texture;
		int frames = Math.max(1, texture.texture.getHeight() / texture.texture.getWidth());
		int index = Math.floorMod(Math.max(1, frame) - 1, frames);
		float height = (float) texture.texture.getWidth() / texture.texture.getHeight();
		return new SubTexture(texture.texture, 0, index * height, 1, (index + 1) * height, texture.texturePackVersion);
	}

	@Override
	public String getName() { return name; }

	@Override
	public boolean isSolid() { return false; }

	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}

	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		boolean above = containsFluid(world, rawChunk, x, y + 1, z);
		boolean internalTop = above && renderInternalTop(world);
		boolean bottom = isExposed(world, rawChunk, x, y - 1, z);
		boolean west = isExposed(world, rawChunk, x - 1, y, z);
		boolean east = isExposed(world, rawChunk, x + 1, y, z);
		boolean north = isExposed(world, rawChunk, x, y, z - 1);
		boolean south = isExposed(world, rawChunk, x, y, z + 1);
		if (above && !internalTop && !bottom && !west && !east && !north && !south)
			return;

		float center = height(world, rawChunk, x, y, z);
		float westHeight = height(world, rawChunk, x - 1, y, z);
		float eastHeight = height(world, rawChunk, x + 1, y, z);
		float northHeight = height(world, rawChunk, x, y, z - 1);
		float southHeight = height(world, rawChunk, x, y, z + 1);
		float nw = cornerHeight(world, rawChunk, x - 1, y, z - 1, center, westHeight, northHeight);
		float ne = cornerHeight(world, rawChunk, x + 1, y, z - 1, center, eastHeight, northHeight);
		float se = cornerHeight(world, rawChunk, x + 1, y, z + 1, center, eastHeight, southHeight);
		float sw = cornerHeight(world, rawChunk, x - 1, y, z + 1, center, westHeight, southHeight);

		// A block above only hides the top when the fluid actually reaches its underside.
		if (internalTop || !above && (Math.min(Math.min(nw, ne), Math.min(se, sw)) < 1 || !isOpaque(world, rawChunk, x, y + 1, z))) {
			nw -= SURFACE_OFFSET;
			ne -= SURFACE_OFFSET;
			se -= SURFACE_OFFSET;
			sw -= SURFACE_OFFSET;
			Vector2f flow = flow(world, rawChunk, x, y, z);
			boolean flowing = flow.lengthSquared() > 0;
			SubTexture texture = flowing ? flowTexture : stillTexture;
			Mesh mesh = geometry.getMesh(texture.texture, getMeshType());
			Vector2f[] uv = new Vector2f[4];
			if (flowing) {
				flow.normalize();
				// Rotate a half-size square around the flowing sprite's center.
				int[] dx = {-1, 1, 1, -1};
				int[] dz = {-1, -1, 1, 1};
				for (int i = 0; i < 4; i++)
					uv[i] = uv(texture, 0.5f + (dx[i] * flow.y - dz[i] * flow.x) * 0.25f,
							0.5f + (dx[i] * flow.x + dz[i] * flow.y) * 0.25f);
			} else {
				uv[0] = uv(texture, 0, 0);
				uv[1] = uv(texture, 1, 0);
				uv[2] = uv(texture, 1, 1);
				uv[3] = uv(texture, 0, 1);
			}
			addQuad(mesh, new Vector3f(x, y + nw, z), new Vector3f(x + 1, y + ne, z),
					new Vector3f(x + 1, y + se, z + 1), new Vector3f(x, y + sw, z + 1),
					colour(world, rawChunk, x, y, z, LightFace.Top, internalTop), uv[0], uv[1], uv[2], uv[3], isWater() && !internalTop);
		}

		float base = bottom ? SURFACE_OFFSET : 0;
		if (west)
			addSide(world, rawChunk, geometry, x, y, z, -1, 0, x + SURFACE_OFFSET, z, x + SURFACE_OFFSET, z + 1, nw, sw, base, LightFace.NorthSouth);
		if (east)
			addSide(world, rawChunk, geometry, x, y, z, 1, 0, x + 1 - SURFACE_OFFSET, z + 1, x + 1 - SURFACE_OFFSET, z, se, ne, base, LightFace.NorthSouth);
		if (north)
			addSide(world, rawChunk, geometry, x, y, z, 0, -1, x + 1, z + SURFACE_OFFSET, x, z + SURFACE_OFFSET, ne, nw, base, LightFace.EastWest);
		if (south)
			addSide(world, rawChunk, geometry, x, y, z, 0, 1, x, z + 1 - SURFACE_OFFSET, x + 1, z + 1 - SURFACE_OFFSET, sw, se, base, LightFace.EastWest);
		if (bottom) {
			Mesh mesh = geometry.getMesh(stillTexture.texture, getMeshType());
			MeshUtil.addQuad(mesh, new Vector3f(x, y + base, z + 1), new Vector3f(x + 1, y + base, z + 1),
					new Vector3f(x + 1, y + base, z), new Vector3f(x, y + base, z),
					colour(world, rawChunk, x, y, z, LightFace.Bottom, false), stillTexture);
		}
	}

	private void addSide(BlockContext world, RawChunk chunk, Geometry geometry, int x, int y, int z, int dx, int dz,
			float x0, float z0, float x1, float z1, float h0, float h1, float base, LightFace face) {
		SubTexture texture = sideTexture(world, chunk, x + dx, y, z + dz);
		Mesh mesh = geometry.getMesh(texture.texture, getMeshType());
		addQuad(mesh, new Vector3f(x0, y + h0, z0), new Vector3f(x1, y + h1, z1),
				new Vector3f(x1, y + base, z1), new Vector3f(x0, y + base, z0),
				colour(world, chunk, x, y, z, face, false), uv(texture, 0, (1 - h0) / 2),
				uv(texture, 0.5f, (1 - h1) / 2), uv(texture, 0.5f, 0.5f), uv(texture, 0, 0.5f), isWater() && texture == flowTexture);
	}

	private static void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour,
			Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3, boolean doubleSided) {
		if (doubleSided)
			MeshUtil.addDoubleSidedQuad(mesh, p0, p1, p2, p3, colour, uv0, uv1, uv2, uv3);
		else
			MeshUtil.addQuad(mesh, p0, p1, p2, p3, colour, uv0, uv1, uv2, uv3);
	}

	protected Geometry.MeshType getMeshType() { return Geometry.MeshType.Solid; }

	protected boolean renderInternalTop(BlockContext world) { return false; }

	protected SubTexture sideTexture(BlockContext world, RawChunk chunk, int x, int y, int z) { return flowTexture; }

	protected static float shade(LightFace face) {
		return switch (face) {
			case Top -> 1;
			case Bottom -> 0.5f;
			case NorthSouth -> 0.6f;
			case EastWest -> 0.8f;
		};
	}

	protected Vector4f colour(BlockContext world, RawChunk chunk, int x, int y, int z, LightFace face, boolean internal) {
		float brightness = shade(face);
		return new Vector4f(brightness, brightness, brightness, 1);
	}

	private static Vector2f uv(SubTexture texture, float u, float v) {
		return new Vector2f(texture.u0 + u * (texture.u1 - texture.u0), texture.v0 + v * (texture.v1 - texture.v0));
	}

	protected abstract boolean containsFluid(BlockContext world, RawChunk chunk, int x, int y, int z);

	private static boolean isOpaque(BlockContext world, RawChunk chunk, int x, int y, int z) {
		BlockStateWrapper block = world.getBlock(chunk, x, y, z);
		return block != null && block.isFullOpaqueBlock() || world.getBlockType(chunk, x, y, z).isSolid();
	}

	private boolean isExposed(BlockContext world, RawChunk chunk, int x, int y, int z) {
		return !containsFluid(world, chunk, x, y, z) && !isOpaque(world, chunk, x, y, z);
	}

	protected float ownHeight(BlockContext world, RawChunk chunk, int x, int y, int z) {
		BlockProperties properties = world.getBlockState(chunk.getChunkCoord(), x, y, z);
		int level;
		if (properties != null && properties.containsKey("level")) {
			level = Integer.parseInt(properties.get("level"));
		} else if (x >= 0 && x < RawChunk.WIDTH && z >= 0 && z < RawChunk.DEPTH) {
			level = chunk.getBlockData(x, y, z);
		} else {
			level = world.getBlockData(chunk.getChunkCoord(), x, y, z);
		}
		// Falling fluids (levels 8..15) have the same own height as a source.
		return (level >= 8 ? 8 : 8 - level) / 9.0f;
	}

	private float height(BlockContext world, RawChunk chunk, int x, int y, int z) {
		if (containsFluid(world, chunk, x, y, z))
			return containsFluid(world, chunk, x, y + 1, z) ? 1 : ownHeight(world, chunk, x, y, z);
		BlockStateWrapper block = world.getBlock(chunk, x, y, z);
		return block != null && block.isFullBlock() || world.getBlockType(chunk, x, y, z).isSolid() ? -1 : 0;
	}

	private float cornerHeight(BlockContext world, RawChunk chunk, int x, int y, int z, float center, float side1, float side2) {
		if (center == 1 || side1 == 1 || side2 == 1)
			return 1;
		float diagonal = side1 > 0 || side2 > 0 ? height(world, chunk, x, y, z) : -1;
		if (diagonal == 1)
			return 1;
		float sum = 0;
		int weights = 0;
		for (float height : new float[]{center, side1, side2, diagonal}) {
			if (height >= 0) {
				int weight = height >= 0.8f ? 10 : 1;
				sum += height * weight;
				weights += weight;
			}
		}
		return sum / weights;
	}

	private Vector2f flow(BlockContext world, RawChunk chunk, int x, int y, int z) {
		Vector2f result = new Vector2f();
		float center = ownHeight(world, chunk, x, y, z);
		for (int[] direction : DIRECTIONS) {
			int nx = x + direction[0];
			int nz = z + direction[1];
			float difference = 0;
			if (containsFluid(world, chunk, nx, y, nz)) {
				difference = center - ownHeight(world, chunk, nx, y, nz);
			} else if (!isOtherFluid(world, chunk, nx, y, nz) && height(world, chunk, nx, y, nz) == 0
					&& containsFluid(world, chunk, nx, y - 1, nz)) {
				// Flow over a ledge uses the fluid below the neighboring empty cell.
				difference = center - ownHeight(world, chunk, nx, y - 1, nz) + 8 / 9.0f;
			}
			result.add(direction[0] * difference, direction[1] * difference);
		}
		return result;
	}

	private boolean isOtherFluid(BlockContext world, RawChunk chunk, int x, int y, int z) {
		BlockType type = world.getBlockType(chunk, x, y, z);
		if (type instanceof Fluid || type.isWater())
			return true;
		BlockProperties properties = world.getBlockState(chunk.getChunkCoord(), x, y, z);
		BlockStateWrapper block = world.getBlock(chunk, x, y, z);
		return Water.containsWater(block == null ? type.getName() : block.getBlockName(), properties, block);
	}
}
