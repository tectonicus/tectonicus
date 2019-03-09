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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class Snow implements BlockType
{
	private final String name;
	
	private SubTexture texture;
	
	public Snow(String name, SubTexture texture)
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		// Determine snow block height
		final float height;
		if(data == 0)
			height = 2.0f / 16.0f;
		else if(data == 1)
			height = 4.0f / 16.0f;
		else if(data == 2)
			height = 6.0f / 16.0f;
		else if(data == 3)
			height = 8.0f / 16.0f;
		else if(data == 4)
			height = 10.0f / 16.0f;
		else if(data == 5)
			height = 12.0f / 16.0f;
		else if(data == 6)
			height = 14.0f / 16.0f;
		else if(data == 7)
			height = 1;
		else
			height = 0;
		
		final float texHeight;
		if(texture.texturePackVersion == VERSION_4)
			texHeight = (1-height) / 16;
		else
			texHeight = 1-height;
		
		SubTexture sideTexture = new SubTexture(texture.texture, texture.u0, texture.v0+texHeight, texture.u1, texture.v1);
		
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		if(!(data == 7 && above.isSolid()))
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+height,	z),
									new Vector3f(x+1,	y+height,	z),
									new Vector3f(x+1,	y+height,	z+1),
									new Vector3f(x,		y+height,	z+1),
									new Vector4f(topLight, topLight, topLight, 1.0f),
									texture);
		}
		
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+height,	z),
									new Vector3f(x,		y+height,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(northLight, northLight, northLight, 1.0f),
									sideTexture); 
		}
		
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+height,		z+1),
									new Vector3f(x+1,		y+height,	z),
									new Vector3f(x+1,		y,	z),
									new Vector3f(x+1,		y,	z+1),
									new Vector4f(southLight, southLight, southLight, 1.0f),
									sideTexture);
		}
		
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+height,	z),
									new Vector3f(x,		y+height,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(eastLight, eastLight, eastLight, 1.0f),
									sideTexture); 
		}
		
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+height,	z+1),
									new Vector3f(x+1,	y+height,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(westLight, westLight, westLight, 1.0f),
									sideTexture); 
		}
	}
}
