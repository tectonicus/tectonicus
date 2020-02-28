/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class NetherWart implements BlockType
{
	private final String name;
	private final SubTexture[] textures;
	
	public NetherWart(String name, SubTexture tex0, SubTexture tex1, SubTexture tex2)
	{
		this.name = name;
		
		textures = new SubTexture[3];
		textures[0] = tex0;
		textures[1] = tex1;
		textures[2] = tex2;
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
	
	public void addInteriorGeometry(int x, int y, int z, tectonicus.BlockContext world, tectonicus.BlockTypeRegistry registry, tectonicus.raw.RawChunk chunk, tectonicus.renderer.Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		SubTexture texture;
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		if (data == 0)
		{
			texture = textures[0];
		}
		else if (data == 1 || data == 2)
		{
			texture = textures[1];
		}
		else if (data == 3)
		{
			texture = textures[2];
		}
		else
		{
			texture = textures[0];
		}
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		Vector4f colour = new Vector4f(lightVal, lightVal, lightVal, 1.0f);
		
		final float off = 4.0f / 16.0f;
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+off),
											new Vector3f(x+1,		y+1,	z+off),
											new Vector3f(x+1,		y,		z+off),
											new Vector3f(x,			y,		z+off),
											colour,
											texture);
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+1-off),
											new Vector3f(x+1,		y+1,	z+1-off),
											new Vector3f(x+1,		y,		z+1-off),
											new Vector3f(x,			y,		z+1-off),
											colour,
											texture);
		
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+off,		y+1,	z),
											new Vector3f(x+off,		y+1,	z+1),
											new Vector3f(x+off,		y,		z+1),
											new Vector3f(x+off,		y,		z),
											colour,
											texture);
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+1-off,	y+1,	z),
											new Vector3f(x+1-off,	y+1,	z+1),
											new Vector3f(x+1-off,	y,		z+1),
											new Vector3f(x+1-off,	y,		z),
											colour,
											texture);
	}
}
