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
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

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
		this.tallGrassTexture = tallGrassTexture;
		this.fernTexture = fernTexture;
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
		
		Colour4f baseColour = getColour(x, z, data, rawChunk);
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		Vector4f colour = new Vector4f(baseColour.r * lightVal, baseColour.g * lightVal, baseColour.b * lightVal, baseColour.a);

		Plant.addPlantGeometry(x, y, z, mesh, colour, texture);
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
	
	private Colour4f getColour(final int x, final int z, final int data, RawChunk rawChunk)
	{
		final int type = data & 0x3;
		
		if (type == 1 || type == 2)
		{
			// Tall grass and ferns take the biome colour
			
			BiomeData biomeData = biomeCache.loadBiomeData(rawChunk.getChunkCoord());
			BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
			Colour4f colour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );
			return colour;
		}
		else
		{
			return new Colour4f(1, 1, 1, 1);
		}
	}
}
