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
import tectonicus.renderer.Geometry.MeshType;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class Slab implements BlockType
{
	private final String name;
	
	private SubTexture sideTexture;
	private SubTexture topTexture;
	
	public Slab(String name, SubTexture sideTexture, SubTexture topTexture)
	{
		if (sideTexture == null)
			throw new RuntimeException("side subtexture is null!");
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		
		this.sideTexture = sideTexture;
		this.topTexture = topTexture;
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
		final int data = rawChunk.getBlockData(x, y, z);
		final boolean upsidedown = data > 7;
		
		final float halfV;
		if (topTexture.texturePackVersion == VERSION_4)
			halfV = 1.0f / 16.0f / 2.0f;
		else
			halfV = 1.0f / 2.0f;
		
		final float vOffset = (upsidedown ? 0f : halfV);
		SubTexture halfSideTexture = new SubTexture(this.sideTexture.texture, this.sideTexture.u0, this.sideTexture.v0+vOffset, this.sideTexture.u1, this.sideTexture.v0+halfV+vOffset);
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh halfSideMesh = geometry.getMesh(halfSideTexture.texture, MeshType.Solid);
		
		final float yOffset = (upsidedown ? 0.5f : 0f);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float bottomLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		final BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		if (!upsidedown || !above.isSolid())
		{
			MeshUtil.addQuad(topMesh,	new Vector3f(x,		y+0.5f+yOffset,	z),
									new Vector3f(x+1,	y+0.5f+yOffset,	z),
									new Vector3f(x+1,	y+0.5f+yOffset,	z+1),
									new Vector3f(x,		y+0.5f+yOffset,	z+1),
									new Vector4f(topLight, topLight, topLight, 1.0f),
									this.topTexture); 
		}
		
		final BlockType bellow = world.getBlockType(rawChunk.getChunkCoord(), x, y-1, z);
		if (upsidedown || !bellow.isSolid())
		{
			MeshUtil.addQuad(topMesh,	new Vector3f(x,		y+yOffset,	z),
									new Vector3f(x+1,	y+yOffset,	z),
									new Vector3f(x+1,	y+yOffset,	z+1),
									new Vector3f(x,		y+yOffset,	z+1),
									new Vector4f(bottomLight, bottomLight, bottomLight, 1.0f),
									this.topTexture); 
		}
		
	//	final int northId = rawChunk.getBlockIdClamped(x-1, y, z, BlockIds.AIR);
	//	BlockType north = registry.find(northId);
		final BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid())
		{
			MeshUtil.addQuad(halfSideMesh,	new Vector3f(x,		y+0.5f+yOffset,	z),
									new Vector3f(x,		y+0.5f+yOffset,	z+1),
									new Vector3f(x,		y+yOffset,		z+1),
									new Vector3f(x,		y+yOffset,		z),
									new Vector4f(northLight, northLight, northLight, 1.0f),
									halfSideTexture); 
		}
		
	//	final int southId = rawChunk.getBlockIdClamped(x+1, y, z, BlockIds.AIR);
	//	BlockType south = registry.find(southId);
		final BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid())
		{
			MeshUtil.addQuad(halfSideMesh,	new Vector3f(x+1,		y+0.5f+yOffset,		z+1),
									new Vector3f(x+1,		y+0.5f+yOffset,	z),
									new Vector3f(x+1,		y+yOffset,	z),
									new Vector3f(x+1,		y+yOffset,	z+1),
									new Vector4f(southLight, southLight, southLight, 1.0f),
									halfSideTexture); 
		}
		
	//	final int eastId = rawChunk.getBlockIdClamped(x, y, z-1, BlockIds.AIR);
	//	BlockType east = registry.find(eastId);
		final BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid())
		{
			MeshUtil.addQuad(halfSideMesh,	new Vector3f(x+1,	y+0.5f+yOffset,	z),
									new Vector3f(x,		y+0.5f+yOffset,	z),
									new Vector3f(x,		y+yOffset,		z),
									new Vector3f(x+1,	y+yOffset,		z),
									new Vector4f(eastLight, eastLight, eastLight, 1.0f),
									halfSideTexture); 
		}
		
	//	final int westId = rawChunk.getBlockIdClamped(x, y, z+1, BlockIds.AIR);
	//	BlockType west = registry.find(westId);
		final BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid())
		{
			MeshUtil.addQuad(halfSideMesh,	new Vector3f(x,		y+0.5f+yOffset,	z+1),
									new Vector3f(x+1,	y+0.5f+yOffset,	z+1),
									new Vector3f(x+1,	y+yOffset,		z+1),
									new Vector3f(x,		y+yOffset,		z+1),
									new Vector4f(westLight, westLight, westLight, 1.0f),
									halfSideTexture); 
		}
	}
	
}
