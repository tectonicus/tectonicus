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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.PaintingEntity;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class ItemFrame implements BlockType
{
	private final String name;
	private final SubTexture background, border, map;

	public ItemFrame(String name, SubTexture background, SubTexture border, SubTexture map)
	{	
		this.name = name;
		
		final float texel;
		if (background.texturePackVersion == VERSION_4)
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.background = new SubTexture(background.texture, background.u0+texel*2, background.v0+texel*2, background.u1-texel*2, background.v1-texel*2);;
		this.border = border;
		this.map = map;
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
		Mesh mesh = geometry.getMesh(border.texture, Geometry.MeshType.Solid);
		Mesh backgroundMesh = geometry.getMesh(background.texture, Geometry.MeshType.Solid);
		Mesh mapMesh = null;
		if (map != null) { //Allow use of pre beta 1.6 minecraft jars
			mapMesh = geometry.getMesh(map.texture, Geometry.MeshType.AlphaTest);
		}
		
		final float texel = 1.0f/16.0f;
			
		for (PaintingEntity entity : rawChunk.getItemFrames())
		{
			int tempX = entity.getLocalX();
			int tempY = entity.getLocalY();
			int tempZ = entity.getLocalZ();
			
			if (tempZ < 0)
				tempZ = 0;
			
			final int localX = entity.getLocalX();
			final int localY = entity.getLocalY();
			final int localZ = entity.getLocalZ();
			final int direction = entity.getDirection();
			final String item = entity.getMotive();
			
			if (direction == 0) // Facing South
			{
				x = localX;
				y = localY;
				z = tempZ = localZ+1;

				if (tempZ < 0)
					tempZ++;
				if (tempZ == 16)
					tempZ--;
			
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				Vector4f color = new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1);
				if (item.equals("minecraft:filled_map"))
				{
					BlockUtil.addBlock(mesh, x, y, z, 0, 0, 0, 16, 16, 1, color, border, topLight, northSouthLight, eastWestLight);
					MeshUtil.addQuad(mapMesh, new Vector3f(x, y+1, z+texel*1.01f), new Vector3f(x+1, y+1, z+texel*1.01f), 
							new Vector3f(x+1, y, z+texel*1.01f), new Vector3f(x, y, z+texel*1.01f), color, map);
				}
				else
				{
					BlockUtil.addBlock(mesh, x, y, z, 3, 13, 0, 10, 1, 1, color, border, topLight, northSouthLight, eastWestLight);  // Top
					BlockUtil.addBlock(mesh, x, y, z, 3, 2, 0, 10, 1, 1, color, border, topLight, northSouthLight, eastWestLight);  // Bottom
					BlockUtil.addBlock(mesh, x, y, z, 2, 2, 0, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Left
					BlockUtil.addBlock(mesh, x, y, z, 13, 2, 0, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Right
				
					MeshUtil.addQuad(backgroundMesh, new Vector3f(x+texel*2.1f, y+texel*13.9f, z+texel/2), new Vector3f(x+texel*13.9f, y+texel*13.9f, z+texel/2), 
											new Vector3f(x+texel*13.9f, y+texel*2.1f, z+texel/2), new Vector3f(x+texel*2.1f, y+texel*2.1f, z+texel/2), color, background);
				}
			}
			else if (direction == 1) // Facing West
			{
				x = tempX = localX-1;
				y = localY;
				z = localZ;
				
				if (tempX < 0)
					tempX++;
				else if (tempX == 16)
					tempX--;
				
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				Vector4f color = new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1);
				if (item.equals("minecraft:filled_map"))
				{
					BlockUtil.addBlock(mesh, x, y, z, 15, 0, 0, 1, 16, 16, color, border, topLight, northSouthLight, eastWestLight);
					MeshUtil.addQuad(mapMesh, new Vector3f(x+texel*14.99f, y+1, z), new Vector3f(x+texel*14.99f, y+1, z+1), 
										new Vector3f(x+texel*14.99f, y, z+1), new Vector3f(x+texel*14.99f, y, z), color, map);
				}
				else
				{
					BlockUtil.addBlock(mesh, x, y, z, 15, 13, 3, 1, 1, 10, color, border, topLight, northSouthLight, eastWestLight);  // Top
					BlockUtil.addBlock(mesh, x, y, z, 15, 2, 3, 1, 1, 10, color, border, topLight, northSouthLight, eastWestLight);  // Bottom
					BlockUtil.addBlock(mesh, x, y, z, 15, 2, 2, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Left
					BlockUtil.addBlock(mesh, x, y, z, 15, 2, 13, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Right
					
					MeshUtil.addQuad(backgroundMesh, new Vector3f(x+texel*15.5f, y+texel*13.9f, z+texel*2.1f), new Vector3f(x+texel*15.5f, y+texel*13.9f, z+texel*13.9f), 
										new Vector3f(x+texel*15.5f, y+texel*2.1f, z+texel*13.9f), new Vector3f(x+texel*15.5f, y+texel*2.1f, z+texel*2.1f), color, background);
				}
			}
			else if (direction == 2) // Facing North
			{
				x = localX;
				y = localY;
				z = tempZ = localZ-1;
				
				if (tempZ < 0)
					tempZ++;
				else if(tempZ == 16)
					tempZ--;
				
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				Vector4f color = new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1);
				if (item.equals("minecraft:filled_map"))
				{
					BlockUtil.addBlock(mesh, x, y, z, 0, 0, 15, 16, 16, 1, color, border, topLight, northSouthLight, eastWestLight);
					MeshUtil.addQuad(mapMesh, new Vector3f(x+1, y+1, z+texel*14.99f), new Vector3f(x, y+1, z+texel*14.99f), 
										new Vector3f(x, y, z+texel*14.99f), new Vector3f(x+1, y, z+texel*14.99f),  color, map);
				}
				else
				{
					BlockUtil.addBlock(mesh, x, y, z, 3, 13, 15, 10, 1, 1, color, border, topLight, northSouthLight, eastWestLight); // Top
					BlockUtil.addBlock(mesh, x, y, z, 3, 2, 15, 10, 1, 1, color, border, topLight, northSouthLight, eastWestLight);  // Bottom
					BlockUtil.addBlock(mesh, x, y, z, 13, 2, 15, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Left
					BlockUtil.addBlock(mesh, x, y, z, 2, 2, 15, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Right
					
					MeshUtil.addQuad(backgroundMesh, new Vector3f(x+texel*13.9f, y+texel*13.9f, z+texel*15.5f), new Vector3f(x+texel*2.1f, y+texel*13.9f, z+texel*15.5f), 
										new Vector3f(x+texel*2.1f, y+texel*2.1f, z+texel*15.5f), new Vector3f(x+texel*13.9f, y+texel*2.1f, z+texel*15.5f),  color, background);
				}
			}
			else if (direction == 3) // Facing East
			{
				x = tempX = localX+1;
				y = localY;
				z = localZ;
				
				if (tempX < 0)
					tempX = 0;
				else if (tempX == 16)
					tempX--;
				
				final float topLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.Top);
				final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.NorthSouth);
				final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), tempX, tempY, tempZ, LightFace.EastWest);
				Vector4f color = new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1);
				if (item.equals("minecraft:filled_map"))
				{
					BlockUtil.addBlock(mesh, x, y, z, 0, 0, 0, 1, 16, 16, color, border, topLight, northSouthLight, eastWestLight);
					MeshUtil.addQuad(mapMesh, new Vector3f(x+texel*1.01f, y+1, z+1), new Vector3f(x+texel*1.01f, y+1, z),  
										new Vector3f(x+texel*1.01f, y, z), new Vector3f(x+texel*1.01f, y, z+1),  color, map);
					
				}
				else
				{
					BlockUtil.addBlock(mesh, x, y, z, 0, 13, 3, 1, 1, 10, color, border, topLight, northSouthLight, eastWestLight);  // Top
					BlockUtil.addBlock(mesh, x, y, z, 0, 2, 3, 1, 1, 10, color, border, topLight, northSouthLight, eastWestLight);  // Bottom
					BlockUtil.addBlock(mesh, x, y, z, 0, 2, 13, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Left
					BlockUtil.addBlock(mesh, x, y, z, 0, 2, 2, 1, 12, 1, color, border, topLight, northSouthLight, eastWestLight);  // Right
					
					MeshUtil.addQuad(backgroundMesh, new Vector3f(x+texel/2, y+texel*13.9f, z+texel*13.9f), new Vector3f(x+texel/2, y+texel*13.9f, z+texel*2.1f),  
										new Vector3f(x+texel/2, y+texel*2.1f, z+texel*2.1f), new Vector3f(x+texel/2, y+texel*2.1f, z+texel*13.9f),  color, background);
				}
			}
		}
	}
}
