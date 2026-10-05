/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import java.util.HashMap;
import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.raw.CushionEntity;
import tectonicus.raw.RawChunk;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;

public class Cushion implements BlockType {
	private static final String[] COLORS = {
		"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
		"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};

	private static final float HEIGHT = 4.0f / 16.0f;
	private static final float TEXTURE_SIZE = 64.0f;

	private final String name;
	private final Map<String, CushionTextures> textures = new HashMap<>();

	public Cushion(String name, TexturePack texturePack) {
		this.name = name;

		SubTexture whiteTexture = findCushionTexture(texturePack, "white");
		CushionTextures croppedWhiteTextures = whiteTexture != null ? new CushionTextures(whiteTexture) : null;

		for (String color : COLORS) {
			SubTexture texture = findCushionTexture(texturePack, color);
			CushionTextures croppedTextures = texture != null ? new CushionTextures(texture) : null;
			if (croppedTextures == null)
				croppedTextures = croppedWhiteTextures;
			if (croppedTextures != null)
				textures.put(color, croppedTextures);
		}
	}

	private SubTexture findCushionTexture(TexturePack texturePack, String color) {
		String path = "assets/minecraft/textures/entity/cushion/" + color + "_cushion.png";
		return texturePack.fileExists(path) ? texturePack.findTexture(path) : null;
	}

	private static class CushionTextures {
		private final SubTexture top;
		private final SubTexture bottom;
		private final SubTexture north;
		private final SubTexture south;
		private final SubTexture west;
		private final SubTexture east;

		private CushionTextures(SubTexture texture) {
			top = crop(texture, 32, 0, 48, 16);
			bottom = crop(texture, 16, 0, 32, 16);
			north = crop(texture, 16, 16, 32, 20);
			south = crop(texture, 48, 16, 64, 20);
			west = crop(texture, 0, 16, 16, 20);
			east = crop(texture, 32, 16, 48, 20);
		}

		private static SubTexture crop(SubTexture texture, int x0, int y0, int x1, int y1) {
			float width = texture.u1 - texture.u0;
			float height = texture.v1 - texture.v0;
			return new SubTexture(texture.texture,
				texture.u0 + width * x0 / TEXTURE_SIZE,
				texture.v0 + height * y0 / TEXTURE_SIZE,
				texture.u0 + width * x1 / TEXTURE_SIZE,
				texture.v0 + height * y1 / TEXTURE_SIZE);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isSolid() {
		return false;
	}

	@Override
	public boolean isWater() {
		return false;
	}

	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}

	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		for (CushionEntity entity : rawChunk.getCushions()) {
			String color = entity.color();
			CushionTextures colorTextures = this.textures.get(color);
			if (colorTextures == null)
				colorTextures = this.textures.get("white");
			if (colorTextures == null)
				continue;

			int blockX = (int)entity.localX();
			int blockY = (int)entity.localY();
			int blockZ = (int)entity.localZ();
			float topLight = world.getLight(rawChunk.getChunkCoord(), blockX, blockY, blockZ, LightFace.Top);
			float northSouthLight = world.getLight(rawChunk.getChunkCoord(), blockX, blockY, blockZ, LightFace.NorthSouth);
			float eastWestLight = world.getLight(rawChunk.getChunkCoord(), blockX, blockY, blockZ, LightFace.EastWest);

			SubMesh cushion = new SubMesh();
			addBox(cushion, colorTextures, topLight, northSouthLight, eastWestLight);
			Mesh mesh = geometry.getMesh(colorTextures.top.texture, Geometry.MeshType.Solid);
			cushion.pushTo(mesh, entity.localX(), entity.localY(), entity.localZ(), SubMesh.Rotation.None, 0);
		}
	}

	private void addBox(SubMesh mesh, CushionTextures textures, float topLight, float northSouthLight, float eastWestLight) {
		Vector4f top = new Vector4f(topLight, topLight, topLight, 1.0f);
		Vector4f northSouth = new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f);
		Vector4f eastWest = new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f);

		mesh.addQuad(new Vector3f(-0.5f, HEIGHT, -0.5f), new Vector3f(0.5f, HEIGHT, -0.5f), new Vector3f(0.5f, HEIGHT, 0.5f), new Vector3f(-0.5f, HEIGHT, 0.5f), top, textures.top);
		mesh.addQuad(new Vector3f(-0.5f, 0, 0.5f), new Vector3f(0.5f, 0, 0.5f), new Vector3f(0.5f, 0, -0.5f), new Vector3f(-0.5f, 0, -0.5f), top, textures.bottom);
		mesh.addQuad(new Vector3f(-0.5f, HEIGHT, -0.5f), new Vector3f(-0.5f, HEIGHT, 0.5f), new Vector3f(-0.5f, 0, 0.5f), new Vector3f(-0.5f, 0, -0.5f), eastWest, textures.west);
		mesh.addQuad(new Vector3f(0.5f, HEIGHT, 0.5f), new Vector3f(0.5f, HEIGHT, -0.5f), new Vector3f(0.5f, 0, -0.5f), new Vector3f(0.5f, 0, 0.5f), eastWest, textures.east);
		mesh.addQuad(new Vector3f(0.5f, HEIGHT, -0.5f), new Vector3f(-0.5f, HEIGHT, -0.5f), new Vector3f(-0.5f, 0, -0.5f), new Vector3f(0.5f, 0, -0.5f), northSouth, textures.north);
		mesh.addQuad(new Vector3f(-0.5f, HEIGHT, 0.5f), new Vector3f(0.5f, HEIGHT, 0.5f), new Vector3f(0.5f, 0, 0.5f), new Vector3f(-0.5f, 0, 0.5f), northSouth, textures.south);
	}
}
