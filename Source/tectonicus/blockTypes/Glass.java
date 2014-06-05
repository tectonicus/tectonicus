/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Glass implements BlockType
{	
	private final String name;
	private final SubTexture texture;
	
	public Glass(String name, SubTexture texture)
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
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Transparent);
		
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		final int aboveId = world.getBlockId(rawChunk.getChunkCoord(), x, y+1, z);
		if (aboveId != BlockIds.GLASS)
			BlockUtil.addTop(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		final int belowId = world.getBlockId(rawChunk.getChunkCoord(), x, y-1, z);
		if (belowId != BlockIds.GLASS)
			BlockUtil.addBottom(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		final int northId = world.getBlockId(rawChunk.getChunkCoord(), x-1, y, z);
		if (northId != BlockIds.GLASS)
			BlockUtil.addNorth(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		final int southId = world.getBlockId(rawChunk.getChunkCoord(), x+1, y, z);
		if (southId != BlockIds.GLASS)
			BlockUtil.addSouth(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		final int eastId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z-1);
		if (eastId != BlockIds.GLASS)
			BlockUtil.addEast(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		final int westId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z+1);
		if (westId != BlockIds.GLASS)
			BlockUtil.addWest(world, rawChunk, mesh, x, y, z, colour, texture, registry);
	}
	
}
