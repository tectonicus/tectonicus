/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class EnchantmentTable implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, bottom;
	private final String id;
	private final SubTexture book;
	
	public EnchantmentTable(String name, SubTexture top, SubTexture side, SubTexture bottom)
	{
		this(name, "", top, side, bottom, null);
	}

	public EnchantmentTable(String name, String id, SubTexture top, SubTexture side, SubTexture bottom, SubTexture book)
	{
		this.name = name;
		this.id = id;
		this.book = book;
		if (!id.isEmpty()) {
			this.top = top;
			this.side = side;
			this.bottom = bottom;
			return;
		}
		
		if (side.texturePackVersion != VERSION_4)
		{
			final float topTexel = 1.0f / top.texture.getHeight();
			final float sideTexel = 1.0f / side.texture.getHeight();
			final float topTile = topTexel * top.texture.getWidth();
			final float sideTile = sideTexel * side.texture.getWidth();
			this.top = new SubTexture(top.texture, top.u0, top.v0, top.u1, top.v0+topTile);
			this.side = new SubTexture(side.texture, side.u0, side.v0, side.u1, side.v0+sideTile);
		}
		else
		{
			this.top = top;
			this.side = side;
		}
		
		this.bottom = bottom;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return false;
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		if (!id.isEmpty()) {
			// Use blockstate model and add the book.
			BlockStateWrapper block = world.getModelRegistry().getBlock(id);
			for (var model : block.getModels(block.getPropertiesWithDefaults(rawChunk.getBlockState(x, y, z)),
					(int)(rawChunk.getChunkCoord().x * RawChunk.WIDTH + x), rawChunk.getWorldY(y),
					(int)(rawChunk.getChunkCoord().z * RawChunk.DEPTH + z))) {
				model.createGeometry(x, y, z, world, rawChunk, geometry);
			}
		} else {
			addTableGeometry(x, y, z, world, rawChunk, geometry);
		}
		if (book != null) {
			float light = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
			addBookGeometry(geometry.getMesh(book.texture, Geometry.MeshType.AlphaTest), book,
					x, y, z, new Vector4f(light, light, light, 1));
		}
	}

	private void addTableGeometry(int x, int y, int z, BlockContext world, RawChunk rawChunk, Geometry geometry) {
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh bottomMesh = geometry.getMesh(bottom.texture, Geometry.MeshType.Solid);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.EastWest);
		
		final float height = 12.0f / 16.0f;
		
		MeshUtil.addQuad(topMesh, new Vector3f(x,		y+height,	z),
								  new Vector3f(x+1,		y+height,	z),
								  new Vector3f(x+1,		y+height,	z+1),
								  new Vector3f(x,		y+height,	z+1),
								  new Vector4f(topLight, topLight, topLight, 1.0f),
								  top);
		
		MeshUtil.addQuad(bottomMesh,	new Vector3f(x+1,	y,	z),
								  		new Vector3f(x,		y,	z),
								  		new Vector3f(x,		y,	z+1),
								  		new Vector3f(x+1,	y,	z+1),
								  		new Vector4f(topLight, topLight, topLight, 1.0f),
								  		bottom);
		
		MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y+1,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y+1,	z),
									new Vector3f(x+1,	y,		z),
									new Vector3f(x+1,	y,		z+1),
									new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z),
									new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z+1),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f),
									side);
	}

	/** The entity book's 64 x 32 texture layout, posed once for a static map. */
	static void addBookGeometry(Mesh mesh, SubTexture texture, int x, int y, int z, Vector4f colour) {
		// Vanilla's book is built in model pixels, with its spine along the Y axis.
		Matrix4f pose = new Matrix4f().translate(x + 0.5f, y + 0.85f, z + 0.5f)
				.rotateZ((float)Math.toRadians(80)).scale(1 / 16f);
		float opening = 1.25f;
		addBookBox(mesh, texture, colour, new Matrix4f(pose).translate(0, 0, -1).rotateY((float)Math.PI + opening),
				0, 0, -6, -5, 0, 6, 10, 0);
		addBookBox(mesh, texture, colour, new Matrix4f(pose).translate(0, 0, 1).rotateY(-opening),
				16, 0, 0, -5, 0, 6, 10, 0);
		addBookBox(mesh, texture, colour, new Matrix4f(pose).rotateY((float)Math.PI / 2),
				12, 0, -1, -5, 0, 2, 10, 0);
		float pageOffset = (float)Math.sin(opening);
		addBookBox(mesh, texture, colour, new Matrix4f(pose).translate(pageOffset, 0, 0).rotateY(opening),
				0, 10, 0, -4, -0.99f, 5, 8, 1);
		addBookBox(mesh, texture, colour, new Matrix4f(pose).translate(pageOffset, 0, 0).rotateY(-opening),
				12, 10, 0, -4, -0.01f, 5, 8, 1);
		addBookBox(mesh, texture, colour, new Matrix4f(pose).translate(pageOffset, 0, 0).rotateY(opening * 0.5f),
				24, 10, 0, -4, 0, 5, 8, 0);
		addBookBox(mesh, texture, colour, new Matrix4f(pose).translate(pageOffset, 0, 0).rotateY(-opening * 0.5f),
				24, 10, 0, -4, 0, 5, 8, 0);
	}

	private static void addBookBox(Mesh mesh, SubTexture texture, Vector4f colour, Matrix4f pose,
			float u, float v, float x, float y, float z, float width, float height, float depth) {
		Vector3f[] p = {
				new Vector3f(x, y, z), new Vector3f(x + width, y, z),
				new Vector3f(x + width, y + height, z), new Vector3f(x, y + height, z),
				new Vector3f(x, y, z + depth), new Vector3f(x + width, y, z + depth),
				new Vector3f(x + width, y + height, z + depth), new Vector3f(x, y + height, z + depth)
		};
		for (Vector3f point : p)
			pose.transformPosition(point);
		// Unfolded cube UVs; zero-depth lids/pages retain both faces.
		addBookFace(mesh, texture, colour, p, 5, 1, 2, 6, u + depth + width, v + depth, depth, height);
		addBookFace(mesh, texture, colour, p, 0, 4, 7, 3, u, v + depth, depth, height);
		addBookFace(mesh, texture, colour, p, 5, 4, 0, 1, u + depth, v, width, depth);
		addBookFace(mesh, texture, colour, p, 2, 3, 7, 6, u + depth + width, v + depth, width, -depth);
		addBookFace(mesh, texture, colour, p, 1, 0, 3, 2, u + depth, v + depth, width, height);
		addBookFace(mesh, texture, colour, p, 4, 5, 6, 7, u + depth * 2 + width, v + depth, width, height);
	}

	private static void addBookFace(Mesh mesh, SubTexture texture, Vector4f colour, Vector3f[] p,
			int a, int b, int c, int d, float u, float v, float width, float height) {
		float du = (texture.u1 - texture.u0) / 64;
		float dv = (texture.v1 - texture.v0) / 32;
		float u0 = texture.u0 + u * du;
		float u1 = texture.u0 + (u + width) * du;
		float v0 = texture.v0 + v * dv;
		float v1 = texture.v0 + (v + height) * dv;
		// Tectonicus uses clockwise front faces
		mesh.addVertex(p[d], colour, u1, v1);
		mesh.addVertex(p[c], colour, u0, v1);
		mesh.addVertex(p[b], colour, u0, v0);
		mesh.addVertex(p[a], colour, u1, v0);
	}
}
