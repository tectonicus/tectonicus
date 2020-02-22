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
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class EndRod implements BlockType
{
	private final String name;

	private final SubTexture texture, rod, base;
	
	public EndRod(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;
		
		final float texel;
		if (texture.texturePackVersion == VERSION_4)
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.rod = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u0+texel*2, texture.v1-texel);
		this.base = new SubTexture(texture.texture, texture.u0+texel*2, texture.v0+texel*2, texture.u0+texel*6, texture.v0+texel*6);
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
		Vector4f colour = new Vector4f(0.9f, 0.9f, 0.9f, 1);
		
		final float offSet = 1.0f / 16.0f;
		
		final int data = rawChunk.getBlockData(x, y, z);
		final int direction = data & 0x7;
		
		SubMesh rodMesh = new SubMesh();
		SubMesh.addBlock(rodMesh, offSet*7, offSet*1, offSet*7, offSet*2, offSet*15, offSet*2, colour, rod, rod, rod);
		SubMesh.addBlock(rodMesh, offSet*6, 0, offSet*6, offSet*4, offSet*1, offSet*4, colour, base, base, base);
		
		
		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 0;
		
		Rotation vertRotation = Rotation.None;
		float vertAngle = 0;
	
		// Set angle/rotation from block data flags
		if (direction == 0)
		{
			// down
			vertRotation = Rotation.Clockwise;
			vertAngle = 180;
		}
		else if (direction == 1)
		{
			// up
			// ...unchanged
		}
		else if (direction == 2)
		{
			// north
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 90;
		}
		else if (direction == 3)
		{
			// south
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 90;
		}
		else if (direction == 4)
		{
			// west
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
		}
		else if (direction == 5)
		{
			// east
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 180;
		}
		
		rodMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
	}
}
