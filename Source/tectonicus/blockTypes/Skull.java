/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.Player;
import tectonicus.raw.RawChunk;
import tectonicus.raw.BlockEntity;
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
	private final SubTexture dtexture;

	private Colour4f colour;

	public Skull(String name, SubTexture texture, SubTexture ctexture, SubTexture stexture, SubTexture wtexture, SubTexture ztexture, SubTexture dtexture) 
	{
		this.name = name;
		
		this.texture = texture;
		this.ctexture = ctexture;
		this.stexture = stexture;
		this.wtexture = wtexture;
		this.ztexture = ztexture;
		this.dtexture = dtexture;
		
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
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		final Vector4f light = new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a);
		
		final float offSet = 1.0f / 16.0f;
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		boolean dragonHead = false;
		
		SubTexture currentTexture = null;
		
		for (BlockEntity entity : rawChunk.getSkulls())
		{
			if (entity.getLocalX() == x && entity.getLocalY() == y && entity.getLocalZ() == z)
			{
				rotation = Rotation.AntiClockwise;
				angle = 90 / 4.0f * entity.blockData + 180;
				
				if (entity.blockId == 0)
					currentTexture = stexture;
				else if (entity.blockId == 1)
					currentTexture = wtexture;
				else if (entity.blockId == 2)
					currentTexture = ztexture;
				else if (entity.blockId == 3)
					currentTexture = texture;
				else if (entity.blockId == 4)
					currentTexture = ctexture;
				else if (entity.blockId == 5)
					currentTexture = dtexture;
				
				dragonHead = entity.blockId == 5 ? true : false;
				
				Player player = new Player(entity.text1, entity.text2, entity.text3);
				if(!player.getSkinURL().equals(""))
				{
					currentTexture = world.getTexturePack().findTexture(world.getPlayerSkinCache().fetchSkin(player), "ph/"+entity.text1);
				}
				
				break;
			}
		}
		
		 float widthTexel;
		 float heightTexel;
		if (currentTexture.texture.getWidth() == currentTexture.texture.getHeight())
		{
			widthTexel = 1.0f / 64.0f;
			heightTexel = 1.0f / 64.0f;
		}
		else
		{
			widthTexel = 1.0f / 64.0f;
			heightTexel = 1.0f / 32.0f;
		}
		
		SubTexture topTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0, texture.u0+widthTexel*16, texture.v0+heightTexel*8);
		SubTexture bottomTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*16, texture.v0, texture.u0+widthTexel*24, texture.v0+heightTexel*8);
		SubTexture faceTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*8, texture.u0+widthTexel*16, texture.v0+heightTexel*16);
		SubTexture rearTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*24, texture.v0+heightTexel*8, texture.u0+widthTexel*32, texture.v0+heightTexel*16);
		SubTexture rightSideTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*8, texture.u0+widthTexel*24, texture.v0+heightTexel*16);
		SubTexture leftSideTexture = new SubTexture(currentTexture.texture, texture.u0, texture.v0+heightTexel*8, texture.u0+widthTexel*8, texture.v0+heightTexel*16);
		
		SubTexture mouthTexture = null;
		SubTexture earTexture = null;
		SubTexture nostrilTexture = null;
		
		if(dragonHead)
		{
			widthTexel = heightTexel = 1.0f / 16.0f;
			float texel = 1.0f / 16.0f / 16.0f;
			topTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0+(heightTexel*2-texel*2), texture.u0+widthTexel*9, texture.v0+(heightTexel*3-texel*2));
			bottomTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*9, texture.v0+(heightTexel*2-texel*2), texture.u0+widthTexel*10, texture.v0+(heightTexel*3-texel*2));
			faceTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0+(heightTexel*3-texel*2), texture.u0+widthTexel*9, texture.v0+(heightTexel*4-texel*2));
			rearTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*10, texture.v0+(heightTexel*3-texel*2), texture.u0+widthTexel*11, texture.v0+(heightTexel*4-texel*2));
			rightSideTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*9, texture.v0+(heightTexel*3-texel*2), texture.u0+widthTexel*10, texture.v0+(heightTexel*4-texel*2));
			leftSideTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*7, texture.v0+(heightTexel*3-texel*2), texture.u0+widthTexel*8, texture.v0+(heightTexel*4-texel*2));
			
			earTexture = new SubTexture(currentTexture.texture, texture.u0, texture.v0+texel*6, texture.u0+texel*16, texture.v0+texel*12);
			mouthTexture = new SubTexture(currentTexture.texture, texture.u0, texture.v0+texel*16, texture.u0+texel*16, texture.v0+texel*35); // Not the actual texture, but it looks close enough
		}
		
		
		if (data > 1)
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
		
		
		if (dragonHead)
		{
			//Top
			subMesh.addQuad(new Vector3f(offSet*2,	offSet*12,	offSet*3),
							new Vector3f(offSet*14,	offSet*12,	offSet*3),
							new Vector3f(offSet*14,	offSet*12,	offSet*15),
							new Vector3f(offSet*2,	offSet*12,	offSet*15),
							light, topTexture);
			
			//Bottom
			subMesh.addQuad(new Vector3f(offSet*2,	0,	offSet*15),
							new Vector3f(offSet*14,	0,	offSet*15),
							new Vector3f(offSet*14,	0,	offSet*3),
							new Vector3f(offSet*2,	0,	offSet*3),
							light, bottomTexture);
			
			//North
			subMesh.addQuad(new Vector3f(offSet*14,	offSet*12,	offSet*3),
							new Vector3f(offSet*2,	offSet*12,	offSet*3),
							new Vector3f(offSet*2,	0,			offSet*3),
							new Vector3f(offSet*14,	0,			offSet*3),
							light, rearTexture);

			
			//South
			subMesh.addQuad(new Vector3f(offSet*2,	offSet*12,	offSet*15),
							new Vector3f(offSet*14,	offSet*12,	offSet*15),
							new Vector3f(offSet*14,	0,			offSet*15),
							new Vector3f(offSet*2,	0,			offSet*15),
							light, faceTexture);
			
			//East
			subMesh.addQuad(new Vector3f(offSet*14,	offSet*12,	offSet*15),
							new Vector3f(offSet*14,	offSet*12,	offSet*3),
							new Vector3f(offSet*14,	0,			offSet*3),
							new Vector3f(offSet*14,	0,			offSet*15),
							light, rightSideTexture);
				
			//West
			subMesh.addQuad(new Vector3f(offSet*2,	offSet*12,	offSet*3),
							new Vector3f(offSet*2,	offSet*12,	offSet*15),
							new Vector3f(offSet*2,		0,		offSet*15),
							new Vector3f(offSet*2,		0,		offSet*3),
							light, leftSideTexture);
			
			SubMesh.addBlock(subMesh, offSet*4.25f, offSet*12, offSet*6, offSet*1.5f, offSet*3.25f, offSet*4.25f, light, earTexture, earTexture, earTexture); // Left ear
			SubMesh.addBlock(subMesh, offSet*10.25f, offSet*12, offSet*6, offSet*1.5f, offSet*3.25f, offSet*4.25f, light, earTexture, earTexture, earTexture); // Right ear			
			SubMesh.addBlock(subMesh, offSet*3, offSet*3, offSet*15, offSet*10, offSet*4, offSet*11, light, mouthTexture, mouthTexture, mouthTexture); //Mouth
			SubMesh.addBlock(subMesh, offSet*3, 0, offSet*15, offSet*10, offSet, offSet*11, light, mouthTexture, mouthTexture, mouthTexture); // Mouth
			SubMesh.addBlock(subMesh, offSet*4, offSet*7, offSet*21, offSet*2, offSet*2, offSet*3.5f, light, rightSideTexture, rightSideTexture, rightSideTexture); // Left nostril
			SubMesh.addBlock(subMesh, offSet*10, offSet*7, offSet*21, offSet*2, offSet*2, offSet*3.5f, light, rightSideTexture, rightSideTexture, rightSideTexture); // Right nostril
		}
		else
		{
			//Top
			subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*4),
							new Vector3f(offSet*12,	offSet*8,	offSet*4),
							new Vector3f(offSet*12,	offSet*8,	offSet*12),
							new Vector3f(offSet*4,	offSet*8,	offSet*12),
							new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
							topTexture);
	
			
			//Bottom
			subMesh.addQuad(new Vector3f(offSet*4,	0,	offSet*12),
							new Vector3f(offSet*12,	0,	offSet*12),
							new Vector3f(offSet*12,	0,	offSet*4),
							new Vector3f(offSet*4,	0,	offSet*4),
							new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
							bottomTexture);
	
		
			//North
			subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*4),
							new Vector3f(offSet*4,	offSet*8,	offSet*4),
							new Vector3f(offSet*4,	0,			offSet*4),
							new Vector3f(offSet*12,	0,			offSet*4),
							new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
							rearTexture);
	
			
			//South
			subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*12),
							new Vector3f(offSet*12,	offSet*8,	offSet*12),
							new Vector3f(offSet*12,	0,			offSet*12),
							new Vector3f(offSet*4,	0,			offSet*12),
							new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
							faceTexture);
			
			//East
			subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*12),
							new Vector3f(offSet*12,	offSet*8,	offSet*4),
							new Vector3f(offSet*12,	0,			offSet*4),
							new Vector3f(offSet*12,	0,			offSet*12),
							new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
							rightSideTexture);
				
			//West
			subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*4),
							new Vector3f(offSet*4,	offSet*8,	offSet*12),
							new Vector3f(offSet*4,		0,		offSet*12),
							new Vector3f(offSet*4,		0,		offSet*4),
							new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
							leftSideTexture);
		}

		if(data > 1)
			subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset+offSet*4, zOffset, rotation, angle);
		else
			subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), x, y, z, rotation, angle);			
	}
}
