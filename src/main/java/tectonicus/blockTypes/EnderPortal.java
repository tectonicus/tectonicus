/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class EnderPortal implements BlockType
{
	private final String name;
	
	private final SubTexture texture;
	
	public EnderPortal(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;
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
	public void addInteriorGeometry(int x, int y, int z, tectonicus.BlockContext world, tectonicus.BlockTypeRegistry registry, tectonicus.raw.RawChunk chunk, tectonicus.renderer.Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		final float height = 0.5f;
		
		MeshUtil.addQuad(mesh, new Vector3f(x,		y+height, z),
								new Vector3f(x+1,	y+height, z),
								new Vector3f(x+1,	y+height, z+1),
								new Vector3f(x,		y+height, z+1), colour, texture);
	}
}
