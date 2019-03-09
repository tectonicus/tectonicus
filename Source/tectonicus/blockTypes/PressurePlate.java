/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class PressurePlate implements BlockType
{
	private final String name;
	private final SubTexture texture;
	private final SubTexture edgeTexture;
	
	public PressurePlate(String name, SubTexture texture)
	{
		this.name = name;
		
		final float texel;
		if (texture.texturePackVersion == VERSION_4)
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.texture = new SubTexture(texture.texture, texture.u0+texel, texture.v0+texel, texture.u1-texel, texture.v1-texel);
		this.edgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u1, texture.v0+texel);
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final float height = 1.0f / 16.0f;
		final float border = 1.0f / 16.0f;  //used to make pressure plate slightly smaller than the block it sits on
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		// Top quad
		MeshUtil.addQuad(mesh, new Vector3f(x+border, y+height, z+border), new Vector3f(x+1-border, y+height, z+border), new Vector3f(x+1-border, y+height, z+1-border), new Vector3f(x+border, y+height, z+1-border), white, texture);
		
		// West edge
		MeshUtil.addQuad(mesh, new Vector3f(x+border, y+height, z+border), new Vector3f(x+border, y+height, z+1-border), new Vector3f(x+border, y, z+1-border), new Vector3f(x+border, y, z+border), white, edgeTexture);
		
		// East edge
		MeshUtil.addQuad(mesh, new Vector3f(x+1-border, y+height, z+1-border), new Vector3f(x+1-border, y+height, z+border), new Vector3f(x+1-border, y, z+border), new Vector3f(x+1-border, y, z+1-border), white, edgeTexture);
		
		// South edge
		MeshUtil.addQuad(mesh, new Vector3f(x+1-border, y+height, z+border), new Vector3f(x+border, y+height, z+border), new Vector3f(x+border, y, z+border), new Vector3f(x+1-border, y, z+border), white, edgeTexture);
		
		// North edge
		MeshUtil.addQuad(mesh, new Vector3f(x+border, y+height, z+1-border), new Vector3f(x+1-border, y+height, z+1-border), new Vector3f(x+1-border, y, z+1-border), new Vector3f(x+border, y, z+1-border), white, edgeTexture);
	}
	
}
