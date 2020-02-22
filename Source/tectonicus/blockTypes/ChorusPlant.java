/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class ChorusPlant implements BlockType
{
	private final String name;

	private final SubTexture tex;
	
	public ChorusPlant(String name, SubTexture texture)
	{
		this.name = name;
		this.tex = texture;
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
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		Vector4f colour = new Vector4f(lightVal, lightVal, lightVal, 1.0f);
	
		final int data = rawChunk.getBlockData(x, y, z);
		
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		BlockType up = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		final int down = world.getBlockId(rawChunk.getChunkCoord(), x, y-1, z);
		
		boolean hasNorth = north instanceof ChorusPlant || north instanceof ChorusFlower;
		boolean hasSouth = south instanceof ChorusPlant || south instanceof ChorusFlower;
		boolean hasEast = east instanceof ChorusPlant || east instanceof ChorusFlower;
		boolean hasWest = west instanceof ChorusPlant || west instanceof ChorusFlower;
		boolean hasUp = up instanceof ChorusPlant || up instanceof ChorusFlower;
		boolean hasDown = down == BlockIds.CHORUS_PLANT || down == BlockIds.END_STONE;
		
		final float offSet = 1.0f / 16.0f;
		SubMesh plantMesh = new SubMesh();
		SubMesh.addBlock(plantMesh, offSet*4, offSet*4, offSet*4, offSet*8, offSet*8, offSet*8, colour, tex, tex, tex); // center cube
		
		if (hasNorth)
			SubMesh.addBlock(plantMesh, offSet*4, offSet*4, 0, offSet*8, offSet*8, offSet*4, colour, tex, tex, tex); // north
		if (hasSouth)
			SubMesh.addBlock(plantMesh, offSet*4, offSet*4, offSet*12, offSet*8, offSet*8, offSet*4, colour, tex, tex, tex); // south
		if (hasEast)
			SubMesh.addBlock(plantMesh, offSet*12, offSet*4, offSet*4, offSet*4, offSet*8, offSet*8, colour, tex, tex, tex); // east
		if (hasWest)
			SubMesh.addBlock(plantMesh, 0, offSet*4, offSet*4, offSet*4, offSet*8, offSet*8, colour, tex, tex, tex); // west
		if (hasUp)
			SubMesh.addBlock(plantMesh, offSet*4, offSet*12, offSet*4, offSet*8, offSet*4, offSet*8, colour, tex, tex, tex); // up
		if (hasDown)
			SubMesh.addBlock(plantMesh, offSet*4, 0, offSet*4, offSet*8, offSet*4, offSet*8, colour, tex, tex, tex); // down
		
		plantMesh.pushTo(geometry.getMesh(tex.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
}
