/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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

public class Carpet implements BlockType
{
	private final String name;
	
	private SubTexture texture, sideTexture;
	
	public Carpet(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;
		
		sideTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u1, texture.v0+1.0f/16.0f);
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
		
		final float	height = 1.0f / 16.0f;

		MeshUtil.addQuad(mesh,	new Vector3f(x,		y+height,	z),
								new Vector3f(x+1,	y+height,	z),
								new Vector3f(x+1,	y+height,	z+1),
								new Vector3f(x,		y+height,	z+1),
								new Vector4f(topLight, topLight, topLight, 1.0f),
								texture);
		
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
