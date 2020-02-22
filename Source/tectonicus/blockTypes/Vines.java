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
import tectonicus.cache.BiomeCache;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class Vines implements BlockType
{
	private final String name;
	
	private SubTexture texture;
	
	private BiomeCache biomeCache;
	private TexturePack texturePack;
	
	public Vines(String name, SubTexture texture, BiomeCache biomeCache, TexturePack texturePack)
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		final int data = chunk.getBlockData(x, y, z);
		
		final boolean isWest = (data & 0x1) > 0;
		final boolean isNorth = (data & 0x2) > 0;
		final boolean isEast = (data & 0x4) > 0;
		final boolean isSouth = (data & 0x8) > 0;
		
	//	final int aboveId = world.getBlockId(chunk.getChunkCoord(), x, y+1, z);
	//	final boolean aboveIsSolid = registry.find(aboveId).isSolid();
		BlockType above = world.getBlockType(chunk.getChunkCoord(), x, y+1, z);
		final boolean aboveIsSolid = above.isSolid();
		
		final boolean isTop = (data == 0 || aboveIsSolid);
		
		/*BiomeData biomeData = biomeCache.loadBiomeData(chunk.getChunkCoord());
		BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
		Colour4f grassColour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );*/
		Colour4f grassColour = world.getGrassColour(chunk.getChunkCoord(), x, y, z);
		
		final float ewLightness = Chunk.getLight(world.getLightStyle(), LightFace.EastWest, chunk, x, y, z);
		Vector4f eastWestLightness = new Vector4f(grassColour.r, grassColour.g, grassColour.b, 1);
		eastWestLightness.mul(ewLightness);
		eastWestLightness.w = 1.0f;
		
		final float nsLightness = Chunk.getLight(world.getLightStyle(), LightFace.EastWest, chunk, x, y, z);
		Vector4f northSouthLightness = new Vector4f(grassColour.r, grassColour.g, grassColour.b, 1);
		northSouthLightness.mul(nsLightness);
		northSouthLightness.w = 1.0f;
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		final float offset = 1.0f / 16.0f;
		if (isTop)
		{
			
		}
		
		if (isNorth)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+offset,	y+1, z+1),
									new Vector3f(x+offset,	y+1, z),
									new Vector3f(x+offset,	y, z),
									new Vector3f(x+offset,	y, z+1), northSouthLightness, texture);
		}
		if (isSouth)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1-offset,	y+1, z),
									new Vector3f(x+1-offset,	y+1, z+1),
									new Vector3f(x+1-offset,	y, z+1),
									new Vector3f(x+1-offset,	y, z), northSouthLightness, texture);
		}
		if (isEast)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1, z+offset),
									new Vector3f(x+1,	y+1, z+offset),
									new Vector3f(x+1,	y, z+offset),
									new Vector3f(x,		y, z+offset), eastWestLightness, texture);
		}
		if (isWest)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1, z+1-offset),
									new Vector3f(x,		y+1, z+1-offset),
									new Vector3f(x,		y, z+1-offset),
									new Vector3f(x+1,	y, z+1-offset), eastWestLightness, texture);
		}
	}
}
