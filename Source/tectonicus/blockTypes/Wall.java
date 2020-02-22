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
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Wall implements BlockType
{
	private final String name;
	private final int blockId;
	private final SubTexture texture;

	public Wall(String name, final int blockId, SubTexture texture)
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.EastWest);
		
		final int aboveId = world.getBlockId(rawChunk.getChunkCoord(), x, y+1, z);
		final int northId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z+1);
		final int southId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z-1);
		final int eastId = world.getBlockId(rawChunk.getChunkCoord(), x+1, y, z);
		final int westId = world.getBlockId(rawChunk.getChunkCoord(), x-1, y, z);
		
		BlockType type = null;
		
		if ((northId == blockId && southId == blockId) && (eastId != blockId && westId != blockId && aboveId != blockId))
		{
			BlockUtil.addBlock(mesh, x, y, z, 	5, 0, 0, 
												6, 13, 16, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		else if ((eastId == blockId && westId == blockId) && (northId != blockId && southId != blockId && aboveId != blockId))
		{
			BlockUtil.addBlock(mesh, x, y, z, 	0, 0, 5,
												16, 13, 6, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		else
		{
			// Center column
			BlockUtil.addBlock(mesh, x, y, z, 4, 0, 4, 8, 16, 8, colour, texture, topLight, northSouthLight, eastWestLight);
			
			// North
			type = registry.find(northId, 0);
			if (northId == blockId || type instanceof FenceGate)
			{
				BlockUtil.addBlock(mesh, x, y, z,	5, 0, 12,
													6, 13, 4, colour, texture, topLight, northSouthLight, eastWestLight);
			}
			
			// South
			type = registry.find(southId, 0);
			if (southId == blockId || type instanceof FenceGate)
			{
				BlockUtil.addBlock(mesh, x, y, z,	5, 0, 0,
													6, 13, 4, colour, texture, topLight, northSouthLight, eastWestLight);
			}
			
			// East
			type = registry.find(eastId, 0);
			if (eastId == blockId || type instanceof FenceGate)
			{
				BlockUtil.addBlock(mesh, x, y, z,	12, 0, 5,
													4, 13, 6, colour, texture, topLight, northSouthLight, eastWestLight);
			}
			
			// West
			type = registry.find(westId, 0);
			if (westId == blockId || type instanceof FenceGate)
			{
				BlockUtil.addBlock(mesh, x, y, z,	0, 0, 5,
													4, 13, 6, colour, texture, topLight, northSouthLight, eastWestLight);
			}
		}
	}

}
