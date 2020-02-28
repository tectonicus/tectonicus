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

public class Ladder implements BlockType
{
	private final String name;
	
	private final SubTexture texture;
	
	public Ladder(String name, SubTexture texture)
	{
		if (texture == null)
			throw new RuntimeException("texture is null!");
	
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		// Block data defines which side of the block the ladder is placed on
		final int data = chunk.getBlockData(x, y, z);
		 
		final float offset = 1.0f / 16.0f;
		
		if (data == 2)
		{
			// West side of block
			
			final float light = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.EastWest);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z+1-offset),
									new Vector3f(x,		y+1,	z+1-offset),
									new Vector3f(x,		y,		z+1-offset),
									new Vector3f(x+1,	y,		z+1-offset),
									new Vector4f(light, light, light, 1.0f),
									texture);
		}
		else if (data == 3)
		{
			// East side of our block
			
			final float light = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.EastWest);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+offset),
									new Vector3f(x+1,	y+1,	z+offset),
									new Vector3f(x+1,	y,		z+offset),
									new Vector3f(x,		y,		z+offset),
									new Vector4f(light, light, light, 1.0f),
									texture);
		}
		else if (data == 4)
		{
			// South side of block
			
			final float light = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+1-offset,	y+1,	z),
									new Vector3f(x+1-offset,	y+1,	z+1),
									new Vector3f(x+1-offset,	y,		z+1),
									new Vector3f(x+1-offset,	y,		z),
									new Vector4f(light, light, light, 1.0f),
									texture);
		}
		else if (data == 5)
		{
			// North side of block
			
			final float light = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+offset,		y+1,	z+1),
									new Vector3f(x+offset,		y+1,	z),
									new Vector3f(x+offset,		y,		z),
									new Vector3f(x+offset,		y,		z+1),
									new Vector4f(light, light, light, 1.0f),
									texture);
		}
	}
	
}
