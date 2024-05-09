/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
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
import tectonicus.paintingregistry.PaintingRegistry;
import tectonicus.paintingregistry.PaintingVariant;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.PaintingEntity;
import tectonicus.raw.RawChunk;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		final float texel = 1.0f/16.0f;
		PaintingRegistry paintingRegistry = world.getPaintingRegistry();

		for (PaintingEntity entity : rawChunk.getPaintings()) {
			String motive = entity.getMotive().toLowerCase();
			SubTexture painting = world.getTexturePack().findTexture(null, motive);
			SubTexture backing = world.getTexturePack().findTexture(null, "minecraft:back");
			Mesh mesh = geometry.getMesh(painting.texture, Geometry.MeshType.AlphaTest);
			Mesh backMesh = geometry.getMesh(backing.texture, Geometry.MeshType.AlphaTest);

			int tempX = entity.getLocalX();
			int tempY = entity.getLocalY();
			int tempZ = entity.getLocalZ();

			if (tempZ < 0)
				tempZ = 0;
			
			int width = 1;  //width in blocks
			int height = 1; //height in blocks
			
			if (!paintingRegistry.isEmpty()) { //1.21 or higher
				PaintingVariant variant = paintingRegistry.get(motive);
				width = variant.getWidth();
				height = variant.getHeight();
			} else {
				// 16x16 paintings are 1x1 blocks
				switch (motive.replace("minecraft:", "")) {
					// 16x32 paintings
					case "pool":
					case "courbet":
					case "sea":
					case "sunset":
					case "creebet":
						width = 2;
						break;
					// 32x16 paintings
					case "wanderer":
					case "graham":
						height = 2;
						break;
					// 32x32 paintings
					case "match":
					case "bust":
					case "stage":
					case "void":
					case "skullandroses":
					case "skull_and_roses":
					case "wither":
					case "earth":
					case "fire":
					case "water":
					case "wind":
						width = height = 2;
						break;
					// 64x32 painting
					case "fighters":
						width = 4;
						height = 2;
						break;
					// 64x48 paintings
					case "skeleton":
					case "donkeykong":
					case "donkey_kong":
						width = 4;
						height = 3;
						break;
					// 64x64 paintings
					case "pointer":
					case "pigscene":
					case "burningskull":
					case "burning_skull":
						width = height = 4;
						break;
					default:
				}
			}
			
			int dim1 = width * 16;
			int dim2 = height * 16;

			final int localX = entity.getLocalX();
			final int localY = entity.getLocalY();
			final int localZ = entity.getLocalZ();
			final int direction = entity.getDirection();

			if (direction == 0) // Facing South
			{
				x = width > 2 ? localX-1 : localX;
				y = height > 2 ? localY-1 : localY;
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

				BlockUtil.addPartialBlock(backMesh, x, y, z, 0, 0, 0,
						dim1, dim2, 1, colour, backing, topLight, northSouthLight, eastWestLight,
						true, true, true, false, true, true);
				MeshUtil.addQuad(mesh, new Vector3f(x, y+height, z+texel), new Vector3f(x+width, y+height, z+texel),
						new Vector3f(x+width, y, z+texel), new Vector3f(x, y, z+texel), colour, painting);
			}
			else if (direction == 1) // Facing West
			{
				x = tempX = localX-1;
				y = height > 2 ? localY-1 : localY;
				z = width > 2 ? localZ-1 : localZ;

				if (tempX < 0)
					tempX++;
				else if (tempX == 16)
					tempX--;

				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ, world.getNightLightAdjustment());
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);

				BlockUtil.addPartialBlock(backMesh, x, y, z, 15, 0, 0,
						1, dim2, dim1, colour, backing, topLight, northSouthLight, eastWestLight,
						true, true, true, true, false, true);
				MeshUtil.addQuad(mesh, new Vector3f(x+texel*15, y+height, z), new Vector3f(x+texel*15, y+height, z+width),
						new Vector3f(x+texel*15, y, z+width), new Vector3f(x+texel*15, y, z), colour, painting);
			}
			else if (direction == 2) // Facing North
			{
				x = width > 1 ? localX-1 : localX;
				x = width > 3 ? x-1 : x;
				y = height > 2 ? localY-1 : localY;
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

				BlockUtil.addPartialBlock(backMesh, x, y, z, 0, 0, 15,
						dim1, dim2, 1, colour, backing, topLight, northSouthLight, eastWestLight,
						true, true, false, true, true, true);
				MeshUtil.addQuad(mesh, new Vector3f(x+width, y+height, z+texel*15), new Vector3f(x, y+height, z+texel*15),
						new Vector3f(x, y, z+texel*15), new Vector3f(x+width, y, z+texel*15), colour, painting);
			}
			else if (direction == 3) // Facing East
			{
				x = tempX = localX+1;
				y = height > 2 ? localY-1 : localY;
				z = width > 1 ? localZ-1 : localZ;
				z = width > 3 ? z-1 : z;

				if (tempX < 0)
					tempX = 0;
				else if (tempX == 16)
					tempX--;

				final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, tempX, tempY, tempZ, world.getNightLightAdjustment());
				Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);

				BlockUtil.addPartialBlock(backMesh, x, y, z, 0, 0, 0,
						1, dim2, dim1, colour, backing, topLight, northSouthLight, eastWestLight,
						true, true, true, true, true, false);
				MeshUtil.addQuad(mesh, new Vector3f(x+texel, y+height, z+width), new Vector3f(x+texel, y+height, z),
						new Vector3f(x+texel, y, z), new Vector3f(x+texel, y, z+width), colour, painting);
			}
		}
	}
}

