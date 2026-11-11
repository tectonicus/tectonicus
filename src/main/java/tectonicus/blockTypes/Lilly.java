/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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
import tectonicus.blockregistry.BlockState;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Lilly implements BlockType
{
	private final String name;
	private final SubTexture texture;
	
	public Lilly(String name, SubTexture texture)
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		// Minecraft uses the same #208030 lily pad tint in every biome.
		Colour4f colour = new Colour4f(32, 128, 48);
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		final float lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		int wx = (int)rawChunk.getChunkCoord().x * 16 + x;
		int wz = (int)rawChunk.getChunkCoord().z * 16 + z;
		
		int rotation = BlockState.createPositionRandom(wx, rawChunk.getWorldY(y), wz).nextInt(4);
		
		if(rotation == 0)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
		else if(rotation == 1)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector3f(x+1,	y,		z+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
		else if(rotation == 2)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
		else if(rotation == 3)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y,		z),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
		
	}
	
	
}
