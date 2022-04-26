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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.raw.PaintingEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Painting implements BlockType
{
	private final String name;
	private final SubTexture texture, backing, kebab, aztec, alban, aztec2, bomb, plant, wasteland,
							 pool, courbet, sea, sunset, creebet, wanderer, graham,
							 match, bust, stage, thevoid, skullandroses, wither, fighters,
							 skeleton, donkeykong, pointer, pigscene, burningskull;

	public Painting(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;
		
		final float texel = 1.0f / 256.0f;
		final float tile = texel * 16;
		
		backing = new SubTexture(texture.texture, texture.u0+tile*12, texture.v0, texture.u0+tile*13, texture.v0+tile);
		
		// 16x16 paintings
		kebab = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u0+tile, texture.v0+tile);
		aztec = new SubTexture(texture.texture, texture.u0+tile, texture.v0, texture.u0+tile*2, texture.v0+tile);
		alban = new SubTexture(texture.texture, texture.u0+tile*2, texture.v0, texture.u0+tile*3, texture.v0+tile);
		aztec2 = new SubTexture(texture.texture, texture.u0+tile*3, texture.v0, texture.u0+tile*4, texture.v0+tile);
		bomb = new SubTexture(texture.texture, texture.u0+tile*4, texture.v0, texture.u0+tile*5, texture.v0+tile);
		plant = new SubTexture(texture.texture, texture.u0+tile*5, texture.v0, texture.u0+tile*6, texture.v0+tile);
		wasteland = new SubTexture(texture.texture, texture.u0+tile*6, texture.v0, texture.u0+tile*7, texture.v0+tile);
		
		// 16x32 paintings
		pool = new SubTexture(texture.texture, texture.u0, texture.v0+tile*2, texture.u0+tile*2, texture.v0+tile*3);
		courbet = new SubTexture(texture.texture, texture.u0+tile*2, texture.v0+tile*2, texture.u0+tile*4, texture.v0+tile*3);
		sea = new SubTexture(texture.texture, texture.u0+tile*4, texture.v0+tile*2, texture.u0+tile*6, texture.v0+tile*3);
		sunset = new SubTexture(texture.texture, texture.u0+tile*6, texture.v0+tile*2, texture.u0+tile*8, texture.v0+tile*3);
		creebet = new SubTexture(texture.texture, texture.u0+tile*8, texture.v0+tile*2, texture.u0+tile*10, texture.v0+tile*3);
		
		// 32x16 paintings
		wanderer = new SubTexture(texture.texture, texture.u0, texture.v0+tile*4, texture.u0+tile, texture.v0+tile*6);
		graham = new SubTexture(texture.texture, texture.u0+tile, texture.v0+tile*4, texture.u0+tile*2, texture.v0+tile*6);
		
		// 32x32 paintings
		match = new SubTexture(texture.texture, texture.u0, texture.v0+tile*8, texture.u0+tile*2, texture.v0+tile*10);
		bust = new SubTexture(texture.texture, texture.u0+tile*2, texture.v0+tile*8, texture.u0+tile*4, texture.v0+tile*10);
		stage = new SubTexture(texture.texture, texture.u0+tile*4, texture.v0+tile*8, texture.u0+tile*6, texture.v0+tile*10);
		thevoid = new SubTexture(texture.texture, texture.u0+tile*6, texture.v0+tile*8, texture.u0+tile*8, texture.v0+tile*10);
		skullandroses = new SubTexture(texture.texture, texture.u0+tile*8, texture.v0+tile*8, texture.u0+tile*10, texture.v0+tile*10);
		wither = new SubTexture(texture.texture, texture.u0+tile*10, texture.v0+tile*8, texture.u0+tile*12, texture.v0+tile*10);
		
		// 64x32 painting
		fighters = new SubTexture(texture.texture, texture.u0, texture.v0+tile*6, texture.u0+tile*4, texture.v0+tile*8);
		
		// 64x48 painting
		skeleton = new SubTexture(texture.texture, texture.u0+tile*12, texture.v0+tile*4, texture.u1, texture.v0+tile*7);
		donkeykong = new SubTexture(texture.texture, texture.u0+tile*12, texture.v0+tile*7, texture.u1, texture.v0+tile*10);
		
		// 64x64 painting
		pointer = new SubTexture(texture.texture, texture.u0, texture.v0+tile*12, texture.u0+tile*4, texture.v1);
		pigscene = new SubTexture(texture.texture, texture.u0+tile*4, texture.v0+tile*12, texture.u0+tile*8, texture.v1);
		burningskull = new SubTexture(texture.texture, texture.u0+tile*8, texture.v0+tile*12, texture.u0+tile*12, texture.v1);
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
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		final float texel = 1.0f/16.0f;
			
		for (PaintingEntity entity : rawChunk.getPaintings())
		{
			int tempX = entity.getLocalX();
			int tempY = entity.getLocalY();
			int tempZ = entity.getLocalZ();
			
			if (tempZ < 0)
				tempZ = 0;
			
			int dim1 = 16;
			int dim2 = 16;
			int numTilesX = 1;
			int numTilesY = 1;
			SubTexture painting = backing;
			
			String motive = entity.getMotive().replace("minecraft:", "").toLowerCase();

			// 16x16 paintings
			switch (motive) {
				case "kebab":
					painting = kebab;
					break;
				case "aztec":
					painting = aztec;
					break;
				case "alban":
					painting = alban;
					break;
				case "aztec2":
					painting = aztec2;
					break;
				case "bomb":
					painting = bomb;
					break;
				case "plant":
					painting = plant;
					break;
				case "wasteland":
					painting = wasteland;
					break;
				// 16x32 paintings
				case "pool":
					painting = pool;
					numTilesX = 2;
					dim1 = numTilesX * 16;
					break;
				case "courbet":
					painting = courbet;
					numTilesX = 2;
					dim1 = numTilesX * 16;
					break;
				case "sea":
					painting = sea;
					numTilesX = 2;
					dim1 = numTilesX * 16;
					break;
				case "sunset":
					painting = sunset;
					numTilesX = 2;
					dim1 = numTilesX * 16;
					break;
				case "creebet":
					painting = creebet;
					numTilesX = 2;
					dim1 = numTilesX * 16;
					break;
				// 32x16 paintings
				case "wanderer":
					painting = wanderer;
					numTilesY = 2;
					dim2 = numTilesY * 16;
					break;
				case "graham":
					painting = graham;
					numTilesY = 2;
					dim2 = numTilesY * 16;
					break;
				// 32x32 paintings
				case "match":
					painting = match;
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "bust":
					painting = bust;
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "stage":
					painting = stage;
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "void":
					painting = thevoid;
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "skullandroses":
				case "skull_and_roses":
					painting = skullandroses;
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "wither":
					painting = wither;
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				// 64x32 painting
				case "fighters":
					painting = fighters;
					numTilesX = 4;
					numTilesY = 2;
					dim1 = numTilesX * 16;
					dim2 = numTilesY * 16;
					break;
				// 64x48 paintings
				case "skeleton":
					painting = skeleton;
					numTilesX = 4;
					numTilesY = 3;
					dim1 = numTilesX * 16;
					dim2 = numTilesY * 16;
					break;
				case "donkeykong":
				case "donkey_kong":
					painting = donkeykong;
					numTilesX = 4;
					numTilesY = 3;
					dim1 = numTilesX * 16;
					dim2 = numTilesY * 16;
					break;
				// 64x64 paintings
				case "pointer":
					painting = pointer;
					numTilesX = numTilesY = 4;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "pigscene":
					painting = pigscene;
					numTilesX = numTilesY = 4;
					dim1 = dim2 = numTilesX * 16;
					break;
				case "burningskull":
				case "burning_skull":
					painting = burningskull;
					numTilesX = numTilesY = 4;
					dim1 = dim2 = numTilesX * 16;
					break;
				default:
			}
			
			final int localX = entity.getLocalX();
			final int localY = entity.getLocalY();
			final int localZ = entity.getLocalZ();
			final int direction = entity.getDirection();
			
			if (direction == 0) // Facing South
			{
				x = numTilesX > 2 ? localX-1 : localX;
				y = numTilesY > 2 ? localY-1 : localY;
				z = tempZ = localZ+1;
				
				if (tempZ < 0)
					tempZ++;
				if (tempZ == 16)
					tempZ--;
				
				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ, world.getNightLightAdjustment());
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				
				BlockUtil.addPartialBlock(mesh, x, y, z, 0, 0, 0, 
													dim1, dim2, 1, colour, backing, topLight, northSouthLight, eastWestLight,
													true, true, true, false, true, true);
				MeshUtil.addQuad(mesh, new Vector3f(x, y+numTilesY, z+texel), new Vector3f(x+numTilesX, y+numTilesY, z+texel), 
										new Vector3f(x+numTilesX, y, z+texel), new Vector3f(x, y, z+texel), colour, painting);
			}
			else if (direction == 1) // Facing West
			{
				x = tempX = localX-1;
				y = numTilesY > 2 ? localY-1 : localY;
				z = numTilesX > 2 ? localZ-1 : localZ;
				
				if (tempX < 0)
					tempX++;
				else if (tempX == 16)
					tempX--;
				
				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ, world.getNightLightAdjustment());
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				
				BlockUtil.addPartialBlock(mesh, x, y, z, 15, 0, 0, 
													1, dim2, dim1, colour, backing, topLight, northSouthLight, eastWestLight,
													true, true, true, true, false, true);
				MeshUtil.addQuad(mesh, new Vector3f(x+texel*15, y+numTilesY, z), new Vector3f(x+texel*15, y+numTilesY, z+numTilesX), 
										new Vector3f(x+texel*15, y, z+numTilesX), new Vector3f(x+texel*15, y, z), colour, painting);
			}
			else if (direction == 2) // Facing North
			{
				x = numTilesX > 1 ? localX-1 : localX;
				x = numTilesX > 2 ? x-1 : x;
				y = numTilesY > 2 ? localY-1 : localY;
				z = tempZ = localZ-1;
				
				if (tempZ < 0)
					tempZ++;
				else if(tempZ == 16)
					tempZ--;
				
				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ, world.getNightLightAdjustment());
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				
				BlockUtil.addPartialBlock(mesh, x, y, z, 0, 0, 15, 
													dim1, dim2, 1, colour, backing, topLight, northSouthLight, eastWestLight,
													true, true, false, true, true, true);
				MeshUtil.addQuad(mesh, new Vector3f(x+numTilesX, y+numTilesY, z+texel*15), new Vector3f(x, y+numTilesY, z+texel*15), 
										new Vector3f(x, y, z+texel*15), new Vector3f(x+numTilesX, y, z+texel*15), colour, painting);
			}
			else if (direction == 3) // Facing East
			{
				x = tempX = localX+1;
				y = numTilesY > 2 ? localY-1 : localY;
				z = numTilesX > 1 ? localZ-1 : localZ;
				z = numTilesX > 2 ? z-1 : z;
				
				if (tempX < 0)
					tempX = 0;
				else if (tempX == 16)
					tempX--;
				
				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ, world.getNightLightAdjustment());
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				
				BlockUtil.addPartialBlock(mesh, x, y, z, 0, 0, 0, 
													1, dim2, dim1, colour, backing, topLight, northSouthLight, eastWestLight,
													true, true, true, true, true, false);
				MeshUtil.addQuad(mesh, new Vector3f(x+texel, y+numTilesY, z+numTilesX), new Vector3f(x+texel, y+numTilesY, z), 
										new Vector3f(x+texel, y, z), new Vector3f(x+texel, y, z+numTilesX), colour, painting);
			}
		}
	}
}
