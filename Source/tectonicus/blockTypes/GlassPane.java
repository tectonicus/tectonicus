/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class GlassPane implements BlockType
{
	private final String name;
	private final SubTexture texture;
	
	public GlassPane(String name, SubTexture texture)
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Transparent);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float topLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		final float northSouthLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.EastWest);
		
		BlockType north = world.getBlockType(chunk.getChunkCoord(), x, y, z-1);
		BlockType south = world.getBlockType(chunk.getChunkCoord(), x, y, z+1);
		BlockType east = world.getBlockType(chunk.getChunkCoord(), x+1, y, z);
		BlockType west = world.getBlockType(chunk.getChunkCoord(), x-1, y, z);
		
		boolean hasNorth = north instanceof GlassPane || north instanceof Glass;
		boolean hasSouth = south instanceof GlassPane || south instanceof Glass;
		boolean hasEast = east instanceof GlassPane || east instanceof Glass;
		boolean hasWest = west instanceof GlassPane || west instanceof Glass;
		
		// TODO: Fix textures for GlassPanes
		// GlassPanes will connect to any other GlassPanes (including Iron Bars), Glass blocks, or any Solid blocks
		
		if ((!hasNorth && !hasSouth && !hasEast && !hasWest) && (!north.isSolid() && !south.isSolid() && !east.isSolid() && !west.isSolid()))
		{
			if (texture.texturePackVersion == "1.9+")
			{
				final float offSet = 1.0f / 16.0f;
				SubMesh glassMesh = new SubMesh();
				SubMesh.addBlock(glassMesh, offSet*8, 0, offSet*8, offSet*2, 1, offSet*2, new Vector4f(topLight, topLight, topLight, 1), texture, texture, texture);
				glassMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Transparent), x, y, z, Rotation.None, 0);
			}
			else
			{
				hasNorth = hasSouth = hasEast = hasWest = true;
			}
		}
		
		if (hasNorth || north.isSolid())
		{
			BlockUtil.addPartialBlock(mesh, x, y, z,	7, 0, 0,
														2, 16, 9,
														colour, texture, topLight, northSouthLight, eastWestLight,
														true, true, false, true, true, true);
		}
		
		if (hasSouth || south.isSolid())
		{
			BlockUtil.addPartialBlock(mesh, x, y, z,	7, 0, 7,
														2, 16, 9,
														colour, texture, topLight, northSouthLight, eastWestLight,
														true, true, true, false, true, true);			
		}
		
		if (hasEast || east.isSolid())
		{
			BlockUtil.addPartialBlock(mesh, x, y, z,	7, 0, 7,
														9, 16, 2,
														colour, texture, topLight, northSouthLight, eastWestLight,
														true, true, true, true, false, true);
		}
		
		if (hasWest || west.isSolid())
		{
			BlockUtil.addPartialBlock(mesh, x, y, z,	0, 0, 7,
														9, 16, 2,
														colour, texture, topLight, northSouthLight, eastWestLight,
														true, true, true, true, true, false);
		}
	}
}
