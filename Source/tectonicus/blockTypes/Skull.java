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
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.raw.TileEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Skull implements BlockType 
{
	private final String name;
	
	private SubTexture texture;
	private final SubTexture ctexture;
	private final SubTexture stexture;
	private final SubTexture wtexture;
	private final SubTexture ztexture;

	private Colour4f colour;

	public Skull(String name, SubTexture texture, SubTexture ctexture, SubTexture stexture, SubTexture wtexture, SubTexture ztexture) 
	{
		this.name = name;
		
		this.texture = texture;
		this.ctexture = ctexture;
		this.stexture = stexture;
		this.wtexture = wtexture;
		this.ztexture = ztexture;
		
		colour = new Colour4f(1, 1, 1, 1);
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
		SubMesh subMesh = new SubMesh();
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float offSet = 1.0f / 16.0f;
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		float lightness = 1;
		
		SubTexture currentTexture = null;
		
		for (TileEntity te : rawChunk.getSkulls())
		{
			if (te.localX == x && te.localY == y && te.localZ == z)
			{
				rotation = Rotation.AntiClockwise;
				angle = 90 / 4.0f * te.data + 180;
				
				if (te.blockData == 0)
					currentTexture = stexture;
				else if (te.blockData == 1)
					currentTexture = wtexture;
				else if (te.blockData == 2)
					currentTexture = ztexture;
				else if (te.blockData == 3)
					currentTexture = texture;
				else if (te.blockData == 4)
					currentTexture = ctexture;
			}
		}
		
		final float widthTexel = 1.0f / currentTexture.texture.getWidth();
		final float heightTexel = 1.0f / currentTexture.texture.getHeight();
		
		SubTexture topTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0, texture.u0+widthTexel*16, texture.v0+heightTexel*8);
		SubTexture bottomTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*16, texture.v0, texture.u0+widthTexel*24, texture.v0+heightTexel*8);
		SubTexture faceTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*8, texture.u0+widthTexel*16, texture.v0+heightTexel*16);
		SubTexture rearTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*24, texture.v0+heightTexel*8, texture.u0+widthTexel*32, texture.v0+heightTexel*16);
		SubTexture rightSideTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*8, texture.u0+widthTexel*24, texture.v0+heightTexel*16);
		SubTexture leftSideTexture = new SubTexture(currentTexture.texture, texture.u0, texture.v0+heightTexel*8, texture.u0+widthTexel*8, texture.v0+heightTexel*16);
		
		if (data > 1)  // TODO: add lighting for each face based on rotation
		{  
			if (data == 2)
			{
				// Facing north
				rotation = Rotation.Clockwise;
				angle = 180;
				zOffset += offSet*4;
			}
			if (data == 3)
			{
				// Facing south
				angle = 0;
				zOffset -= offSet*4;
			}
			else if (data == 4)
			{
				// Facing west
				rotation = Rotation.Clockwise;
				angle = -90;
				xOffset += offSet*4;
			}
			else if (data == 5)
			{
				// Facing east
				rotation = Rotation.Clockwise;
				angle = 90;
				xOffset -= offSet*4;
			}
		}
		
		//Top
		//float lightness = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);

		subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*4),
						new Vector3f(offSet*12,	offSet*8,	offSet*4),
						new Vector3f(offSet*12,	offSet*8,	offSet*12),
						new Vector3f(offSet*4,	offSet*8,	offSet*12),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						topTexture);

		
		//Bottom
		//lightness = world.getLight(rawChunk.getChunkCoord(), x, y-1, z, LightFace.Top);
		
		subMesh.addQuad(new Vector3f(offSet*4,	0,	offSet*12),
						new Vector3f(offSet*12,	0,	offSet*12),
						new Vector3f(offSet*12,	0,	offSet*4),
						new Vector3f(offSet*4,	0,	offSet*4),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						bottomTexture);

	
		//North
		//lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		
		subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*4),
						new Vector3f(offSet*4,	offSet*8,	offSet*4),
						new Vector3f(offSet*4,	0,			offSet*4),
						new Vector3f(offSet*12,	0,			offSet*4),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						rearTexture);

		
		//South
		//lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*12),
						new Vector3f(offSet*12,	offSet*8,	offSet*12),
						new Vector3f(offSet*12,	0,			offSet*12),
						new Vector3f(offSet*4,	0,			offSet*12),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						faceTexture);
		
		//East	
		//lightness = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
			
		subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*12),
						new Vector3f(offSet*12,	offSet*8,	offSet*4),
						new Vector3f(offSet*12,	0,			offSet*4),
						new Vector3f(offSet*12,	0,			offSet*12),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						rightSideTexture);
			
		//West
		//lightness = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		
		subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*4),
						new Vector3f(offSet*4,	offSet*8,	offSet*12),
						new Vector3f(offSet*4,		0,		offSet*12),
						new Vector3f(offSet*4,		0,		offSet*4),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						leftSideTexture);
		
		
		
		if(data > 1)
			subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset+offSet*4, zOffset, rotation, angle);
		else
			subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), x, y, z, rotation, angle);
			
	}
}
