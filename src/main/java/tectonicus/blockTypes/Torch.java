/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class Torch implements BlockType
{
	private final String name;
	
	private final SubTexture sideTexture;
	private final SubTexture topTexture;
	
	public Torch(String name, SubTexture texture)
	{
		if (texture == null)
			throw new RuntimeException("side texture cannot be null!");
		
		this.name = name;
		
		final float texelSize;
		if (texture.texturePackVersion == VERSION_4)
			texelSize = 1.0f / 16.0f / 16.0f;
		else
			texelSize = 1.0f / 16.0f;
		
		final float topOffset = texelSize * 6;
		this.sideTexture = new SubTexture(texture.texture, texture.u0, texture.v0 + topOffset, texture.u1, texture.v1);
		
		final float uOffset = texelSize * 7;
		final float vOffset0 = texelSize * 6;
		final float vOffset1 = texelSize * 8;
		this.topTexture = new SubTexture(texture.texture, texture.u0 + uOffset, texture.v0 + vOffset0, texture.u1 - uOffset, texture.v1 - vOffset1);
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
		Mesh mesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.AlphaTest);
		
		final float lightness = 1.0f;
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float leftSide = 7.0f / 16.0f;
		final float rightSide = 9.0f / 16.0f;
		final float height = 10.0f / 16.0f;
		
		// Data defines torch placement
		// 0x1: Pointing south
		// 0x2: Pointing north
		// 0x3; Pointing west
		// 0x4: Pointing east
		// 0x5: Standing on the floor
		
		final int data = chunk.getBlockData(x, y, z);
		
		final float bottomOffsetX;
		final float bottomOffsetZ;
		final float bottomOffsetY;
		
		if (data == 1)
		{
			// Pointing south
			bottomOffsetX = -0.5f;
			bottomOffsetZ = 0.0f;
			bottomOffsetY = 0.4f;
		}
		else if (data == 2)
		{
			// Pointing north
			bottomOffsetX = 0.5f;
			bottomOffsetZ = 0.0f;
			bottomOffsetY = 0.4f;
		}
		else if (data == 3)
		{
			// Pointing west
			bottomOffsetX = 0.0f;
			bottomOffsetZ = -0.5f;
			bottomOffsetY = 0.4f;
		}
		else if (data == 4)
		{
			// Pointing east
			bottomOffsetX = 0.0f;
			bottomOffsetZ = 0.5f;
			bottomOffsetY = 0.4f;
		}
		else
		{
			// Standing on the floor
			bottomOffsetX = 0.0f;
			bottomOffsetZ = 0.0f;
			bottomOffsetY = 0.0f;
		}
		
		// Top
		MeshUtil.addQuad(mesh,	new Vector3f(x+leftSide,	y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x+rightSide,	y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x+rightSide,	y+height+bottomOffsetY,	z+rightSide),
								new Vector3f(x+leftSide,	y+height+bottomOffsetY,	z+rightSide),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								topTexture);
		
		// North
		MeshUtil.addQuad(mesh,	new Vector3f(x+leftSide,					y+height+bottomOffsetY,	z),
								new Vector3f(x+leftSide,					y+height+bottomOffsetY,	z+1),
								new Vector3f(x+leftSide + bottomOffsetX,	y+bottomOffsetY,			z+1 + bottomOffsetZ),
								new Vector3f(x+leftSide + bottomOffsetX,	y+bottomOffsetY,			z + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								sideTexture);
		
		// South
		MeshUtil.addQuad(mesh,	new Vector3f(x+rightSide,					y+height+bottomOffsetY,		z+1),
								new Vector3f(x+rightSide,					y+height+bottomOffsetY,		z),
								new Vector3f(x+rightSide + bottomOffsetX,	y+bottomOffsetY,			z + bottomOffsetZ),
								new Vector3f(x+rightSide + bottomOffsetX,	y+bottomOffsetY,			z+1 + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								sideTexture);
		
		// East
		MeshUtil.addQuad(mesh,	new Vector3f(x+1,					y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x,						y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x + bottomOffsetX,		y+bottomOffsetY,			z+leftSide + bottomOffsetZ),
								new Vector3f(x+1 + bottomOffsetX,	y+bottomOffsetY,			z+leftSide + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								sideTexture);
		
		// West
		MeshUtil.addQuad(mesh,	new Vector3f(x,						y+height+bottomOffsetY,	z+rightSide),
								new Vector3f(x+1,					y+height+bottomOffsetY,	z+rightSide),
								new Vector3f(x+1 + bottomOffsetX,	y+bottomOffsetY,			z+rightSide + bottomOffsetZ),
								new Vector3f(x + bottomOffsetX,		y+bottomOffsetY,			z+rightSide + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								sideTexture);
	}
	
}
