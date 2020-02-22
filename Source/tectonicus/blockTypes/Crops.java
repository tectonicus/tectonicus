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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Crops implements BlockType
{
	private final String name;
	private final SubTexture[] textures;
	
	public Crops(String name, SubTexture t0, SubTexture t1, SubTexture t2, SubTexture t3, SubTexture t4, SubTexture t5, SubTexture t6, SubTexture t7)
	{
		this.name = name;
		
		textures = new SubTexture[8];
		textures[0] = t0;
		textures[1] = t1;
		textures[2] = t2;
		textures[3] = t3;
		textures[4] = t4;
		textures[5] = t5;
		textures[6] = t6;
		textures[7] = t7;
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		Vector4f colour = new Vector4f(lightVal, lightVal, lightVal, 1.0f);
	
		final int data = rawChunk.getBlockData(x, y, z);
		
		SubTexture texture = textures[data];
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		final float offset = 4.0f / 16.0f;
		
		// West edge	
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+offset,	y+1,	z),
											new Vector3f(x+offset,	y+1,	z+1),
											new Vector3f(x+offset,	y,		z+1),
											new Vector3f(x+offset,	y,		z),
											colour,
											texture);
		
		// East edge		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+1-offset,	y+1,	z),
											new Vector3f(x+1-offset,	y+1,	z+1),
											new Vector3f(x+1-offset,	y,		z+1),
											new Vector3f(x+1-offset,	y,		z),
											colour,
											texture);
		
		// South edge		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+offset),
											new Vector3f(x+1,		y+1,	z+offset),
											new Vector3f(x+1,		y,		z+offset),
											new Vector3f(x,			y,		z+offset),
											colour,
											texture);
		
		// North edge
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+1-offset),
											new Vector3f(x+1,		y+1,	z+1-offset),
											new Vector3f(x+1,		y,		z+1-offset),
											new Vector3f(x,			y,		z+1-offset),
											colour,
											texture);
	}
	
}
