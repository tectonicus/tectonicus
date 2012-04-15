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

import java.awt.Color;
import java.awt.Point;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.BiomeIds;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class Grass implements BlockType
{
	private final String name;
	private final BiomeCache biomeCache;
	private final TexturePack texturePack;
	
	private final SubTexture sideTexture, grassSideTexture, topTexture, bottomTexture, snowSideTexture;
	
	public Grass(String name, SubTexture sideTexture, SubTexture grassSideTexture, SubTexture snowSideTexture, SubTexture topTexture, SubTexture bottomTexture, BiomeCache biomeCache, TexturePack texturePack)
	{
		if (sideTexture == null || topTexture == null)
			throw new RuntimeException("subtexture is null!");
		
		this.name = name;
		
		this.biomeCache = biomeCache;
		this.texturePack = texturePack;
		
		this.sideTexture = sideTexture;
		this.grassSideTexture = grassSideTexture;
		this.snowSideTexture = snowSideTexture;
		this.topTexture = topTexture;
		this.bottomTexture = bottomTexture;
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return true;
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
		/*
		BiomeData biomeData = biomeCache.loadBiomeData(rawChunk.getChunkCoord());
		BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
		Color awtColour = texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY());
		Colour4f colour = new Colour4f(awtColour);
		*/
		/*
		final int biomeId = rawChunk.getBiomeId(x, y, z);
		Point colourCoord = BiomeIds.getColourCoord(biomeId);
		Color awtColour = texturePack.getGrassColour(colourCoord.x, colourCoord.y);
		Colour4f colour = new Colour4f(awtColour);
		*/
		Colour4f colour = world.getGrassColour(rawChunk.getChunkCoord(), x, y, z);
		
		/*
		try
		{
			FileBiomeCache fBiomes = (FileBiomeCache)biomeCache;
			WorldProcessor worldProcessor = fBiomes.getWorldProcessor();
			
			int blockX = (int)(rawChunk.getChunkCoord().x*RawChunk.WIDTH + x);
			int blockZ = (int)(rawChunk.getChunkCoord().z*RawChunk.HEIGHT + z);
			byte[] bpCoords = worldProcessor.getCoordsAtBlock(blockX, blockZ);
			
			int rgb = worldProcessor.getRGBAtBlock(blockX, blockZ, ColourType.GrassColour);
			Color bpColour = new Color(rgb);
			
			if (awtColour.getRGB() != bpColour.getRGB())
			{
				System.out.println("haltz");
			}
			
			Colour4f glColour = new Colour4f(bpColour);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		*/
		
		
		
		Colour4f white = new Colour4f(1, 1, 1, 1);
		
		final int aboveId = rawChunk.getBlockIdClamped(x, y+1, z, BlockIds.AIR);
		final boolean aboveIsSnow = aboveId == BlockIds.SNOW;
		SubTexture actualSideTexture = aboveIsSnow ? snowSideTexture : sideTexture;
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		BlockUtil.addTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		Mesh bottomMesh = geometry.getMesh(bottomTexture.texture, Geometry.MeshType.Solid);
		BlockUtil.addBottom(world, rawChunk, bottomMesh, x, y, z, white, bottomTexture, registry);
		
		Mesh actualSideMesh = geometry.getMesh(actualSideTexture.texture, Geometry.MeshType.Solid);
		BlockUtil.addNorth(world, rawChunk, actualSideMesh, x, y, z, white, actualSideTexture, registry);
		BlockUtil.addSouth(world, rawChunk, actualSideMesh, x, y, z, white, actualSideTexture, registry);
		BlockUtil.addEast(world, rawChunk, actualSideMesh, x, y, z, white, actualSideTexture, registry);
		BlockUtil.addWest(world, rawChunk, actualSideMesh, x, y, z, white, actualSideTexture, registry);
		
		if (!aboveIsSnow)
		{
			Mesh alphaMesh = geometry.getMesh(grassSideTexture.texture, Geometry.MeshType.AlphaTest);
			BlockUtil.addNorth(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
			BlockUtil.addSouth(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
			BlockUtil.addEast(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
			BlockUtil.addWest(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);	
		}
	}
}
