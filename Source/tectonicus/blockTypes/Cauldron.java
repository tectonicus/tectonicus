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
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class Cauldron implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, bottom;
	private final SubTexture water;
	
	public Cauldron(String name, SubTexture top, SubTexture side, SubTexture bottom, SubTexture water)
	{
		this.name = name;
		
		this.top = top;
		this.side = side;
		this.bottom = bottom;
		
		if (water.texturePackVersion == VERSION_4)
			this.water = water;
		else
			this.water = new SubTexture(water.texture, water.u0, water.v0, water.u1, water.v0+16.0f/512.0f);
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.AlphaTest);
		Mesh sideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh bottomMesh = geometry.getMesh(bottom.texture, Geometry.MeshType.AlphaTest);
		Mesh waterMesh = geometry.getMesh(water.texture, Geometry.MeshType.Transparent);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		// Top
		MeshUtil.addQuad(topMesh, new Vector3f(x,	y+1, z),
								  new Vector3f(x+1,	y+1, z),
								  new Vector3f(x+1,	y+1, z+1),
								  new Vector3f(x, 	y+1, z+1),
								  colour, top);
		
		// Optional water
		final int data = rawChunk.getBlockData(x, y, z);
		if (data > 0)
		{
			float waterLevel = 0.1f;
			if (data == 1)
			{
				waterLevel = 5.0f / 16.0f;
			}
			else if (data == 2)
			{
				waterLevel = 10.0f / 16.0f;
			}
			else if (data == 3)
			{
				waterLevel = 15.0f / 16.0f;
			}
			MeshUtil.addQuad(waterMesh, new Vector3f(x,	y+waterLevel, z),
									  	new Vector3f(x+1,	y+waterLevel, z),
									  	new Vector3f(x+1,	y+waterLevel, z+1),
									  	new Vector3f(x, 	y+waterLevel, z+1),
									  	colour, water);
		}
		
		final float height = 4.0f / 16.0f;
		
		// Bottom
		MeshUtil.addQuad(bottomMesh, new Vector3f(x,	y+height, z),
									 new Vector3f(x+1,	y+height, z),
									 new Vector3f(x+1,	y+height, z+1),
									 new Vector3f(x, 	y+height, z+1),
									 colour, bottom);
		
		// North
		MeshUtil.addQuad(sideMesh, new Vector3f(x, y+1, z),
								   new Vector3f(x, y+1, z+1),
								   new Vector3f(x, y, 	z+1),
								   new Vector3f(x, y, 	z),
								   colour, side);
		
		// South
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1, y+1, 	z+1),
								   new Vector3f(x+1, y+1, 	z),
								   new Vector3f(x+1, y, 	z),
								   new Vector3f(x+1, y, 	z+1),
								   colour, side);
		
		// East
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1, y+1, 	z),
								   new Vector3f(x, 	 y+1, 	z),
								   new Vector3f(x, 	 y, 	z),
								   new Vector3f(x+1, y, 	z),
								   colour, side);
		
		// West
		MeshUtil.addQuad(sideMesh, new Vector3f(x,	 y+1, 	z+1),
								   new Vector3f(x+1, y+1, 	z+1),
								   new Vector3f(x+1, y, 	z+1),
								   new Vector3f(x,	 y, 	z+1),
								   colour, side);
		
		final float offset = 2.0f / 16.0f;
		
		// Inv north
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1-offset, y+1, z),
				   				   new Vector3f(x+1-offset, y+1, z+1),
				   				   new Vector3f(x+1-offset, y,   z+1),
				   				   new Vector3f(x+1-offset, y,   z),
				   				   colour, side);
		
		// Inv south
		MeshUtil.addQuad(sideMesh, new Vector3f(x+offset, y+1, z+1),
								   new Vector3f(x+offset, y+1, z),
								   new Vector3f(x+offset, y,   z),
								   new Vector3f(x+offset, y,   z+1),
								   colour, side);
		
		// Inv west
		MeshUtil.addQuad(sideMesh, new Vector3f(x,		y+1, z+offset),
								   new Vector3f(x+1,	y+1, z+offset),
								   new Vector3f(x+1,	y,   z+offset),
								   new Vector3f(x,		y,   z+offset),
								   colour, side);
		
		// Inv east
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1,	y+1, z+1-offset),
								   new Vector3f(x,		y+1, z+1-offset),
								   new Vector3f(x,		y,   z+1-offset),
								   new Vector3f(x+1,	y,   z+1-offset),
								   colour, side);
	}
}
