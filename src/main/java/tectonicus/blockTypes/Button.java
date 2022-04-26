/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Button implements BlockType
{
	private static final int WIDTH = 6;
	private static final int HEIGHT = 2;
	private static final int DEPTH = 4;
	
	private final String name;
	private final SubTexture texture;
	
	public Button(String name, SubTexture texture)
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
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float texel = 1.0f / texture.texture.getWidth();
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, world.getNightLightAdjustment());
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		SubMesh subMesh = new SubMesh();

		float angle = 0;
		float vertAngle = 0;
		
		final int direction = data;
		
		if (direction == 0 || direction == 5)
		{
			SubMesh.addBlock(subMesh, texel*5, 0, texel*6, texel * WIDTH, texel * HEIGHT, texel * DEPTH, white, texture, texture, texture);
			
			if (direction == 0)
				vertAngle = 180;
		}
		else
		{
			SubMesh.addBlock(subMesh, texel*5, texel*6, 0, texel * WIDTH, texel * DEPTH, texel * HEIGHT, white, texture, texture, texture);
		}
		
		
		if (direction == 1)
		{
			// Facing east
			angle = 90;
		}
		else if (direction == 2)
		{
			// Facing west
			angle = -90;
		}
		else if (direction == 3)
		{ 
			//Facing south
		}
		else if (direction == 4)
		{
			// Facing north
			angle = 180;
		}
		
		subMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.Clockwise, angle, Rotation.Clockwise, vertAngle);
	}
	
}
