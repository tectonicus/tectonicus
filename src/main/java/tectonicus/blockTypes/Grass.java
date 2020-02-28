/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.cache.BiomeCache;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class Grass implements BlockType
{
	public enum BetterGrassMode
	{
		None,
		Fast,
		Fancy
	}
	
	private final String name;
	
	private final BetterGrassMode betterGrass;

	private final SubTexture sideTexture, grassSideTexture, topTexture, bottomTexture, snowSideTexture;
	
	public Grass(String name, BetterGrassMode betterGrassMode, SubTexture sideTexture, SubTexture grassSideTexture, SubTexture snowSideTexture, SubTexture topTexture, SubTexture bottomTexture, BiomeCache biomeCache, TexturePack texturePack)
	{
		if (sideTexture == null || topTexture == null)
			throw new RuntimeException("subtexture is null!");
		
		this.name = name;
		
		this.betterGrass = betterGrassMode;
		
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
		SubTexture actualSideTexture = aboveIsSnow ? snowSideTexture
				: betterGrass == BetterGrassMode.Fast ? topTexture
				: sideTexture;
		
		Colour4f sideColour = betterGrass == BetterGrassMode.Fast ? colour : white;
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		BlockUtil.addTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		Mesh bottomMesh = geometry.getMesh(bottomTexture.texture, Geometry.MeshType.Solid);
		BlockUtil.addBottom(world, rawChunk, bottomMesh, x, y, z, white, bottomTexture, registry);
		
		Mesh actualSideMesh = geometry.getMesh(actualSideTexture.texture, Geometry.MeshType.Solid);
		BlockUtil.addNorth(world, rawChunk, actualSideMesh, x, y, z, sideColour, actualSideTexture, registry);
		BlockUtil.addSouth(world, rawChunk, actualSideMesh, x, y, z, sideColour, actualSideTexture, registry);
		BlockUtil.addEast(world, rawChunk, actualSideMesh, x, y, z, sideColour, actualSideTexture, registry);
		BlockUtil.addWest(world, rawChunk, actualSideMesh, x, y, z, sideColour, actualSideTexture, registry);
		
		if (!aboveIsSnow && betterGrass != BetterGrassMode.Fast)
		{
			if (betterGrass != BetterGrassMode.Fancy)
			{
				Mesh alphaMesh = geometry.getMesh(grassSideTexture.texture, Geometry.MeshType.AlphaTest);
				BlockUtil.addNorth(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
				BlockUtil.addSouth(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
				BlockUtil.addEast(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
				BlockUtil.addWest(world, rawChunk, alphaMesh, x, y, z, colour, grassSideTexture, registry);
			}
			else
			{
				Mesh alphaMesh = geometry.getMesh(grassSideTexture.texture, Geometry.MeshType.AlphaTest);
				boolean isNorthGrass = world.getBlockId(rawChunk.getChunkCoord(), x-1, y-1, z) == BlockIds.GRASS;
				BlockUtil.addNorth(world, rawChunk, alphaMesh, x, y, z, colour, isNorthGrass ? topTexture : grassSideTexture, registry);
				boolean isSouthGrass = world.getBlockId(rawChunk.getChunkCoord(), x+1, y-1, z) == BlockIds.GRASS;
				BlockUtil.addSouth(world, rawChunk, alphaMesh, x, y, z, colour, isSouthGrass ? topTexture : grassSideTexture, registry);
				boolean isEastGrass = world.getBlockId(rawChunk.getChunkCoord(), x, y-1, z-1) == BlockIds.GRASS;
				BlockUtil.addEast(world, rawChunk, alphaMesh, x, y, z, colour, isEastGrass ? topTexture : grassSideTexture, registry);
				boolean isWestGrass = world.getBlockId(rawChunk.getChunkCoord(), x, y-1, z+1) == BlockIds.GRASS;
				BlockUtil.addWest(world, rawChunk, alphaMesh, x, y, z, colour, isWestGrass ? topTexture : grassSideTexture, registry);
			}
		}
	}
}
