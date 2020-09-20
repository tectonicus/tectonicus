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
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.raw.PaintingEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class PaintingNew implements BlockType
{
	private final String name;

	public PaintingNew(String name)
	{
		this.name = name;
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
		final float texel = 1.0f/16.0f;

		for (PaintingEntity entity : rawChunk.getPaintings())
		{
			String motive = entity.getMotive().replace("minecraft:", "").toLowerCase();
			SubTexture painting = world.getTexturePack().findTexture(null, motive);
			SubTexture backing = world.getTexturePack().findTexture(null, "back");
			Mesh mesh = geometry.getMesh(painting.texture, Geometry.MeshType.Solid);
			Mesh backMesh = geometry.getMesh(backing.texture, Geometry.MeshType.Solid);

			int tempX = entity.getLocalX();
			int tempY = entity.getLocalY();
			int tempZ = entity.getLocalZ();

			if (tempZ < 0)
				tempZ = 0;

			int dim1 = 16;
			int dim2 = 16;
			int numTilesX = 1;
			int numTilesY = 1;

			//String motive = entity.getMotive().replace("minecraft:", "").toLowerCase();

			// 16x16 paintings
			switch (motive) {
				// 16x32 paintings
				case "pool":
				case "courbet":
				case "sea":
				case "sunset":
				case "creebet":
					numTilesX = 2;
					dim1 = numTilesX * 16;
					break;
				// 32x16 paintings
				case "wanderer":
				case "graham":
					numTilesY = 2;
					dim2 = numTilesY * 16;
					break;
				// 32x32 paintings
				case "match":
				case "bust":
				case "stage":
				case "void":
				case "skullandroses":
				case "skull_and_roses":
				case "wither":
					numTilesX = numTilesY = 2;
					dim1 = dim2 = numTilesX * 16;
					break;
				// 64x32 painting
				case "fighters":
					numTilesX = 4;
					numTilesY = 2;
					dim1 = numTilesX * 16;
					dim2 = numTilesY * 16;
					break;
				// 64x48 paintings
				case "skeleton":
				case "donkeykong":
				case "donkey_kong":
					numTilesX = 4;
					numTilesY = 3;
					dim1 = numTilesX * 16;
					dim2 = numTilesY * 16;
					break;
				// 64x64 paintings
				case "pointer":
				case "pigscene":
				case "burningskull":
				case "burning_skull":
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

				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ);
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);

				BlockUtil.addPartialBlock(backMesh, x, y, z, 0, 0, 0,
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

				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ);
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);

				BlockUtil.addPartialBlock(backMesh, x, y, z, 15, 0, 0,
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

				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ);
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);

				BlockUtil.addPartialBlock(backMesh, x, y, z, 0, 0, 15,
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

				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ);
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);

				BlockUtil.addPartialBlock(backMesh, x, y, z, 0, 0, 0,
						1, dim2, dim1, colour, backing, topLight, northSouthLight, eastWestLight,
						true, true, true, true, true, false);
				MeshUtil.addQuad(mesh, new Vector3f(x+texel, y+numTilesY, z+numTilesX), new Vector3f(x+texel, y+numTilesY, z),
						new Vector3f(x+texel, y, z), new Vector3f(x+texel, y, z+numTilesX), colour, painting);
			}
		}
	}
}

