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
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class Lilly implements BlockType
{
	private final String name;
	private final SubTexture texture;
	
	private final BiomeCache biomeCache;
	private final TexturePack texturePack;
	
	public Lilly(String name, SubTexture texture, BiomeCache biomeCache, TexturePack texturePack)
	{
		this.name = name;
		this.texture = texture;
		
		this.biomeCache = biomeCache;
		this.texturePack = texturePack;
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
		BiomeData biomeData = biomeCache.loadBiomeData(rawChunk.getChunkCoord());
		BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
		Colour4f colour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		final float lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		int wx = (int)rawChunk.getChunkCoord().x * 16 + x;
		int wz = (int)rawChunk.getChunkCoord().z * 16 + z;
		
		/* The three lines below, to figure out lilypad rotation, were taken straight from this blog here: http://llbit.se/?p=1537 */
		long pr = (wx * 3129871L) ^ (wz * 116129781L) ^ ((long) y);
		pr = pr * pr * 42317861L + pr * 11L;
		int rotation = 3 & (int)(pr >> 16);
		
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
