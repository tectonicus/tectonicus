/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
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
import tectonicus.Version;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.util.Random;

import static tectonicus.Version.VERSION_13;
import static tectonicus.Version.VERSION_4;

public class Water implements BlockType
{
	private final String name;
	private SubTexture subTexture;
	private final Version texturePackVersion;
	
	public Water(String name, SubTexture subTexture, int frame)
	{
		this.name = name;
		this.texturePackVersion = subTexture.getTexturePackVersion();
		
		final int texHeight = subTexture.texture.getHeight();
		final int texWidth = subTexture.texture.getWidth();
		final int numTiles = texHeight/texWidth;

		if(numTiles > 1 && frame == 0)
		{
			Random rand = new Random();
			frame = rand.nextInt(numTiles)+1;
		}

		if (subTexture.texturePackVersion == VERSION_4)
			this.subTexture = subTexture;
		else
			this.subTexture = new SubTexture(subTexture.texture, subTexture.u0, subTexture.v0+(float)((frame-1)*texWidth)/texHeight, subTexture.u1, subTexture.v0+(float)(frame*texWidth)/texHeight);
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
		return true;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(subTexture.texture, Geometry.MeshType.Transparent);
		
		final float alpha = 0.8f;
		final float internalAlpha = 0.3f;
		final float waterLevel = 14.0f/16.0f;
		final Colour4f waterColor;
		if (texturePackVersion.getNumVersion() >= VERSION_13.getNumVersion()) {
			waterColor = world.getWaterColor(rawChunk.getChunkCoord(), x, y, z);
		} else {
			waterColor = new Colour4f();
		}

		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		BlockType aboveNorth = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z+1);
		BlockType aboveSouth = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z-1);
		BlockType aboveEast = world.getBlockType(rawChunk.getChunkCoord(), x+1, y+1, z);
		BlockType aboveWest = world.getBlockType(rawChunk.getChunkCoord(), x-1, y+1, z);

		BlockProperties aboveProperties = world.getBlockState(rawChunk.getChunkCoord(), x, y+1, z);
		BlockProperties belowProperties = world.getBlockState(rawChunk.getChunkCoord(), x, y-1, z);
		BlockProperties northProperties = world.getBlockState(rawChunk.getChunkCoord(), x, y, z-1);
		BlockProperties southProperties = world.getBlockState(rawChunk.getChunkCoord(), x, y, z+1);
		BlockProperties westProperties = world.getBlockState(rawChunk.getChunkCoord(), x-1, y, z);
		BlockProperties eastProperties = world.getBlockState(rawChunk.getChunkCoord(), x+1, y, z);

		//TODO: handle some waterlogged blocks better e.g. waterlogged stairs

		if(!above.getName().equals("Ice") && !above.isWater() && !aboveNorth.isWater() && !aboveSouth.isWater() && !aboveEast.isWater() && !aboveWest.isWater())  // Only water blocks that don't have another water block above them should be lower
		{
			BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
			String westName = world.getBlockName(rawChunk.getChunkCoord(), x-1, y, z);
			if (!west.isWater() && !isWaterlogged(westProperties, westName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+waterLevel,	z),
										new Vector3f(x,		y+waterLevel,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector4f(waterColor.r * northLight, waterColor.g * northLight, waterColor.b * northLight, alpha),
										subTexture); 
			}

			BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
			String eastName = world.getBlockName(rawChunk.getChunkCoord(), x+1, y, z);
			if (!east.isWater() && !isWaterlogged(eastProperties, eastName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+waterLevel,		z+1),
										new Vector3f(x+1,		y+waterLevel,	z),
										new Vector3f(x+1,		y,	z),
										new Vector3f(x+1,		y,	z+1),
										new Vector4f(waterColor.r * southLight, waterColor.g * southLight, waterColor.b * southLight, alpha),
										subTexture); 
			}

			BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
			String northName = world.getBlockName(rawChunk.getChunkCoord(), x, y, z-1);
			if (!north.isWater() && !isWaterlogged(northProperties, northName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+waterLevel,	z),
										new Vector3f(x,		y+waterLevel,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector4f(waterColor.r *eastLight, waterColor.g * eastLight, waterColor.b * eastLight, alpha),
										subTexture); 
			}

			BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
			String southName = world.getBlockName(rawChunk.getChunkCoord(), x, y, z+1);
			if (!south.isWater() && !isWaterlogged(southProperties, southName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+waterLevel,	z+1),
										new Vector3f(x+1,	y+waterLevel,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(waterColor.r *westLight, waterColor.g * westLight, waterColor.b * westLight, alpha),
										subTexture); 
			}
			
		//	if (!above.isWater())
			
				final float aboveAlpha = above.isWater() ? internalAlpha : alpha;
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+waterLevel,	z),
										new Vector3f(x+1,	y+waterLevel,	z),
										new Vector3f(x+1,	y+waterLevel,	z+1),
										new Vector3f(x,		y+waterLevel,	z+1),
										new Vector4f(waterColor.r * topLight, waterColor.g * topLight, waterColor.b * topLight, aboveAlpha),
										subTexture);
			
			
			BlockType below = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
			String belowName = world.getBlockName(rawChunk.getChunkCoord(), x, y+1, z);
			if (!below.isWater() && !isWaterlogged(belowProperties, belowName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,	z+1),
										new Vector3f(x+1,	y,	z+1),
										new Vector3f(x+1,	y,	z),
										new Vector3f(x,		y,	z),
										new Vector4f(waterColor.r * topLight, waterColor.g * topLight, waterColor.b * topLight, alpha),
										subTexture);
			}
		}
		else
		{
			BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
			String westName = world.getBlockName(rawChunk.getChunkCoord(), x-1, y, z);
			if (!west.isWater() && !isWaterlogged(westProperties, westName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector4f(waterColor.r * northLight, waterColor.g * northLight, waterColor.b * northLight, alpha),
										subTexture); 
			}

			BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
			String eastName = world.getBlockName(rawChunk.getChunkCoord(), x+1, y, z);
			if (!east.isWater() && !isWaterlogged(eastProperties, eastName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+1,		z+1),
										new Vector3f(x+1,		y+1,	z),
										new Vector3f(x+1,		y,	z),
										new Vector3f(x+1,		y,	z+1),
										new Vector4f(waterColor.r * southLight, waterColor.g * southLight, waterColor.b * southLight, alpha),
										subTexture); 
			}

			BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
			String northName = world.getBlockName(rawChunk.getChunkCoord(), x, y, z-1);
			if (!north.isWater() && !isWaterlogged(northProperties, northName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z),
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector4f(waterColor.r * eastLight, waterColor.g * eastLight, waterColor.b * eastLight, alpha),
										subTexture); 
			}

			BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
			String southName = world.getBlockName(rawChunk.getChunkCoord(), x, y, z+1);
			if (!south.isWater() && !isWaterlogged(southProperties, southName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(waterColor.r * westLight, waterColor.g * westLight, waterColor.b * westLight, alpha),
										subTexture); 
			}

			String aboveName = world.getBlockName(rawChunk.getChunkCoord(), x, y+1, z);
			if (!isWaterlogged(aboveProperties, aboveName)) {
				final float aboveAlpha = above.isWater() ? internalAlpha : alpha;
				MeshUtil.addQuad(mesh, new Vector3f(x, y + 1, z),
									   new Vector3f(x + 1, y + 1, z),
									   new Vector3f(x + 1, y + 1, z + 1),
									   new Vector3f(x, y + 1, z + 1),
									   new Vector4f(waterColor.r * topLight, waterColor.g * topLight, waterColor.b * topLight, aboveAlpha),
									   subTexture);
			}

			BlockType below = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
			String belowName = world.getBlockName(rawChunk.getChunkCoord(), x, y+1, z);
			if (!below.isWater() && !isWaterlogged(belowProperties, belowName))
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,	z+1),
										new Vector3f(x+1,	y,	z+1),
										new Vector3f(x+1,	y,	z),
										new Vector3f(x,		y,	z),
										new Vector4f(waterColor.r * topLight, waterColor.g * topLight, waterColor.b * topLight, alpha),
										subTexture);
			}
		}
	}

	private boolean isWaterlogged(BlockProperties properties, String id) {
		return (properties != null && properties.containsKey("waterlogged") && properties.get("waterlogged").equals("true"))
				|| id.contains("kelp") || id.contains("seagrass") || id.contains("pickle") || id.contains("coral");
	}
	
}
