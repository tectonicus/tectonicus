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
import tectonicus.cache.BiomeCache;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

import static tectonicus.Version.VERSION_4;

public class TallGrass implements BlockType
{
	private final String name;
	private final BiomeCache biomeCache;
	private final TexturePack texturePack;
	
	private final SubTexture deadGrassTexture, tallGrassTexture, fernTexture;
	
	public TallGrass(String name, SubTexture deadGrassTexture, SubTexture tallGrassTexture, SubTexture fernTexture, BiomeCache biomeCache, TexturePack texturePack)
	{
		this.name = name;
		
		this.biomeCache = biomeCache;
		this.texturePack = texturePack;
		
		this.deadGrassTexture = deadGrassTexture;
		
		if (tallGrassTexture.texturePackVersion != VERSION_4)
		{
			final float grassTexel = 1.0f / tallGrassTexture.texture.getHeight();
			final float fernTexel = 1.0f / fernTexture.texture.getHeight();
			final float grassTile = grassTexel * tallGrassTexture.texture.getWidth();
			final float fernTile = fernTexel * fernTexture.texture.getWidth();
			this.tallGrassTexture = new SubTexture(tallGrassTexture.texture, tallGrassTexture.u0, tallGrassTexture.v0, tallGrassTexture.u1, tallGrassTexture.v0+grassTile);
			this.fernTexture = new SubTexture(fernTexture.texture, fernTexture.u0, fernTexture.v0, fernTexture.u1, fernTexture.v0+fernTile);
		}
		else
		{
			this.tallGrassTexture = tallGrassTexture;
			this.fernTexture = fernTexture;
		}
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
		SubTexture texture = getTexture(data);
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		Colour4f baseColour = getColour(x, y, z, data, world, rawChunk);
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		Vector4f colour = new Vector4f(baseColour.r * lightVal, baseColour.g * lightVal, baseColour.b * lightVal, baseColour.a);

		Plant.addPlantGeometry(x, y, z, 0, mesh, colour, texture);
	}

	private SubTexture getTexture(final int data)
	{
		final int type = data & 0x3;
		
		if (type == 0)
		{
			return deadGrassTexture;
		}
		else if (type == 1)
		{
			return tallGrassTexture;
		}
		else if (type == 2)
		{
			return fernTexture;
		}
		return tallGrassTexture;
	}
	
	private Colour4f getColour(final int x, final int y, final int z, final int data, BlockContext world, RawChunk rawChunk)
	{
		final int type = data & 0x3;
		
		if (type == 1 || type == 2)
		{
			// Tall grass and ferns take the biome colour
			
			/*BiomeData biomeData = biomeCache.loadBiomeData(rawChunk.getChunkCoord());
			BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
			Colour4f colour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );*/
			Colour4f colour = world.getPlantTintColor(rawChunk.getChunkCoord(), x, y, z, false);
			return colour;
		}
		else
		{
			return new Colour4f(1, 1, 1, 1);
		}
	}
}
