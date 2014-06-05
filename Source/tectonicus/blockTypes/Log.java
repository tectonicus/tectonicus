/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Log implements BlockType
{
	private final String name;
	private final SubTexture sideTexture, topTexture;
	private Colour4f colour;
	
	public Log(String name, SubTexture sideTex, SubTexture topTexture)
	{
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		this.sideTexture = sideTex;
		this.topTexture = topTexture;
		this.colour = new Colour4f(1, 1, 1, 1);
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
	public void addInteriorGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
		
		/*Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		BlockUtil.addInteriorTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addInteriorNorth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorSouth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorEast(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorWest(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);*/
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final int data = rawChunk.getBlockData(x, y, z);
		final int type = rawChunk.getBlockId(x, y, z);
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		//0x4 - 0x7 sideways log east/west facing
		//0x8 - 0x11 sideways log north/south facing
		
		// Quartz Pillar
		//3 east/west facing
		//4 north/south facing

		if (type != 155 && data >= 0x4 && data <= 0x7 || type == 155 && data == 3)  // side, top, and bottom textures for east/west facing sideways blocks have to be rotated
		{
			BlockType above = context.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
			if (!above.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
				
				MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x+1,	y+1,	z),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x,	y+1,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u0, sideTexture.v1), new Vector2f(sideTexture.u0, sideTexture.v0),new Vector2f(sideTexture.u1, sideTexture.v0), new Vector2f(sideTexture.u1, sideTexture.v1));
			}
			
			BlockType below = context.getBlockType(rawChunk.getChunkCoord(), x, y-1, z);
			if (!below.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y-1, z, LightFace.Top);
				
				MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y,	z+1),
										new Vector3f(x+1,	y,	z+1),
										new Vector3f(x+1,	y,	z),
										new Vector3f(x,		y,	z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockType south = context.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);  //TODO:  Some side textures need a bit of adjusting still
			if (!south.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
				
				MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z),
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1),new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0));
			}
			
			BlockType north = context.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
			if (!north.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
				
				MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z+1),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockUtil.addNorth(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);  //East
			BlockUtil.addSouth(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);  //West
		}
		else if (data >= 0x8 && data <= 0x11 || type == 155 && data == 4)  // side textures for north/south facing sideways blocks have to be rotated
		{
			BlockUtil.addTop(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addBottom(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			
			BlockType east = context.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
			if (!east.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
				
				MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,		y+1,	z+1),
										new Vector3f(x+1,		y+1,	z),
										new Vector3f(x+1,		y,		z),
										new Vector3f(x+1,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockType west = context.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
			if (!west.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
				
				MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockUtil.addEast(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);  //North
			BlockUtil.addWest(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);  //South
		}
		else
		{
			BlockUtil.addTop(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addBottom(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		}
	}
	
}
