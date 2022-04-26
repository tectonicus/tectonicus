/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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
import tectonicus.chunk.Chunk;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class FruitStem implements BlockType
{
	private final String name;
	
	private BiomeCache biomeCache;
	private TexturePack texturePack;
	
	private SubTexture growingStem;
	private SubTexture bentStem;
	
	private int fruitBlockId;
	
	public FruitStem(String name, final int fruitBlockId, SubTexture growingStem, SubTexture bentStem, BiomeCache biomeCache, TexturePack texturePack)
	{
		this.name = name;
		this.growingStem = growingStem;
		this.bentStem = bentStem;
		
		this.biomeCache = biomeCache;
		this.texturePack = texturePack;
		
		this.fruitBlockId = fruitBlockId;
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
		
		final float texel = 1.0f / 16.0f;
		//final float offsetY = (y-1) + (1 + data*2) * texel;
		final float offsetY = (1 + data*2) * texel;
		
		final boolean isBent;
		float horizAngle = 0;
		
		if (data == 0x7)
		{
			// Fully grown, try and find a fruit to bend towards
			final boolean isFruitW = world.getBlockId(chunk.getChunkCoord(), x-1, y, z) == fruitBlockId;
			final boolean isFruitE = world.getBlockId(chunk.getChunkCoord(), x+1, y, z) == fruitBlockId;
			final boolean isFruitN = world.getBlockId(chunk.getChunkCoord(), x, y, z-1) == fruitBlockId;
			final boolean isFruitS = world.getBlockId(chunk.getChunkCoord(), x, y, z+1) == fruitBlockId;
			
			isBent = (isFruitN || isFruitS || isFruitE || isFruitW);
			
			if(isFruitN)
				horizAngle = 270;
			else if(isFruitS)
				horizAngle = 90;
			else if(isFruitW)
				horizAngle = 0;
			else if(isFruitE)
				horizAngle = 180;					
		}
		else
		{
			isBent = false;
		}
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z, world.getNightLightAdjustment());
		
		BiomeData biomeData = biomeCache.loadBiomeData(chunk.getChunkCoord());
		BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
		Colour4f colour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );
		colour.r *= lightness;
		colour.g *= lightness;
		colour.b *= lightness;
		
		//final float lightVal = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		/*Colour4f grassColour = world.getGrassColour(chunk.getChunkCoord(), x, y, z);
		Vector4f colour = new Vector4f(grassColour.r, grassColour.g, grassColour.b, 1);*/
		
		SubTexture texture = isBent ? bentStem : growingStem;
		SubMesh subMesh = new SubMesh();
		//Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		subMesh.addDoubleSidedQuad(new Vector3f(0,	0+offsetY,	0.5f),
									new Vector3f(1,	0+offsetY,	0.5f),
									new Vector3f(1,	-1+offsetY,		0.5f),
									new Vector3f(0,	-1+offsetY,		0.5f),
									new Vector4f(colour.r, colour.g, colour.b, 1.0f), texture);
		
		subMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.Clockwise, horizAngle);		
		
		//Plant.addPlantGeometry(x, offsetY, z, mesh, new Vector4f(colour.r, colour.g, colour.b, 1.0f), texture);
	}
}
