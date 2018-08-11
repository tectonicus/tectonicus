/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class BlockUtil
{
	
	public static void addTop(BlockContext world, RawChunk rawChunk, Mesh mesh, final int blockX, final int blockY, final int blockZ, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
	//	final int aboveId = world.getBlockId(rawChunk.getChunkCoord(), blockX, blockY+1, blockZ);
	//	BlockType above = registry.find(aboveId);
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), blockX, blockY+1, blockZ);
		if (!above.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), blockX, blockY+1, blockZ, LightFace.Top);
			
			MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY+1,	blockZ),
									new Vector3f(blockX+1,	blockY+1,	blockZ),
									new Vector3f(blockX+1,	blockY+1,	blockZ+1),
									new Vector3f(blockX,	blockY+1,	blockZ+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addTop(BlockContext world, RawChunk rawChunk, Mesh mesh, final int blockX, final int blockY, final int blockZ, Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation)
	{
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), blockX, blockY+1, blockZ);
		if (!above.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), blockX, blockY+1, blockZ, LightFace.Top);
			
			if(rotation == Rotation.AntiClockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY+1,	blockZ+1),
										new Vector3f(blockX,	blockY+1,	blockZ),
										new Vector3f(blockX+1,	blockY+1,	blockZ),
										new Vector3f(blockX+1,	blockY+1,	blockZ+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Clockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX+1,	blockY+1,	blockZ),
										new Vector3f(blockX+1,	blockY+1,	blockZ+1),
										new Vector3f(blockX,	blockY+1,	blockZ+1),
										new Vector3f(blockX,	blockY+1,	blockZ),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Flip)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX+1,	blockY+1,	blockZ+1),
										new Vector3f(blockX,	blockY+1,	blockZ+1),
										new Vector3f(blockX,	blockY+1,	blockZ),
										new Vector3f(blockX+1,	blockY+1,	blockZ),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY+1,	blockZ),
										new Vector3f(blockX+1,	blockY+1,	blockZ),
										new Vector3f(blockX+1,	blockY+1,	blockZ+1),
										new Vector3f(blockX,	blockY+1,	blockZ+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
		}
	}
	
	public static void addBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, final int blockX, final int blockY, final int blockZ, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
	//	final int belowId = world.getBlockId(rawChunk.getChunkCoord(), blockX, blockY-1, blockZ);
	//	BlockType below = registry.find(belowId);
		BlockType below = world.getBlockType(rawChunk.getChunkCoord(), blockX, blockY-1, blockZ);
		if (!below.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), blockX, blockY-1, blockZ, LightFace.Top);
	//		final float lightness = world.getLight(rawChunk.getChunkCoord(), blockX, 128, blockZ, LightFace.Top);
			
			MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY,		blockZ+1),
									new Vector3f(blockX+1,	blockY,		blockZ+1),
									new Vector3f(blockX+1,	blockY,		blockZ),
									new Vector3f(blockX,	blockY,		blockZ),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, final int blockX, final int blockY, final int blockZ, Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation)
	{
		BlockType below = world.getBlockType(rawChunk.getChunkCoord(), blockX, blockY-1, blockZ);
		if (!below.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), blockX, blockY-1, blockZ, LightFace.Top);
			
			if(rotation == Rotation.AntiClockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY,		blockZ),
										new Vector3f(blockX,	blockY,		blockZ+1),
										new Vector3f(blockX+1,	blockY,		blockZ+1),
										new Vector3f(blockX+1,	blockY,		blockZ),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Clockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX+1,	blockY,		blockZ+1),
										new Vector3f(blockX+1,	blockY,		blockZ),
										new Vector3f(blockX,	blockY,		blockZ),
										new Vector3f(blockX,	blockY,		blockZ+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Flip)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX+1,	blockY,		blockZ),
										new Vector3f(blockX,	blockY,		blockZ),
										new Vector3f(blockX,	blockY,		blockZ+1),
										new Vector3f(blockX+1,	blockY,		blockZ+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else
			{
				MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY,		blockZ+1),
										new Vector3f(blockX+1,	blockY,		blockZ+1),
										new Vector3f(blockX+1,	blockY,		blockZ),
										new Vector3f(blockX,	blockY,		blockZ),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
		}
	}
	
	//Actually West
	public static void addNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
	//	final int northId = world.getBlockId(rawChunk.getChunkCoord(), x-1, y, z);
	//	BlockType north = registry.find(northId);
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y+1,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addWest(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation)
	{
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!west.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
			
			if (rotation == Rotation.AntiClockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z),
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Clockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector3f(x,		y+1,	z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Flip)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),						
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y+1,	z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
		}
	}
	
	//Actually East
	public static void addSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
	//	final int southId = world.getBlockId(rawChunk.getChunkCoord(), x+1, y, z);
	//	BlockType south = registry.find(southId);
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+1,	z+1),
									new Vector3f(x+1,		y+1,	z),
									new Vector3f(x+1,		y,		z),
									new Vector3f(x+1,		y,		z+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addEast(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation)
	{
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!east.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
			
			if (rotation == Rotation.AntiClockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y,		z+1),
										new Vector3f(x+1,		y+1,	z+1),
										new Vector3f(x+1,		y+1,	z),
										new Vector3f(x+1,		y,		z),										
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Clockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+1,	z),
										new Vector3f(x+1,		y,		z),
										new Vector3f(x+1,		y,		z+1),
										new Vector3f(x+1,		y+1,	z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Flip)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y,		z),
										new Vector3f(x+1,		y,		z+1),
										new Vector3f(x+1,		y+1,	z+1),
										new Vector3f(x+1,		y+1,	z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+1,	z+1),
										new Vector3f(x+1,		y+1,	z),
										new Vector3f(x+1,		y,		z),
										new Vector3f(x+1,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
		}
	}
	
	//Actually North
	public static void addEast(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
	//	final int eastId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z-1);
	//	BlockType east = registry.find(eastId);
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z),
									new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation)
	{
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!north.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
			
			if (rotation == Rotation.AntiClockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y,		z),
										new Vector3f(x+1,	y+1,	z),
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Clockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector3f(x+1,	y+1,	z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Flip)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector3f(x+1,	y+1,	z),
										new Vector3f(x,		y+1,	z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z),
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
		}
	}
	
	//Actually South
	public static void addWest(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
	//	final int westId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z+1);
	//	BlockType west = registry.find(westId);
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation)
	{
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!south.isSolid())
		{
			final float lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
			
			if (rotation == Rotation.AntiClockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Clockwise)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y+1,	z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else if (rotation == Rotation.Flip)
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
			else
			{
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										texture);
			}
		}
	}
	
	public static void addInteriorTop(BlockContext world, RawChunk rawChunk, Mesh mesh, final int blockX, final int blockY, final int blockZ, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
		BlockType above = world.getBlockType(rawChunk.getChunkCoord(), blockX, blockY+1, blockZ);
		if (!above.isSolid())
		{
			final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, blockX, blockY+1, blockZ);
			
			MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY+1,	blockZ),
									new Vector3f(blockX+1,	blockY+1,	blockZ),
									new Vector3f(blockX+1,	blockY+1,	blockZ+1),
									new Vector3f(blockX,	blockY+1,	blockZ+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addInteriorBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, final int blockX, final int blockY, final int blockZ, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
		BlockType below = world.getBlockType(rawChunk.getChunkCoord(), blockX, blockY-1, blockZ);
		if (!below.isSolid())
		{
			final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, blockX, blockY-1, blockZ);
			
			MeshUtil.addQuad(mesh,	new Vector3f(blockX,	blockY,		blockZ+1),
									new Vector3f(blockX+1,	blockY,		blockZ+1),
									new Vector3f(blockX+1,	blockY,		blockZ),
									new Vector3f(blockX,	blockY,		blockZ),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addInteriorNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
		BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid())
		{
			final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.NorthSouth, rawChunk, x-1, y, z);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y+1,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addInteriorSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
		BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid())
		{
			final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.NorthSouth, rawChunk, x+1, y, z);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+1,	z+1),
									new Vector3f(x+1,		y+1,	z),
									new Vector3f(x+1,		y,		z),
									new Vector3f(x+1,		y,		z+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addInteriorEast(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
		BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid())
		{
			final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.EastWest, rawChunk, x, y, z-1);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z),
									new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static void addInteriorWest(BlockContext world, RawChunk rawChunk, Mesh mesh, final int x, final int y, final int z, Colour4f colour, SubTexture texture, BlockTypeRegistry registry)
	{
		BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid())
		{
			final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.EastWest, rawChunk, x, y, z+1);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
									texture);
		}
	}
	
	public static final void clamp(Vector4f vec)
	{
		vec.x = clamp(vec.x, 0, 1);
		vec.y = clamp(vec.y, 0, 1);
		vec.z = clamp(vec.z, 0, 1);
		vec.w = clamp(vec.w, 0, 1);
	}
	
	private static float clamp(float val, float min, float max)
	{
		if (val < min)
			return min;
		if (val > max)
			return max;
		return val;
	}

	public static void addBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
										final int offsetX, final int offsetY, final int offsetZ,
										final int width, final int height, final int depth,
										Vector4f colour, SubTexture texture,
										final float topLight, final float northSouthLight, final float eastWestLight)
	{
		addBlock(mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northSouthLight, northSouthLight, eastWestLight, eastWestLight);
	}
	
	public static void addBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northLight, final float southLight, final float eastLight, final float westLight)
	{
		final float normOffX = (offsetX / 16.0f);
		final float normOffY = (offsetY / 16.0f);
		final float normOffZ = (offsetZ / 16.0f);
		
		final float x = blockX + normOffX;
		final float y = blockY + normOffY;
		final float z = blockZ + normOffZ;
		
		final float w = width / 16.0f;
		final float h = height / 16.0f;
		final float d = depth / 16.0f;
		
		final float uRange = texture.u1 - texture.u0;
		final float vRange = texture.v1 - texture.v0;
		
		Vector4f topColour = new Vector4f(colour.x * topLight, colour.y * topLight, colour.z * topLight, colour.w);
		Vector4f northColour = new Vector4f(colour.x * northLight, colour.y * northLight, colour.z * northLight, colour.w);
		Vector4f southColour = new Vector4f(colour.x * southLight, colour.y * southLight, colour.z * southLight, colour.w);
		Vector4f eastColour = new Vector4f(colour.x * eastLight, colour.y * eastLight, colour.z * eastLight, colour.w);
		Vector4f westColour = new Vector4f(colour.x * westLight, colour.y * westLight, colour.z * westLight, colour.w);
		
		// Top
		{
			Vector3f p0 = new Vector3f(x,		y + h,	z);
			Vector3f p1 = new Vector3f(x + w,	y + h,	z);
			Vector3f p2 = new Vector3f(x + w,	y + h,	z + d);
			Vector3f p3 =  new Vector3f(x,		y + h,	z + d);
			MeshUtil.addQuad(mesh,	p0, p1, p2, p3,
									topColour,
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * (normOffZ + d)),
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * (normOffZ + d))
			);
		}

		// Bottom
		{
			Vector3f p0 = new Vector3f(x,		y,	z);
			Vector3f p1 = new Vector3f(x + w,	y,	z);
			Vector3f p2 = new Vector3f(x + w,	y,	z + d);
			Vector3f p3 =  new Vector3f(x,		y,	z + d);
			MeshUtil.addQuad(mesh,	p3, p2, p1, p0,
									topColour,
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * (normOffZ + d)),
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * (normOffZ + d))
			);
		}
		
		// North
		MeshUtil.addQuad(mesh,	new Vector3f(x,	y + h,	z),
			 					new Vector3f(x,	y + h,	z + d),
			 					new Vector3f(x,	y,		z + d),
			 					new Vector3f(x,	y,		z),
			 					northColour,
			 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * normOffY),
			 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * normOffY)
		);
		
		// South
		MeshUtil.addQuad(mesh,	new Vector3f(x + w,	y + h,	z + d),
			 					new Vector3f(x + w,	y + h,	z),
			 					new Vector3f(x + w,	y,		z),
			 					new Vector3f(x + w,	y,		z + d),
			 					southColour,
			 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * normOffY),
			 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * normOffY)
		);

		// East
		MeshUtil.addQuad(mesh,	new Vector3f(x + w,	y + h,	z),
			 					new Vector3f(x,		y + h,	z),
			 					new Vector3f(x,		y,		z),
			 					new Vector3f(x + w,	y,		z ),
			 					eastColour,
			 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * normOffY),
			 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * normOffY)
		);
		
		// West
		MeshUtil.addQuad(mesh,	new Vector3f(x,		y + h,	z + d),
			 					new Vector3f(x + w,	y + h,	z + d),
			 					new Vector3f(x + w,	y,		z + d),
			 					new Vector3f(x,		y,		z + d),
			 					westColour,
			 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * (normOffY + h)),
			 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * normOffY),
			 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * normOffY)
		);
	}
	
	public static void addPartialBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northSouthLight, final float eastWestLight,
			final boolean top, final boolean bottom, final boolean north, final boolean south, final boolean east, final boolean west)
	{
		addPartialBlock(mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northSouthLight, northSouthLight, eastWestLight, eastWestLight, top, bottom, north, south, east, west);
	}
	
	public static void addPartialBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northLight, final float southLight, final float eastLight, final float westLight, 
			final boolean top, final boolean bottom, final boolean north, final boolean south, final boolean east, final boolean west)
	{
		final float normOffX = (offsetX / 16.0f);
		final float normOffY = (offsetY / 16.0f);
		final float normOffZ = (offsetZ / 16.0f);
		
		final float x = blockX + normOffX;
		final float y = blockY + normOffY;
		final float z = blockZ + normOffZ;
		
		final float w = width / 16.0f;
		final float h = height / 16.0f;
		final float d = depth / 16.0f;
		
		final float uRange = texture.u1 - texture.u0;
		final float vRange = texture.v1 - texture.v0;
		
		Vector4f topColour = new Vector4f(colour.x * topLight, colour.y * topLight, colour.z * topLight, colour.w);
		Vector4f northColour = new Vector4f(colour.x * northLight, colour.y * northLight, colour.z * northLight, colour.w);
		Vector4f southColour = new Vector4f(colour.x * southLight, colour.y * southLight, colour.z * southLight, colour.w);
		Vector4f eastColour = new Vector4f(colour.x * eastLight, colour.y * eastLight, colour.z * eastLight, colour.w);
		Vector4f westColour = new Vector4f(colour.x * westLight, colour.y * westLight, colour.z * westLight, colour.w);
		
		// Top
		if(top)
		{
			Vector3f p0 = new Vector3f(x,		y + h,	z);
			Vector3f p1 = new Vector3f(x + w,	y + h,	z);
			Vector3f p2 = new Vector3f(x + w,	y + h,	z + d);
			Vector3f p3 =  new Vector3f(x,		y + h,	z + d);
			MeshUtil.addQuad(mesh,	p0, p1, p2, p3,
									topColour,
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * (normOffZ + d)),
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * (normOffZ + d))
			);
		}

		// Bottom
		if(bottom)
		{
			Vector3f p0 = new Vector3f(x,		y,	z);
			Vector3f p1 = new Vector3f(x + w,	y,	z);
			Vector3f p2 = new Vector3f(x + w,	y,	z + d);
			Vector3f p3 =  new Vector3f(x,		y,	z + d);
			MeshUtil.addQuad(mesh,	p3, p2, p1, p0,
									topColour,
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * (normOffZ + d)),
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * (normOffZ + d))
			);
		}
		
		// East
		if(east)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,	y + h,	z),
				 					new Vector3f(x,	y + h,	z + d),
				 					new Vector3f(x,	y,		z + d),
				 					new Vector3f(x,	y,		z),
				 					eastColour,
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * normOffY)
			);
		}
		
		// West
		if(west)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x + w,	y + h,	z + d),
				 					new Vector3f(x + w,	y + h,	z),
				 					new Vector3f(x + w,	y,		z),
				 					new Vector3f(x + w,	y,		z + d),
				 					westColour,
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * normOffY)
			);
		}

		// north
		if(north)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x + w,	y + h,	z),
				 					new Vector3f(x,		y + h,	z),
				 					new Vector3f(x,		y,		z),
				 					new Vector3f(x + w,	y,		z ),
				 					northColour,
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * normOffY)
			);
		}
		
		// South
		if(south)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y + h,	z + d),
				 					new Vector3f(x + w,	y + h,	z + d),
				 					new Vector3f(x + w,	y,		z + d),
				 					new Vector3f(x,		y,		z + d),
				 					southColour,
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * normOffY)
			);
		}
	}
}
