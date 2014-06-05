/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.awt.Color;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class Leaves implements BlockType
{
	private final String name;
	private final SubTexture texture;
	private final Color color; 
	
	private final BiomeCache biomeCache;
	private final TexturePack texturePack;
	
	public Leaves(String name, SubTexture texture, Color color, BiomeCache biomeCache, TexturePack texturePack)
	{
		this.name = name;
		
		this.texture = texture;
		this.color = color;
		
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		Colour4f colour = null;
		Color rawColour = this.color;
		if (rawColour == null)
		{
			colour = world.getGrassColour(rawChunk.getChunkCoord(), x, y, z);
			/*BiomeData biomeData = biomeCache.loadBiomeData(rawChunk.getChunkCoord());
			BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
			rawColour = texturePack.getFoliageColour(colourCoord.getX(), colourCoord.getY());*/
		}
		else		
			colour = new Colour4f(rawColour);
		
		BlockUtil.addTop(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addBottom(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		BlockUtil.addNorth(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addSouth(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addEast(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addWest(world, rawChunk, mesh, x, y, z, colour, texture, registry);
	}
	
}
