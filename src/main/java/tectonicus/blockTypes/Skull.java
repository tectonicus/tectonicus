/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.awt.image.BufferedImage;

import org.joml.Vector3f;
import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.Player;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SkullEntity;
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
		if(dtexture != null)
			this.dtexture = dtexture;
		else
			this.dtexture = texture;
		
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
		String xyz = "x" + x + "y" + y + "z" + z;
                SkullEntity skullEntity = rawChunk.getSkulls().get(xyz);
                if (skullEntity == null) {
                        // There is no entity when rendering item icons. Use default values...
                        skullEntity = new SkullEntity(0, 0, 0, 0, 0, 0, 0, 0, 0);
                }
                addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry, skullEntity);
        }

        public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry, SkullEntity entity) 
        {				
		SubMesh subMesh = new SubMesh();

		//1.13+
		final BlockProperties properties = rawChunk.getBlockState(x, y, z);
		final String blockName = rawChunk.getBlockName(x, y, z);
		String facing = null;
		String rotationString = null;
		if (properties != null) {
			facing = properties.get("facing");
			rotationString = properties.get("rotation");
		}
		int rotationValue;
		if (rotationString != null) {
			rotationValue = Integer.parseInt(rotationString);
		} else {
			rotationValue = entity.getRotation();
		}


		final int data = rawChunk.getBlockData(x, y, z);
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, world.getNightLightAdjustment());
		final Vector4f light = new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a);
		
		final float offSet = 1.0f / 16.0f;
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;

		Rotation rotation = Rotation.AntiClockwise;

		float angle = 90 / 4.0f * rotationValue + 180;
		
		int blockId = entity.getSkullType();

		SubTexture currentTexture = texture;
		if (blockId == 0)
			currentTexture = stexture;
		else if (blockId == 1)
			currentTexture = wtexture;
		else if (blockId == 2)
			currentTexture = ztexture;
		//blockId == 3 is default texture
		else if (blockId == 4)
			currentTexture = ctexture;
		else if (blockId == 5)
			currentTexture = dtexture;
		
		final boolean dragonHead = blockId == 5 || blockName.equals("minecraft:dragon_head") || blockName.equals("minecraft:dragon_wall_head");
                final boolean piglinHead = blockId == 6 || blockName.equals("minecraft:piglin_head") || blockName.equals("minecraft:piglin_wall_head");
		
		Player player = new Player(entity.getName(), entity.getUUID(), entity.getSkinURL());
		if(!player.getSkinURL().equals(""))
		{
			BufferedImage skin = world.getPlayerSkinCache().fetchSkin(player);
			if (skin != null)
				currentTexture = world.getTexturePack().findTexture(skin, "ph/"+entity.getName());
			else
				currentTexture = texture;
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
                SubTexture teethTexture = null;
		
		if (dragonHead)
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
                else if (piglinHead)
                {
                        topTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0, texture.u0+widthTexel*18, texture.v0+heightTexel*8);
                        bottomTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*18, texture.v0, texture.u0+widthTexel*28, texture.v0+heightTexel*8);
                        faceTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*8, texture.u0+widthTexel*18, texture.v0+heightTexel*16);
                        rearTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*26, texture.v0+heightTexel*8, texture.u0+widthTexel*36, texture.v0+heightTexel*16);
                        rightSideTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*18, texture.v0+heightTexel*8, texture.u0+widthTexel*26, texture.v0+heightTexel*16);
                        leftSideTexture = new SubTexture(currentTexture.texture, texture.u0, texture.v0+widthTexel*8, texture.u0+heightTexel*8, texture.v0+heightTexel*16);
                        
                        earTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*40, texture.v0+heightTexel*10, texture.u0+widthTexel*44, texture.v0+heightTexel*15);
                        mouthTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*32, texture.v0+heightTexel*2, texture.u0+widthTexel*36, texture.v0+heightTexel*6);
                        teethTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*3, texture.v0+heightTexel*1, texture.u0+widthTexel*4, texture.v0+heightTexel*3);
                }
		
		
		if (data > 1 || facing != null)
		{  
			if (data == 2 || (facing != null && facing.equals("north")))
			{
				// Facing north
				rotation = Rotation.Clockwise;
				angle = 180;
				zOffset += offSet*4;
			}
			if (data == 3 || (facing != null && facing.equals("south")))
			{
				// Facing south
				angle = 0;
				zOffset -= offSet*4;
			}
			else if (data == 4 || (facing != null && facing.equals("west")))
			{
				// Facing west
				rotation = Rotation.Clockwise;
				angle = -90;
				xOffset += offSet*4;
			}
			else if (data == 5 || (facing != null && facing.equals("east")))
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
                                        new Vector3f(offSet*2,	0,		offSet*3),
                                        new Vector3f(offSet*14,	0,		offSet*3),
                                        light, rearTexture);

			
			//South
			subMesh.addQuad(new Vector3f(offSet*2,	offSet*12,	offSet*15),
                                        new Vector3f(offSet*14,	offSet*12,	offSet*15),
                                        new Vector3f(offSet*14,	0,		offSet*15),
                                        new Vector3f(offSet*2,	0,		offSet*15),
                                        light, faceTexture);
			
			//East
			subMesh.addQuad(new Vector3f(offSet*14,	offSet*12,	offSet*15),
                                        new Vector3f(offSet*14,	offSet*12,	offSet*3),
                                        new Vector3f(offSet*14,	0,		offSet*3),
                                        new Vector3f(offSet*14,	0,		offSet*15),
                                        light, rightSideTexture);
				
			//West
			subMesh.addQuad(new Vector3f(offSet*2,	offSet*12,	offSet*3),
                                        new Vector3f(offSet*2,	offSet*12,	offSet*15),
                                        new Vector3f(offSet*2,		0,	offSet*15),
                                        new Vector3f(offSet*2,		0,	offSet*3),
                                        light, leftSideTexture);
			
			SubMesh.addBlock(subMesh, offSet*4.25f, offSet*12, offSet*6, offSet*1.5f, offSet*3.25f, offSet*4.25f, light, earTexture, earTexture, earTexture); // Left ear
			SubMesh.addBlock(subMesh, offSet*10.25f, offSet*12, offSet*6, offSet*1.5f, offSet*3.25f, offSet*4.25f, light, earTexture, earTexture, earTexture); // Right ear			
			SubMesh.addBlock(subMesh, offSet*3, offSet*3, offSet*15, offSet*10, offSet*4, offSet*11, light, mouthTexture, mouthTexture, mouthTexture); //Mouth
			SubMesh.addBlock(subMesh, offSet*3, 0, offSet*15, offSet*10, offSet, offSet*11, light, mouthTexture, mouthTexture, mouthTexture); // Mouth
			SubMesh.addBlock(subMesh, offSet*4, offSet*7, offSet*21, offSet*2, offSet*2, offSet*3.5f, light, rightSideTexture, rightSideTexture, rightSideTexture); // Left nostril
			SubMesh.addBlock(subMesh, offSet*10, offSet*7, offSet*21, offSet*2, offSet*2, offSet*3.5f, light, rightSideTexture, rightSideTexture, rightSideTexture); // Right nostril
		}
                else if (piglinHead)
                {
                    	//Top
			subMesh.addQuad(new Vector3f(offSet*3,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*13,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*13,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*3,	offSet*8,	offSet*12),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        topTexture);
	
			
			//Bottom
			subMesh.addQuad(new Vector3f(offSet*3,	0,	offSet*12),
                                        new Vector3f(offSet*13,	0,	offSet*12),
                                        new Vector3f(offSet*13,	0,	offSet*4),
                                        new Vector3f(offSet*3,	0,	offSet*4),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        bottomTexture);
	
		
			//North
			subMesh.addQuad(new Vector3f(offSet*13,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*3,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*3,	0,		offSet*4),
                                        new Vector3f(offSet*13,	0,		offSet*4),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        rearTexture);
	
			
			//South
			subMesh.addQuad(new Vector3f(offSet*3,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*13,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*13,	0,		offSet*12),
                                        new Vector3f(offSet*3,	0,		offSet*12),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        faceTexture);
			
			//East
			subMesh.addQuad(new Vector3f(offSet*13,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*13,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*13,	0,		offSet*4),
                                        new Vector3f(offSet*13,	0,		offSet*12),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        rightSideTexture);
				
			//West
			subMesh.addQuad(new Vector3f(offSet*3,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*3,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*3,		0,	offSet*12),
                                        new Vector3f(offSet*3,		0,	offSet*4),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        leftSideTexture);
                        
                        
                        // Right ear
                        subMesh.addQuad(new Vector3f(offSet*3,	offSet*7,	offSet*6),
                                        new Vector3f(offSet*3,	offSet*7,	offSet*10),
                                        new Vector3f(0,     	offSet*3,	offSet*10),
                                        new Vector3f(0,     	offSet*3,	offSet*6),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        earTexture);
                        subMesh.addQuad(new Vector3f(offSet*3, 	offSet*7,	offSet*10),
                                        new Vector3f(offSet*4,	offSet*6,	offSet*10),
                                        new Vector3f(offSet*1,	offSet*2,	offSet*10),
                                        new Vector3f(0,         offSet*3,	offSet*10),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        topTexture);
                        subMesh.addQuad(new Vector3f(offSet*3, 	offSet*7,	offSet*6),
                                        new Vector3f(0, 	offSet*3,	offSet*6),
                                        new Vector3f(offSet*1,	offSet*2,	offSet*6),
                                        new Vector3f(offSet*4,	offSet*6,	offSet*6),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        topTexture);
                        
                        // Left ear
			subMesh.addQuad(new Vector3f(offSet*13,	offSet*7,	offSet*10),
                                        new Vector3f(offSet*13,	offSet*7,	offSet*6),
                                        new Vector3f(offSet*16,	offSet*3,	offSet*6),
                                        new Vector3f(offSet*16,	offSet*3,	offSet*10),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        earTexture);
                        subMesh.addQuad(new Vector3f(offSet*13, offSet*7,	offSet*10),
                                        new Vector3f(offSet*16,	offSet*3,	offSet*10),
                                        new Vector3f(offSet*15,	offSet*2,	offSet*10),
                                        new Vector3f(offSet*12,	offSet*6,	offSet*10),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        topTexture);
                        subMesh.addQuad(new Vector3f(offSet*13, offSet*7,	offSet*6),
                                        new Vector3f(offSet*12,	offSet*6,	offSet*6),
                                        new Vector3f(offSet*15,	offSet*2,	offSet*6),
                                        new Vector3f(offSet*16,	offSet*3,	offSet*6),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        topTexture);                        
                        // Snout
                        subMesh.addQuad(new Vector3f(offSet*6,	offSet*4,	offSet*13),
                                        new Vector3f(offSet*10,	offSet*4,	offSet*13),
                                        new Vector3f(offSet*10,	0,		offSet*13),
                                        new Vector3f(offSet*6,	0,		offSet*13),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        mouthTexture);
                        subMesh.addQuad(new Vector3f(offSet*6,	offSet*4,	offSet*12),
                                        new Vector3f(offSet*10,	offSet*4,	offSet*12),
                                        new Vector3f(offSet*10,	offSet*4,	offSet*13),
                                        new Vector3f(offSet*6,	offSet*4,	offSet*13),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        topTexture);
                        
                        // Teeth
                        SubMesh.addBlock(subMesh, offSet*5, 0, offSet*12, offSet, offSet*2, offSet, light, teethTexture, teethTexture, teethTexture);
                        SubMesh.addBlock(subMesh, offSet*10, 0, offSet*12, offSet, offSet*2, offSet, light, teethTexture, teethTexture, teethTexture);
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
                                        new Vector3f(offSet*4,	0,		offSet*4),
                                        new Vector3f(offSet*12,	0,		offSet*4),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        rearTexture);
	
			
			//South
			subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*12,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*12,	0,		offSet*12),
                                        new Vector3f(offSet*4,	0,		offSet*12),
                                        new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
                                        faceTexture);
			
			//East
			subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*12),
                                        new Vector3f(offSet*12,	offSet*8,	offSet*4),
                                        new Vector3f(offSet*12,	0,		offSet*4),
                                        new Vector3f(offSet*12,	0,		offSet*12),
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

		if(data > 1 || facing != null)
			subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset+offSet*4+entity.getYOffset(), zOffset, rotation, angle);
		else
			subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), x, y+entity.getYOffset(), z, rotation, angle);			
	}
}
