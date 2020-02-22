/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
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

import static tectonicus.Version.VERSION_4;

public class Soil implements BlockType
{
	private final String name;
	
	private SubTexture topTexture;
	private SubTexture sideTexture;
	
	public Soil(String name, SubTexture topTexture, SubTexture sideTexture)
	{
		this.name = name;
		
		this.topTexture = topTexture;
		
		final float texel;
		if (topTexture.texturePackVersion == VERSION_4)
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		final float offset = texel * 1;
		this.sideTexture = new SubTexture(sideTexture.texture, sideTexture.u0, sideTexture.v0 + offset, sideTexture.u1, sideTexture.v1);
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
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		final float height = 15.0f / 16.0f;
		
	//	final int aboveId = world.getBlockId(rawChunk.getChunkCoord(), x, y+1, z);
	//	BlockType above = registry.find(aboveId);
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		if (!above.isSolid())
		{
			MeshUtil.addQuad(topMesh,	new Vector3f(x,		y+height,	z),
										new Vector3f(x+1,	y+height,	z),
										new Vector3f(x+1,	y+height,	z+1),
										new Vector3f(x,		y+height,	z+1),
										new Vector4f(topLight, topLight, topLight, 1.0f),
										topTexture); 
		}
		
	//	final int northId = world.getBlockId(rawChunk.getChunkCoord(), x-1, y, z);
	//	BlockType north = registry.find(northId);
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid())
		{
			MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+height,	z),
										new Vector3f(x,		y+height,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector4f(northLight, northLight, northLight, 1.0f),
										sideTexture); 
		}
		
	//	final int southId = world.getBlockId(rawChunk.getChunkCoord(), x+1, y, z);
	//	BlockType south = registry.find(southId);
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid())
		{
			MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,		y+height,		z+1),
										new Vector3f(x+1,		y+height,	z),
										new Vector3f(x+1,		y,	z),
										new Vector3f(x+1,		y,	z+1),
										new Vector4f(southLight, southLight, southLight, 1.0f),
										sideTexture); 
		}
		
	//	final int eastId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z-1);
	//	BlockType east = registry.find(eastId);
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid())
		{
			MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+height,	z),
										new Vector3f(x,		y+height,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector4f(eastLight, eastLight, eastLight, 1.0f),
										sideTexture); 
		}
		
	//	final int westId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z+1);
	//	BlockType west = registry.find(westId);
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid())
		{
			MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+height,	z+1),
										new Vector3f(x+1,	y+height,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(westLight, westLight, westLight, 1.0f),
										sideTexture); 
		}
	}
	
}
