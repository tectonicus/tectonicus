/*
 * Copyright (c) 2024, Tectonicus contributors.  All rights reserved.
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
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.ArmorItem;
import tectonicus.raw.ArmorStandEntity;
import tectonicus.raw.ArmorTrimTag;
import tectonicus.raw.DisplayTag;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SkullEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;

public class ArmorStand implements BlockType
{
        private interface ArmorMeshBuilder {
                void build(int x, int y, int z, Geometry geometry, Vector4f colour, float angle, SubTexture texture, int offsetMultiplier);
        }
    
        private static final float UNIT = 1.0f / 16.0f;
        private static final float EPSILON = 0.005f / 16.0f; // So that different layer textures are not on the same plane
    
	private final String name;
        
        private final TexturePack texturePack;
        
        private final SubTexture texture;
	
        private final SubTexture baseTopTexture;
        private final SubTexture baseSideTexture;
        private final SubTexture legSideTexture;
        private final SubTexture hipsFrontTexture;
        private final SubTexture hipsSideTexture;
        private final SubTexture torsoSideTexture;
        private final SubTexture shouldersFrontTexture;
        private final SubTexture shouldersSideTexture;
        private final SubTexture neckTopTexture;
        private final SubTexture neckSideTexture;

        public ArmorStand(String name, TexturePack texturePack)
	{
		this.name = name;
                
                this.texturePack = texturePack;
                
                texture = texturePack.findTexture("assets/minecraft/textures/entity/armorstand/wood.png");
                
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 64.0f;
                
		baseTopTexture = new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*32, texture.u0+widthTexel*24, texture.v0+heightTexel*44);
		baseSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*44, texture.u0+widthTexel*24, texture.v0+heightTexel*45);
		legSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*10, texture.v0+heightTexel*2, texture.u0+widthTexel*12, texture.v0+heightTexel*13);
		hipsFrontTexture = new SubTexture(texture.texture, texture.u0+widthTexel*2, texture.v0+heightTexel*50, texture.u0+widthTexel*10, texture.v0+heightTexel*52);
		hipsSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*50, texture.u0+widthTexel*2, texture.v0+heightTexel*52);
                torsoSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*2, texture.u0+widthTexel*18, texture.v0+heightTexel*9);
		shouldersFrontTexture = new SubTexture(texture.texture, texture.u0+widthTexel*3, texture.v0+heightTexel*29, texture.u0+widthTexel*15, texture.v0+heightTexel*32);
		shouldersSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*29, texture.u0+widthTexel*3, texture.v0+heightTexel*32);
                neckTopTexture = new SubTexture(texture.texture, texture.u0+widthTexel*2, texture.v0+heightTexel*0, texture.u0+widthTexel*4, texture.v0+heightTexel*2);
                neckSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*2, texture.u0+widthTexel*2, texture.v0+heightTexel*8);
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
	public void addInteriorGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
                addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
                for (ArmorStandEntity entity : rawChunk.getArmorStands()) {
                        x = entity.getLocalX();
                        y = entity.getLocalY();
                        z = entity.getLocalZ();
                    
                        final Vector4f colour = new Vector4f(1, 1, 1, 1);

                        if (!entity.getInvisible() && !entity.getNoBasePlate()) {
                                buildBaseMesh(x, y, z, geometry, colour);
                        }
                        if (!entity.getInvisible()) {
                                buildStandMesh(x, y, z, geometry, colour, entity.getYaw());
                        }
                        
                        ArmorItem feetArmor = entity.getFeetArmor();
                        ArmorItem legsArmor = entity.getLegsArmor();
                        ArmorItem chestArmor = entity.getChestArmor();
                        ArmorItem headArmor = entity.getHeadArmor();
                        
                        if (feetArmor != null) {
                                buildArmorMesh(x, y, z, world, registry, rawChunk, geometry, entity, colour, feetArmor, (byte)1, this::buildFeetArmorMesh);
                        }
                        if (legsArmor != null) {
                                buildArmorMesh(x, y, z, world, registry, rawChunk, geometry, entity, colour, legsArmor, (byte)2, this::buildLegsArmorMesh);
                        }
                        if (chestArmor != null) {
                                buildArmorMesh(x, y, z, world, registry, rawChunk, geometry, entity, colour, chestArmor, (byte)1, this::buildChestArmorMesh);
                        }
                        if (headArmor != null) {
                                buildArmorMesh(x, y, z, world, registry, rawChunk, geometry, entity, colour, headArmor, (byte)1, this::buildHeadArmorMesh);
                        }
                }
	}
        
        private void buildBaseMesh(int x, int y, int z, Geometry geometry, Vector4f colour) {
                SubMesh mesh = new SubMesh();
		
		// Front
		mesh.addQuad(new Vector3f(2*UNIT, 1*UNIT, 14*UNIT), new Vector3f(14*UNIT, 1*UNIT, 14*UNIT), new Vector3f(14*UNIT, 0*UNIT, 14*UNIT), new Vector3f(2*UNIT, 0*UNIT, 14*UNIT), colour, baseSideTexture);
		// Back
		mesh.addQuad(new Vector3f(14*UNIT, 1*UNIT, 2*UNIT), new Vector3f(2*UNIT, 1*UNIT, 2*UNIT), new Vector3f(2*UNIT, 0*UNIT, 2*UNIT), new Vector3f(14*UNIT, 0*UNIT, 2*UNIT), colour, baseSideTexture);
		// Top
		mesh.addQuad(new Vector3f(2*UNIT, 1*UNIT, 2*UNIT), new Vector3f(14*UNIT, 1*UNIT, 2*UNIT), new Vector3f(14*UNIT, 1*UNIT, 14*UNIT), new Vector3f(2*UNIT, 1*UNIT, 14*UNIT), colour, baseTopTexture);
		// Left edge
		mesh.addQuad(new Vector3f(2*UNIT, 1*UNIT, 2*UNIT), new Vector3f(2*UNIT, 1*UNIT, 14*UNIT), new Vector3f(2*UNIT, 0*UNIT, 14*UNIT), new Vector3f(2*UNIT, 0*UNIT, 2*UNIT), colour, baseSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(14*UNIT, 1*UNIT, 14*UNIT), new Vector3f(14*UNIT, 1*UNIT, 2*UNIT), new Vector3f(14*UNIT, 0*UNIT, 2*UNIT), new Vector3f(14*UNIT, 0*UNIT, 14*UNIT), colour, baseSideTexture);
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
        }

        private void buildStandMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float angle) {
                SubMesh mesh = new SubMesh();


                // Left leg
                
		// Front
		mesh.addQuad(new Vector3f(5*UNIT, 12*UNIT, 9*UNIT), new Vector3f(7*UNIT, 12*UNIT, 9*UNIT), new Vector3f(7*UNIT, 1*UNIT, 9*UNIT), new Vector3f(5*UNIT, 1*UNIT, 9*UNIT), colour, legSideTexture);
		// Back
		mesh.addQuad(new Vector3f(7*UNIT, 12*UNIT, 7*UNIT), new Vector3f(5*UNIT, 12*UNIT, 7*UNIT), new Vector3f(5*UNIT, 1*UNIT, 7*UNIT), new Vector3f(7*UNIT, 1*UNIT, 7*UNIT), colour, legSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(5*UNIT, 12*UNIT, 7*UNIT), new Vector3f(5*UNIT, 12*UNIT, 9*UNIT), new Vector3f(5*UNIT, 1*UNIT, 9*UNIT), new Vector3f(5*UNIT, 1*UNIT, 7*UNIT), colour, legSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(7*UNIT, 12*UNIT, 9*UNIT), new Vector3f(7*UNIT, 12*UNIT, 7*UNIT), new Vector3f(7*UNIT, 1*UNIT, 7*UNIT), new Vector3f(7*UNIT, 1*UNIT, 9*UNIT), colour, legSideTexture);


                // Right leg
                
		// Front
		mesh.addQuad(new Vector3f(9*UNIT, 12*UNIT, 9*UNIT), new Vector3f(11*UNIT, 12*UNIT, 9*UNIT), new Vector3f(11*UNIT, 1*UNIT, 9*UNIT), new Vector3f(9*UNIT, 1*UNIT, 9*UNIT), colour, legSideTexture);
		// Back
		mesh.addQuad(new Vector3f(11*UNIT, 12*UNIT, 7*UNIT), new Vector3f(9*UNIT, 12*UNIT, 7*UNIT), new Vector3f(9*UNIT, 1*UNIT, 7*UNIT), new Vector3f(11*UNIT, 1*UNIT, 7*UNIT), colour, legSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(9*UNIT, 12*UNIT, 7*UNIT), new Vector3f(9*UNIT, 12*UNIT, 9*UNIT), new Vector3f(9*UNIT, 1*UNIT, 9*UNIT), new Vector3f(9*UNIT, 1*UNIT, 7*UNIT), colour, legSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(11*UNIT, 12*UNIT, 9*UNIT), new Vector3f(11*UNIT, 12*UNIT, 7*UNIT), new Vector3f(11*UNIT, 1*UNIT, 7*UNIT), new Vector3f(11*UNIT, 1*UNIT, 9*UNIT), colour, legSideTexture);

                
                // Hips
                
		// Front
		mesh.addQuad(new Vector3f(4*UNIT, 14*UNIT, 9*UNIT), new Vector3f(12*UNIT, 14*UNIT, 9*UNIT), new Vector3f(12*UNIT, 12*UNIT, 9*UNIT), new Vector3f(4*UNIT, 12*UNIT, 9*UNIT), colour, hipsFrontTexture);
		// Back
		mesh.addQuad(new Vector3f(12*UNIT, 14*UNIT, 7*UNIT), new Vector3f(4*UNIT, 14*UNIT, 7*UNIT), new Vector3f(4*UNIT, 12*UNIT, 7*UNIT), new Vector3f(12*UNIT, 12*UNIT, 7*UNIT), colour, hipsFrontTexture);
		// Top
		mesh.addQuad(new Vector3f(4*UNIT, 14*UNIT, 7*UNIT), new Vector3f(12*UNIT, 14*UNIT, 7*UNIT), new Vector3f(12*UNIT, 14*UNIT, 9*UNIT), new Vector3f(4*UNIT, 14*UNIT, 9*UNIT), colour, hipsFrontTexture);
		// Left edge
		mesh.addQuad(new Vector3f(4*UNIT, 14*UNIT, 7*UNIT), new Vector3f(4*UNIT, 14*UNIT, 9*UNIT), new Vector3f(4*UNIT, 12*UNIT, 9*UNIT), new Vector3f(4*UNIT, 12*UNIT, 7*UNIT), colour, hipsSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(12*UNIT, 14*UNIT, 9*UNIT), new Vector3f(12*UNIT, 14*UNIT, 7*UNIT), new Vector3f(12*UNIT, 12*UNIT, 7*UNIT), new Vector3f(12*UNIT, 12*UNIT, 9*UNIT), colour, hipsSideTexture);


                // Left part of torso
                
		// Front
		mesh.addQuad(new Vector3f(5*UNIT, 21*UNIT, 9*UNIT), new Vector3f(7*UNIT, 21*UNIT, 9*UNIT), new Vector3f(7*UNIT, 14*UNIT, 9*UNIT), new Vector3f(5*UNIT, 14*UNIT, 9*UNIT), colour, torsoSideTexture);
		// Back
		mesh.addQuad(new Vector3f(7*UNIT, 21*UNIT, 7*UNIT), new Vector3f(5*UNIT, 21*UNIT, 7*UNIT), new Vector3f(5*UNIT, 14*UNIT, 7*UNIT), new Vector3f(7*UNIT, 14*UNIT, 7*UNIT), colour, torsoSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(5*UNIT, 21*UNIT, 7*UNIT), new Vector3f(5*UNIT, 21*UNIT, 9*UNIT), new Vector3f(5*UNIT, 14*UNIT, 9*UNIT), new Vector3f(5*UNIT, 14*UNIT, 7*UNIT), colour, torsoSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(7*UNIT, 21*UNIT, 9*UNIT), new Vector3f(7*UNIT, 21*UNIT, 7*UNIT), new Vector3f(7*UNIT, 14*UNIT, 7*UNIT), new Vector3f(7*UNIT, 14*UNIT, 9*UNIT), colour, torsoSideTexture);


                // Right part of torso
                
		// Front
		mesh.addQuad(new Vector3f(9*UNIT, 21*UNIT, 9*UNIT), new Vector3f(11*UNIT, 21*UNIT, 9*UNIT), new Vector3f(11*UNIT, 14*UNIT, 9*UNIT), new Vector3f(9*UNIT, 14*UNIT, 9*UNIT), colour, torsoSideTexture);
		// Back
		mesh.addQuad(new Vector3f(11*UNIT, 21*UNIT, 7*UNIT), new Vector3f(9*UNIT, 21*UNIT, 7*UNIT), new Vector3f(9*UNIT, 14*UNIT, 7*UNIT), new Vector3f(11*UNIT, 14*UNIT, 7*UNIT), colour, torsoSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(9*UNIT, 21*UNIT, 7*UNIT), new Vector3f(9*UNIT, 21*UNIT, 9*UNIT), new Vector3f(9*UNIT, 14*UNIT, 9*UNIT), new Vector3f(9*UNIT, 14*UNIT, 7*UNIT), colour, torsoSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(11*UNIT, 21*UNIT, 9*UNIT), new Vector3f(11*UNIT, 21*UNIT, 7*UNIT), new Vector3f(11*UNIT, 14*UNIT, 7*UNIT), new Vector3f(11*UNIT, 14*UNIT, 9*UNIT), colour, torsoSideTexture);

                
                // Shoulders
                
		// Front
		mesh.addQuad(new Vector3f(2*UNIT, 24*UNIT, 9.5f*UNIT), new Vector3f(14*UNIT, 24*UNIT, 9.5f*UNIT), new Vector3f(14*UNIT, 21*UNIT, 9.5f*UNIT), new Vector3f(2*UNIT, 21*UNIT, 9.5f*UNIT), colour, shouldersFrontTexture);
		// Back
		mesh.addQuad(new Vector3f(14*UNIT, 24*UNIT, 6.5f*UNIT), new Vector3f(2*UNIT, 24*UNIT, 6.5f*UNIT), new Vector3f(2*UNIT, 21*UNIT, 6.5f*UNIT), new Vector3f(14*UNIT, 21*UNIT, 6.5f*UNIT), colour, shouldersFrontTexture);
		// Top
		mesh.addQuad(new Vector3f(2*UNIT, 24*UNIT, 6.5f*UNIT), new Vector3f(14*UNIT, 24*UNIT, 6.5f*UNIT), new Vector3f(14*UNIT, 24*UNIT, 9.5f*UNIT), new Vector3f(2*UNIT, 24*UNIT, 9.5f*UNIT), colour, shouldersFrontTexture);
		// Left edge
		mesh.addQuad(new Vector3f(2*UNIT, 24*UNIT, 6.5f*UNIT), new Vector3f(2*UNIT, 24*UNIT, 9.5f*UNIT), new Vector3f(2*UNIT, 21*UNIT, 9.5f*UNIT), new Vector3f(2*UNIT, 21*UNIT, 6.5f*UNIT), colour, shouldersSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(14*UNIT, 24*UNIT, 9.5f*UNIT), new Vector3f(14*UNIT, 24*UNIT, 6.5f*UNIT), new Vector3f(14*UNIT, 21*UNIT, 6.5f*UNIT), new Vector3f(14*UNIT, 21*UNIT, 9.5f*UNIT), colour, shouldersSideTexture);


                // Neck/head
                
		// Front
		mesh.addQuad(new Vector3f(7*UNIT, 30*UNIT, 9*UNIT), new Vector3f(9*UNIT, 30*UNIT, 9*UNIT), new Vector3f(9*UNIT, 24*UNIT, 9*UNIT), new Vector3f(7*UNIT, 24*UNIT, 9*UNIT), colour, neckSideTexture);
		// Back
		mesh.addQuad(new Vector3f(9*UNIT, 30*UNIT, 7*UNIT), new Vector3f(7*UNIT, 30*UNIT, 7*UNIT), new Vector3f(7*UNIT, 24*UNIT, 7*UNIT), new Vector3f(9*UNIT, 24*UNIT, 7*UNIT), colour, neckSideTexture);
		// Top
		mesh.addQuad(new Vector3f(7*UNIT, 30*UNIT, 7*UNIT), new Vector3f(9*UNIT, 30*UNIT, 7*UNIT), new Vector3f(9*UNIT, 30*UNIT, 9*UNIT), new Vector3f(7*UNIT, 30*UNIT, 9*UNIT), colour, neckTopTexture);
		// Left edge
		mesh.addQuad(new Vector3f(7*UNIT, 30*UNIT, 7*UNIT), new Vector3f(7*UNIT, 30*UNIT, 9*UNIT), new Vector3f(7*UNIT, 24*UNIT, 9*UNIT), new Vector3f(7*UNIT, 24*UNIT, 7*UNIT), colour, neckSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(9*UNIT, 30*UNIT, 9*UNIT), new Vector3f(9*UNIT, 30*UNIT, 7*UNIT), new Vector3f(9*UNIT, 24*UNIT, 7*UNIT), new Vector3f(9*UNIT, 24*UNIT, 9*UNIT), colour, neckSideTexture);

                
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.AntiClockwise, angle);
        }
        
        private void buildArmorMesh(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry, ArmorStandEntity armorStand, Vector4f colour, ArmorItem armor, byte layer, ArmorMeshBuilder meshBuilder) {
                final float angle = armorStand.getYaw();
                                
                String armorMaterial = armor.id.substring("minecraft:".length(), armor.id.indexOf('_'));
                if (armorMaterial.equals("golden")) {
                        armorMaterial = "gold";
                }
                
                SubTexture layerTexture = texturePack.findTextureOrDefault(String.format("assets/minecraft/textures/models/armor/%s_layer_%d.png", armorMaterial, layer), null);
                SubTexture overlayLayerTexture = texturePack.findTextureOrDefault(String.format("assets/minecraft/textures/models/armor/%s_layer_%d_overlay.png", armorMaterial, layer), null);
                
                ArmorTrimTag armorTrim = armor.getTag(tectonicus.raw.ArmorTrimTag.class);
                if (armorTrim != null) {
                        final String pattern = armorTrim.pattern.substring("minecraft:".length());
                        final String suffix = layer == 2 ? "_leggings" : "";
                        final String material = armorTrim.material.substring("minecraft:".length());

                        final String trimTextureFile = String.format("assets/minecraft/textures/trims/models/armor/%s%s.png", pattern, suffix);
                        final String materialTextureFile = String.format("assets/minecraft/textures/trims/color_palettes/%s.png", material);
                        final String paletteTextureFile = String.format("assets/minecraft/textures/trims/color_palettes/trim_palette.png", pattern, suffix);
                        
                        SubTexture trimTexture = texturePack.findPalettedTexture(trimTextureFile, materialTextureFile, paletteTextureFile);

                        meshBuilder.build(x, y, z, geometry, colour, angle, trimTexture, 1);
                }
                
                if (layerTexture == null) {
                        // Armor texture not found. Maybe it is some other item (e.g. mob head)
                        // Try finding relevant item in block registry and add its geometry
                        buildOtherItemMesh(x, y, z, world, registry, rawChunk, geometry, armorStand, colour, armor);
                        return;
                }
                
                if (armorMaterial.equals("leather")) {
                        meshBuilder.build(x, y, z, geometry, colour, angle, overlayLayerTexture, 0);
                 
                        DisplayTag display = armor.getTag(DisplayTag.class);
                        colour = display == null
                                ? new Vector4f(106/255f, 64/255f, 41/255f, 1) // Default brown leather
                                : new Vector4f(((display.color >> 16) & 255)/255f, ((display.color >> 8) & 255)/255f, (display.color & 255)/255f, 1);
                }
		
                meshBuilder.build(x, y, z, geometry, colour, angle, layerTexture, -1);
        }
        
        private void buildOtherItemMesh(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry, ArmorStandEntity armorStand, Vector4f colour, ArmorItem armor) {
                BlockType blockType = registry.find(armor.id);
                if (blockType != null) {
                        if (blockType instanceof Skull) {
                                // We do have a mob head, but there is no skull entity on these coordinates (there is armor stand instead).
                                // Create entity and pass it to addEdgeGeometry via parameter so that the call does not fail with java.lang.NullPointerException
                                // when trying to find and work with skull entity from rawChunk.

                                int skullType = -1;
                                switch (armor.id) {
                                        case "minecraft:dragon_head":
                                                skullType = 5;
                                                break;
                                        case "minecraft:piglin_head":
                                                skullType = 6;
                                                break;
                                }
                                
                                // Convert rotation angle from degrees to 0..15 format used by skulls
                                float rotation = armorStand.getYaw() - 180;
                                while (rotation < 0) {
                                    rotation += 360;
                                }
                                rotation = rotation / 360f * 16;

                                // Use constructor that acceptas yOffset to elevate the skull to correct position on armor stand
                                SkullEntity skullEntity = new SkullEntity(0, 0, 0, x, y, z, skullType, (int)Math.round(rotation), 24*UNIT);
                                ((Skull)blockType).addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry, skullEntity);
                        } else if (blockType instanceof JackOLantern || armor.id.equals("minecraft:carved_pumpkin")) {
                                SubTexture frontTexture = texturePack.findTexture("assets/minecraft/textures/block/carved_pumpkin.png");
                                SubTexture sideTexture = texturePack.findTexture("assets/minecraft/textures/block/pumpkin_side.png");
                                SubTexture topTexture = texturePack.findTexture("assets/minecraft/textures/block/pumpkin_top.png");
                                
                                SubMesh frontMesh = new SubMesh();
                                SubMesh sideMesh = new SubMesh();
                                SubMesh topMesh = new SubMesh();

                                // Front
                                frontMesh.addQuad(new Vector3f(4*UNIT, 32*UNIT, 12*UNIT), new Vector3f(12*UNIT, 32*UNIT, 12*UNIT), new Vector3f(12*UNIT, 24*UNIT, 12*UNIT), new Vector3f(4*UNIT, 24*UNIT, 12*UNIT), colour, frontTexture);
                                // Back
                                sideMesh.addQuad(new Vector3f(12*UNIT, 32*UNIT, 4*UNIT), new Vector3f(4*UNIT, 32*UNIT, 4*UNIT), new Vector3f(4*UNIT, 24*UNIT, 4*UNIT), new Vector3f(12*UNIT, 24*UNIT, 4*UNIT), colour, sideTexture);
                                // Top
                                topMesh.addQuad(new Vector3f(4*UNIT, 32*UNIT, 4*UNIT), new Vector3f(12*UNIT, 32*UNIT, 4*UNIT), new Vector3f(12*UNIT, 32*UNIT, 12*UNIT), new Vector3f(4*UNIT, 32*UNIT, 12*UNIT), colour, topTexture);
                                // Left edge
                                sideMesh.addQuad(new Vector3f(4*UNIT, 32*UNIT, 4*UNIT), new Vector3f(4*UNIT, 32*UNIT, 12*UNIT), new Vector3f(4*UNIT, 24*UNIT, 12*UNIT), new Vector3f(4*UNIT, 24*UNIT, 4*UNIT), colour, sideTexture);
                                // Right edge
                                sideMesh.addQuad(new Vector3f(12*UNIT, 32*UNIT, 12*UNIT), new Vector3f(12*UNIT, 32*UNIT, 4*UNIT), new Vector3f(12*UNIT, 24*UNIT, 4*UNIT), new Vector3f(12*UNIT, 24*UNIT, 12*UNIT), colour, sideTexture);

                                frontMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.AntiClockwise, armorStand.getYaw());
                                sideMesh.pushTo(geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.AntiClockwise, armorStand.getYaw());
                                topMesh.pushTo(geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.AntiClockwise, armorStand.getYaw());
                        }
                }
        }
        
        private void buildFeetArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float angle, SubTexture texture, int offsetMultiplier) {
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
    
                SubMesh mesh = new SubMesh();

                
                // Left foot (this one is "turned inside out" to produce mirror image of right foot)

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(12.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(7.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(7.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*4, texture.v0+heightTexel*20, texture.u0+widthTexel*8, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(7.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(7.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*20, texture.u0+widthTexel*16, texture.v0+heightTexel*32));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(7.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(7.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(7.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(7.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*20, texture.u0+widthTexel*12, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*20, texture.u0+widthTexel*4, texture.v0+heightTexel*32));


                // Right foot

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*4, texture.v0+heightTexel*20, texture.u0+widthTexel*8, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*20, texture.u0+widthTexel*16, texture.v0+heightTexel*32));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*20, texture.u0+widthTexel*4, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 13.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(8.5f*UNIT+offsetMultiplier*EPSILON, 0.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*20, texture.u0+widthTexel*12, texture.v0+heightTexel*32));
                
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.AntiClockwise, angle);
        }
        
        private void buildLegsArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float angle, SubTexture texture, int offsetMultiplier) {
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
                
                offsetMultiplier += 1; // To be slightly bigger than the stand and not overlap
    
                SubMesh mesh = new SubMesh();

                
                // Front
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*20, texture.v0+heightTexel*27, texture.u0+widthTexel*28, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*32, texture.v0+heightTexel*27, texture.u0+widthTexel*40, texture.v0+heightTexel*32));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*27, texture.u0+widthTexel*20, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 17*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 12*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*28, texture.v0+heightTexel*27, texture.u0+widthTexel*32, texture.v0+heightTexel*32));

                
                // Left leg (this one is "turned inside out" to produce mirror image of right leg)

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(12*UNIT-offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT-offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*4, texture.v0+heightTexel*20, texture.u0+widthTexel*8, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT-offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT-offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*20, texture.u0+widthTexel*16, texture.v0+heightTexel*32));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(8*UNIT-offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 10*UNIT-offsetMultiplier*EPSILON), new Vector3f(8*UNIT-offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 6*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT-offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 6*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT-offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 10*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*20, texture.u0+widthTexel*12, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 6*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 13*UNIT-offsetMultiplier*EPSILON, 10*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 10*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 1*UNIT+offsetMultiplier*EPSILON, 6*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*20, texture.u0+widthTexel*4, texture.v0+heightTexel*32));


                // Right leg

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*4, texture.v0+heightTexel*20, texture.u0+widthTexel*8, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*20, texture.u0+widthTexel*16, texture.v0+heightTexel*32));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*20, texture.u0+widthTexel*4, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 13*UNIT+offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 6*UNIT-offsetMultiplier*EPSILON), new Vector3f(8*UNIT+offsetMultiplier*EPSILON, 1*UNIT-offsetMultiplier*EPSILON, 10*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*20, texture.u0+widthTexel*12, texture.v0+heightTexel*32));
                
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.AntiClockwise, angle);
        }
        
        private void buildChestArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float angle, SubTexture texture, int offsetMultiplier) {
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
    
                SubMesh mesh = new SubMesh();

                
                // Chest piece
                
                // Front
		mesh.addDoubleSidedQuad(new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*20, texture.v0+heightTexel*20, texture.u0+widthTexel*28, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*32, texture.v0+heightTexel*20, texture.u0+widthTexel*40, texture.v0+heightTexel*32));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(3.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*20, texture.u0+widthTexel*20, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(12.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*28, texture.v0+heightTexel*20, texture.u0+widthTexel*32, texture.v0+heightTexel*32));
                
                
                // Left pauldron (this one is "turned inside out" to produce mirror image of right pauldron)

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(15.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*44, texture.v0+heightTexel*20, texture.u0+widthTexel*48, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(10.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*52, texture.v0+heightTexel*20, texture.u0+widthTexel*56, texture.v0+heightTexel*32));
		// Top
		mesh.addDoubleSidedQuad(new Vector3f(15.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*44, texture.v0+heightTexel*16, texture.u0+widthTexel*48, texture.v0+heightTexel*20));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(10.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(10.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*48, texture.v0+heightTexel*20, texture.u0+widthTexel*52, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(15.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(15.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*40, texture.v0+heightTexel*20, texture.u0+widthTexel*44, texture.v0+heightTexel*32));


                // Right pauldron

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*44, texture.v0+heightTexel*20, texture.u0+widthTexel*48, texture.v0+heightTexel*32));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*52, texture.v0+heightTexel*20, texture.u0+widthTexel*56, texture.v0+heightTexel*32));
		// Top
		mesh.addDoubleSidedQuad(new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*44, texture.v0+heightTexel*16, texture.u0+widthTexel*48, texture.v0+heightTexel*20));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(0.5f*UNIT-offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*40, texture.v0+heightTexel*20, texture.u0+widthTexel*44, texture.v0+heightTexel*32));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 24.5f*UNIT+offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 5.5f*UNIT-offsetMultiplier*EPSILON), new Vector3f(5.5f*UNIT+offsetMultiplier*EPSILON, 11.5f*UNIT-offsetMultiplier*EPSILON, 10.5f*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*48, texture.v0+heightTexel*20, texture.u0+widthTexel*52, texture.v0+heightTexel*32));
                
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.AntiClockwise, angle);
        }
        
        private void buildHeadArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float angle, SubTexture texture, int offsetMultiplier) {
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
    
                SubMesh mesh = new SubMesh();

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*8, texture.u0+widthTexel*16, texture.v0+heightTexel*16));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*24, texture.v0+heightTexel*8, texture.u0+widthTexel*32, texture.v0+heightTexel*16));
		// Top
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*0, texture.u0+widthTexel*16, texture.v0+heightTexel*8));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(4*UNIT-offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*8, texture.u0+widthTexel*8, texture.v0+heightTexel*16));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 32*UNIT+offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 4*UNIT-offsetMultiplier*EPSILON), new Vector3f(12*UNIT+offsetMultiplier*EPSILON, 24*UNIT-offsetMultiplier*EPSILON, 12*UNIT+offsetMultiplier*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*8, texture.u0+widthTexel*24, texture.v0+heightTexel*16));
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.AntiClockwise, angle);
        }
}