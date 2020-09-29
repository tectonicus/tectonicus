/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.List;

public class Bell implements BlockType
{
	private final String name;
	private final String id;

	private final SubTexture texture, side, top, bottom, bottomSide;

	public Bell(String name, String id, SubTexture texture)
	{
		this.name = name;
		this.id = id;
		this.texture = texture;

		final float texel = 1.0f / 32.0f;

		top = new SubTexture(texture.texture, texture.u0+texel*6, texture.v0, texture.u0+texel*12, texture.v0+texel*6);
		side = new SubTexture(texture.texture, texture.u0+texel*6, texture.v0+texel*13, texture.u0, texture.v0+texel*6);
		bottom = new SubTexture(texture.texture, texture.u0+texel*8, texture.v0+texel*13, texture.u0+texel*16, texture.v0+texel*21);
		bottomSide = new SubTexture(texture.texture, texture.u0+texel*8, texture.v0+texel*23, texture.u0, texture.v0+texel*20);
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}

	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final float offSet = 1.0f / 16.0f;

		BlockStateWrapper bellBlock = world.getModelRegistry().getBlock(id);
		List<BlockStateModel> models = bellBlock.getModels(rawChunk.getBlockState(x, y, z));
		for (BlockStateModel bsc : models) {  //There should only be one model per variant for bells in vanilla Minecraft
			bsc.getBlockModel().createGeometry(x, y, z, world, rawChunk, geometry, bsc.getXRotation(), bsc.getYRotation());
		}

		float light = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		Vector4f colour = new Vector4f(light, light, light, 1);

		SubMesh bellMesh = new SubMesh();
		SubMesh.addBlockSimple(bellMesh, offSet * 5, offSet * 6, offSet * 5, offSet * 6, offSet * 7, offSet * 6, colour, side, top, top);
		SubMesh.addBlockSimple(bellMesh, offSet * 4, offSet * 3, offSet * 4, offSet * 8, offSet * 3, offSet * 8, colour, bottomSide, bottom, bottom);

		bellMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.None, 0);
	}
}
