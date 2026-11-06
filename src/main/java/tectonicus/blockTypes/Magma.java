/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.blockTypes;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Version;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

/** Legacy magma has a full-bright lightmap, but still receives directional shading. */
public class Magma implements BlockType {
	@Getter
	private final String name;
	private final SubTexture texture;

	public Magma(String name, SubTexture texture) {
		this.name = name;
		// Animated textures are vertical strips; render the first square frame like SolidBlockType.
		this.texture = texture.texturePackVersion == Version.VERSION_4 ? texture
				: new SubTexture(texture.texture, texture.u0, texture.v0, texture.u1,
						texture.v0 + (float) texture.texture.getWidth() / texture.texture.getHeight());
	}

	@Override
	public boolean isSolid() {
		return true;
	}

	@Override
	public boolean isWater() {
		return false;
	}

	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry) {
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}

	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry) {
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		addFace(world, chunk, mesh, x, y + 1, z, LightFace.Top,
				new Vector3f(x, y + 1, z), new Vector3f(x + 1, y + 1, z), new Vector3f(x + 1, y + 1, z + 1), new Vector3f(x, y + 1, z + 1));
		addFace(world, chunk, mesh, x, y - 1, z, LightFace.Bottom,
				new Vector3f(x, y, z + 1), new Vector3f(x + 1, y, z + 1), new Vector3f(x + 1, y, z), new Vector3f(x, y, z));
		addFace(world, chunk, mesh, x - 1, y, z, LightFace.NorthSouth,
				new Vector3f(x, y + 1, z), new Vector3f(x, y + 1, z + 1), new Vector3f(x, y, z + 1), new Vector3f(x, y, z));
		addFace(world, chunk, mesh, x + 1, y, z, LightFace.NorthSouth,
				new Vector3f(x + 1, y + 1, z + 1), new Vector3f(x + 1, y + 1, z), new Vector3f(x + 1, y, z), new Vector3f(x + 1, y, z + 1));
		addFace(world, chunk, mesh, x, y, z - 1, LightFace.EastWest,
				new Vector3f(x + 1, y + 1, z), new Vector3f(x, y + 1, z), new Vector3f(x, y, z), new Vector3f(x + 1, y, z));
		addFace(world, chunk, mesh, x, y, z + 1, LightFace.EastWest,
				new Vector3f(x, y + 1, z + 1), new Vector3f(x + 1, y + 1, z + 1), new Vector3f(x + 1, y, z + 1), new Vector3f(x, y, z + 1));
	}

	private void addFace(BlockContext world, RawChunk chunk, Mesh mesh, int neighborX, int neighborY, int neighborZ,
			LightFace face, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
		if (world.getBlockType(chunk.getChunkCoord(), neighborX, neighborY, neighborZ).isSolid())
			return;
		float shade = Chunk.getFaceFactor(world.getLightStyle(), face);
		MeshUtil.addQuad(mesh, p0, p1, p2, p3, new Vector4f(shade, shade, shade, 1), texture);
	}
}
