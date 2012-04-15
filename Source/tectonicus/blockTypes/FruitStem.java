/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
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
		final float offsetY = (y-1) + (1 + data*2) * texel;
		
		final boolean isBent;
		
		if (data == 0x7)
		{
			// Fully grown, try and find a fruit to bend towards
			final boolean isFruitN = world.getBlockId(chunk.getChunkCoord(), x-1, y, z) == fruitBlockId;
			final boolean isFruitS = world.getBlockId(chunk.getChunkCoord(), x+1, y, z) == fruitBlockId;
			final boolean isFruitE = world.getBlockId(chunk.getChunkCoord(), x, y, z-1) == fruitBlockId;
			final boolean isFruitW = world.getBlockId(chunk.getChunkCoord(), x, y, z+1) == fruitBlockId;
			
			isBent = (isFruitN || isFruitS || isFruitE || isFruitW);
			
			// TODO: Actually bend the stem towards the fruit
		}
		else
		{
			isBent = false;
		}
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		
		BiomeData biomeData = biomeCache.loadBiomeData(chunk.getChunkCoord());
		BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
		Colour4f colour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );
		colour.r *= lightness;
		colour.g *= lightness;
		colour.b *= lightness;
		
		SubTexture texture = isBent ? bentStem : growingStem;
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		Plant.addPlantGeometry(x, offsetY, z, mesh, new Vector4f(colour.r, colour.g, colour.b, 1.0f), texture);
	}
}
