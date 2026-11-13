/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;

import tectonicus.BlockContext;
import tectonicus.BlockOcclusion;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil.FaceDirection;
import tectonicus.rasteriser.LegacyBlockLighting;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Ice implements BlockType
{
	private final String name;
	
	private SubTexture texture;
	
	public Ice(String name, SubTexture texture)
	{
		if (texture == null)
			throw new RuntimeException("side subtexture is null!");

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
	public BlockOcclusion getOcclusion() {
		return BlockOcclusion.TRANSLUCENT;
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
		LegacyBlockLighting lighting = LegacyBlockLighting.forBlock(world, rawChunk, x, y, z);
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Transparent);
		
		final float topLight = lighting.isSmooth() ? 0 : world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = lighting.isSmooth() ? 0 : world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = lighting.isSmooth() ? 0 : world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = lighting.isSmooth() ? 0 : world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = lighting.isSmooth() ? 0 : world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
	//	final int northId = world.getBlockId(rawChunk.getChunkCoord(), x-1, y, z);
	//	BlockType north = registry.find(northId);
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid() && north != this)
		{
			lighting.addQuad(mesh, FaceDirection.WEST, LightFace.NorthSouth,	new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y+1,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									LegacyBlockLighting.WHITE, northLight,
									texture); 
		}
		
	//	final int southId = world.getBlockId(rawChunk.getChunkCoord(), x+1, y, z);
	//	BlockType south = registry.find(southId);
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid() && south != this)
		{
			lighting.addQuad(mesh, FaceDirection.EAST, LightFace.NorthSouth,	new Vector3f(x+1,		y+1,		z+1),
									new Vector3f(x+1,		y+1,	z),
									new Vector3f(x+1,		y,	z),
									new Vector3f(x+1,		y,	z+1),
									LegacyBlockLighting.WHITE, southLight,
									texture); 
		}
		
	//	final int eastId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z-1);
	//	BlockType east = registry.find(eastId);
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid() && east != this)
		{
			lighting.addQuad(mesh, FaceDirection.NORTH, LightFace.EastWest,	new Vector3f(x+1,	y+1,	z),
									new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									LegacyBlockLighting.WHITE, eastLight,
									texture); 
		}
		
	//	final int westId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z+1);
	//	BlockType west = registry.find(westId);
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid() && west != this)
		{
			lighting.addQuad(mesh, FaceDirection.SOUTH, LightFace.EastWest,	new Vector3f(x,		y+1,	z+1),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									LegacyBlockLighting.WHITE, westLight,
									texture); 
		}
		
	//	final int aboveId = world.getBlockId(rawChunk.getChunkCoord(), x, y+1, z);
	//	BlockType above = registry.find(aboveId);
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		if (!above.isSolid() && above != this)
		{
			lighting.addQuad(mesh, FaceDirection.UP, LightFace.Top,	new Vector3f(x,		y+1,	z),
									new Vector3f(x+1,	y+1,	z),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x,		y+1,	z+1),
									LegacyBlockLighting.WHITE, topLight,
									texture); 
		}
	}
	
}
