/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Fence implements BlockType
{
	private final String name;
	private final int blockId;
	private final SubTexture texture;
	
	public Fence(String name, final int blockId, SubTexture texture)
	{
		this.name = name;
		this.blockId = blockId;
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float topLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		final float northSouthLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.EastWest);
		
		// Center collumn is always present
		BlockUtil.addBlock(mesh, x, y, z, 6, 0, 6, 4, 16, 4, colour, texture, topLight, northSouthLight, eastWestLight);
		
		// Auto-connect to adjacent fences
		
		// Bars are two wide and three high
		
		// North
		final int northId = world.getBlockId(chunk.getChunkCoord(), x-1, y, z);
		if (northId == blockId || northId == BlockIds.FENCE_GATE)
		{
			// Top bar
			BlockUtil.addBlock(mesh, x, y, z,	0, 12, 7,
												8, 3, 2, colour, texture, topLight, northSouthLight, eastWestLight);
			
			// Bottom bar
			BlockUtil.addBlock(mesh, x, y, z,	0, 6, 7,
												8, 3, 2, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		
		// South
		final int southId = world.getBlockId(chunk.getChunkCoord(), x+1, y, z);
		if (southId == blockId || southId == BlockIds.FENCE_GATE)
		{
			// Top bar
			BlockUtil.addBlock(mesh, x, y, z,	8, 12, 7,
												8, 3, 2, colour, texture, topLight, northSouthLight, eastWestLight);
			
			// Bottom bar
			BlockUtil.addBlock(mesh, x, y, z,	8, 6, 7,
												8, 3, 2, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		
		// East
		final int eastId = world.getBlockId(chunk.getChunkCoord(), x, y, z-1);
		if (eastId == blockId || eastId == BlockIds.FENCE_GATE)
		{
			// Top bar
			BlockUtil.addBlock(mesh, x, y, z,	7, 12, 0,
												2, 3, 8, colour, texture, topLight, northSouthLight, eastWestLight);
			
			// Bottom bar
			BlockUtil.addBlock(mesh, x, y, z,	7, 6, 0,
												2, 3, 8, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		
		// West
		final int westId = world.getBlockId(chunk.getChunkCoord(), x, y, z+1);
		if (westId == blockId || westId == BlockIds.FENCE_GATE)
		{
			// Top bar
			BlockUtil.addBlock(mesh, x, y, z,	7, 12, 8,
												2, 3, 8, colour, texture, topLight, northSouthLight, eastWestLight);
			
			// Bottom bar
			BlockUtil.addBlock(mesh, x, y, z,	7, 6, 8,
												2, 3, 8, colour, texture, topLight, northSouthLight, eastWestLight);
		}
	}
}
