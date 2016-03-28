/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class EndRod implements BlockType
{
	private final String name;

	private final SubTexture texture, rod, base;
	
	public EndRod(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;
		
		final float texel;
		if (texture.texturePackVersion == "1.4")
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.rod = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u0+texel*2, texture.v1-texel);
		this.base = new SubTexture(texture.texture, texture.u0+texel*2, texture.v0+texel*2, texture.u0+texel*6, texture.v0+texel*6);
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
		Vector4f colour = new Vector4f(0.9f, 0.9f, 0.9f, 1);
		
		final float offSet = 1.0f / 16.0f;
		
		SubMesh rodMesh = new SubMesh();
		SubMesh.addBlock(rodMesh, offSet*7, offSet*1, offSet*7, offSet*2, offSet*15, offSet*2, colour, rod, rod, rod);
		SubMesh.addBlock(rodMesh, offSet*6, 0, offSet*6, offSet*4, offSet*1, offSet*4, colour, base, base, base);
		
		rodMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
}
