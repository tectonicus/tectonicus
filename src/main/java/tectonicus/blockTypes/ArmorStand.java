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
import tectonicus.raw.DisplayTag;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;

public class ArmorStand implements BlockType
{
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
                        final float unit = 1.0f / 16.0f;
                        final float angle = entity.getYaw();

                        if (!entity.getInvisible() && !entity.getNoBasePlate()) {
                                buildBaseMesh(x, y, z, geometry, colour, unit);
                        }
                        if (!entity.getInvisible()) {
                                buildStandMesh(x, y, z, geometry, colour, unit, angle);
                        }
                        
                        ArmorItem feetArmor = entity.getFeetArmor();
                        ArmorItem legsArmor = entity.getLegsArmor();
                        ArmorItem chestArmor = entity.getChestArmor();
                        ArmorItem headArmor = entity.getHeadArmor();
                        
                        if (feetArmor != null) {
                                buildFeetArmorMesh(x, y, z, geometry, colour, unit, angle, feetArmor);
                        }
                        if (legsArmor != null) {
                                buildLegsArmorMesh(x, y, z, geometry, colour, unit, angle, legsArmor);
                        }
                        if (chestArmor != null) {
                                buildChestArmorMesh(x, y, z, geometry, colour, unit, angle, chestArmor);
                        }
                        if (headArmor != null) {
                                buildHeadArmorMesh(x, y, z, geometry, colour, unit, angle, headArmor);
                        }
                }
	}
        
        private void buildBaseMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit) {
                SubMesh mesh = new SubMesh();
		
		// Front
		mesh.addQuad(new Vector3f(2*unit, 1*unit, 14*unit), new Vector3f(14*unit, 1*unit, 14*unit), new Vector3f(14*unit, 0*unit, 14*unit), new Vector3f(2*unit, 0*unit, 14*unit), colour, baseSideTexture);
		// Back
		mesh.addQuad(new Vector3f(14*unit, 1*unit, 2*unit), new Vector3f(2*unit, 1*unit, 2*unit), new Vector3f(2*unit, 0*unit, 2*unit), new Vector3f(14*unit, 0*unit, 2*unit), colour, baseSideTexture);
		// Top
		mesh.addQuad(new Vector3f(2*unit, 1*unit, 2*unit), new Vector3f(14*unit, 1*unit, 2*unit), new Vector3f(14*unit, 1*unit, 14*unit), new Vector3f(2*unit, 1*unit, 14*unit), colour, baseTopTexture);
		// Left edge
		mesh.addQuad(new Vector3f(2*unit, 1*unit, 2*unit), new Vector3f(2*unit, 1*unit, 14*unit), new Vector3f(2*unit, 0*unit, 14*unit), new Vector3f(2*unit, 0*unit, 2*unit), colour, baseSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(14*unit, 1*unit, 14*unit), new Vector3f(14*unit, 1*unit, 2*unit), new Vector3f(14*unit, 0*unit, 2*unit), new Vector3f(14*unit, 0*unit, 14*unit), colour, baseSideTexture);
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
        }

        private void buildStandMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit, float angle) {
                SubMesh mesh = new SubMesh();


                // Left leg
                
		// Front
		mesh.addQuad(new Vector3f(5*unit, 12*unit, 9*unit), new Vector3f(7*unit, 12*unit, 9*unit), new Vector3f(7*unit, 1*unit, 9*unit), new Vector3f(5*unit, 1*unit, 9*unit), colour, legSideTexture);
		// Back
		mesh.addQuad(new Vector3f(7*unit, 12*unit, 7*unit), new Vector3f(5*unit, 12*unit, 7*unit), new Vector3f(5*unit, 1*unit, 7*unit), new Vector3f(7*unit, 1*unit, 7*unit), colour, legSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(5*unit, 12*unit, 7*unit), new Vector3f(5*unit, 12*unit, 9*unit), new Vector3f(5*unit, 1*unit, 9*unit), new Vector3f(5*unit, 1*unit, 7*unit), colour, legSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(7*unit, 12*unit, 9*unit), new Vector3f(7*unit, 12*unit, 7*unit), new Vector3f(7*unit, 1*unit, 7*unit), new Vector3f(7*unit, 1*unit, 9*unit), colour, legSideTexture);


                // Right leg
                
		// Front
		mesh.addQuad(new Vector3f(9*unit, 12*unit, 9*unit), new Vector3f(11*unit, 12*unit, 9*unit), new Vector3f(11*unit, 1*unit, 9*unit), new Vector3f(9*unit, 1*unit, 9*unit), colour, legSideTexture);
		// Back
		mesh.addQuad(new Vector3f(11*unit, 12*unit, 7*unit), new Vector3f(9*unit, 12*unit, 7*unit), new Vector3f(9*unit, 1*unit, 7*unit), new Vector3f(11*unit, 1*unit, 7*unit), colour, legSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(9*unit, 12*unit, 7*unit), new Vector3f(9*unit, 12*unit, 9*unit), new Vector3f(9*unit, 1*unit, 9*unit), new Vector3f(9*unit, 1*unit, 7*unit), colour, legSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(11*unit, 12*unit, 9*unit), new Vector3f(11*unit, 12*unit, 7*unit), new Vector3f(11*unit, 1*unit, 7*unit), new Vector3f(11*unit, 1*unit, 9*unit), colour, legSideTexture);

                
                // Hips
                
		// Front
		mesh.addQuad(new Vector3f(4*unit, 14*unit, 9*unit), new Vector3f(12*unit, 14*unit, 9*unit), new Vector3f(12*unit, 12*unit, 9*unit), new Vector3f(4*unit, 12*unit, 9*unit), colour, hipsFrontTexture);
		// Back
		mesh.addQuad(new Vector3f(12*unit, 14*unit, 7*unit), new Vector3f(4*unit, 14*unit, 7*unit), new Vector3f(4*unit, 12*unit, 7*unit), new Vector3f(12*unit, 12*unit, 7*unit), colour, hipsFrontTexture);
		// Top
		mesh.addQuad(new Vector3f(4*unit, 14*unit, 7*unit), new Vector3f(12*unit, 14*unit, 7*unit), new Vector3f(12*unit, 14*unit, 9*unit), new Vector3f(4*unit, 14*unit, 9*unit), colour, hipsFrontTexture);
		// Left edge
		mesh.addQuad(new Vector3f(4*unit, 14*unit, 7*unit), new Vector3f(4*unit, 14*unit, 9*unit), new Vector3f(4*unit, 12*unit, 9*unit), new Vector3f(4*unit, 12*unit, 7*unit), colour, hipsSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(12*unit, 14*unit, 9*unit), new Vector3f(12*unit, 14*unit, 7*unit), new Vector3f(12*unit, 12*unit, 7*unit), new Vector3f(12*unit, 12*unit, 9*unit), colour, hipsSideTexture);


                // Left part of torso
                
		// Front
		mesh.addQuad(new Vector3f(5*unit, 21*unit, 9*unit), new Vector3f(7*unit, 21*unit, 9*unit), new Vector3f(7*unit, 14*unit, 9*unit), new Vector3f(5*unit, 14*unit, 9*unit), colour, torsoSideTexture);
		// Back
		mesh.addQuad(new Vector3f(7*unit, 21*unit, 7*unit), new Vector3f(5*unit, 21*unit, 7*unit), new Vector3f(5*unit, 14*unit, 7*unit), new Vector3f(7*unit, 14*unit, 7*unit), colour, torsoSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(5*unit, 21*unit, 7*unit), new Vector3f(5*unit, 21*unit, 9*unit), new Vector3f(5*unit, 14*unit, 9*unit), new Vector3f(5*unit, 14*unit, 7*unit), colour, torsoSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(7*unit, 21*unit, 9*unit), new Vector3f(7*unit, 21*unit, 7*unit), new Vector3f(7*unit, 14*unit, 7*unit), new Vector3f(7*unit, 14*unit, 9*unit), colour, torsoSideTexture);


                // Right part of torso
                
		// Front
		mesh.addQuad(new Vector3f(9*unit, 21*unit, 9*unit), new Vector3f(11*unit, 21*unit, 9*unit), new Vector3f(11*unit, 14*unit, 9*unit), new Vector3f(9*unit, 14*unit, 9*unit), colour, torsoSideTexture);
		// Back
		mesh.addQuad(new Vector3f(11*unit, 21*unit, 7*unit), new Vector3f(9*unit, 21*unit, 7*unit), new Vector3f(9*unit, 14*unit, 7*unit), new Vector3f(11*unit, 14*unit, 7*unit), colour, torsoSideTexture);
		// Left edge
		mesh.addQuad(new Vector3f(9*unit, 21*unit, 7*unit), new Vector3f(9*unit, 21*unit, 9*unit), new Vector3f(9*unit, 14*unit, 9*unit), new Vector3f(9*unit, 14*unit, 7*unit), colour, torsoSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(11*unit, 21*unit, 9*unit), new Vector3f(11*unit, 21*unit, 7*unit), new Vector3f(11*unit, 14*unit, 7*unit), new Vector3f(11*unit, 14*unit, 9*unit), colour, torsoSideTexture);

                
                // Shoulders
                
		// Front
		mesh.addQuad(new Vector3f(2*unit, 24*unit, 9.5f*unit), new Vector3f(14*unit, 24*unit, 9.5f*unit), new Vector3f(14*unit, 21*unit, 9.5f*unit), new Vector3f(2*unit, 21*unit, 9.5f*unit), colour, shouldersFrontTexture);
		// Back
		mesh.addQuad(new Vector3f(14*unit, 24*unit, 6.5f*unit), new Vector3f(2*unit, 24*unit, 6.5f*unit), new Vector3f(2*unit, 21*unit, 6.5f*unit), new Vector3f(14*unit, 21*unit, 6.5f*unit), colour, shouldersFrontTexture);
		// Top
		mesh.addQuad(new Vector3f(2*unit, 24*unit, 6.5f*unit), new Vector3f(14*unit, 24*unit, 6.5f*unit), new Vector3f(14*unit, 24*unit, 9.5f*unit), new Vector3f(2*unit, 24*unit, 9.5f*unit), colour, shouldersFrontTexture);
		// Left edge
		mesh.addQuad(new Vector3f(2*unit, 24*unit, 6.5f*unit), new Vector3f(2*unit, 24*unit, 9.5f*unit), new Vector3f(2*unit, 21*unit, 9.5f*unit), new Vector3f(2*unit, 21*unit, 6.5f*unit), colour, shouldersSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(14*unit, 24*unit, 9.5f*unit), new Vector3f(14*unit, 24*unit, 6.5f*unit), new Vector3f(14*unit, 21*unit, 6.5f*unit), new Vector3f(14*unit, 21*unit, 9.5f*unit), colour, shouldersSideTexture);


                // Neck/head
                
		// Front
		mesh.addQuad(new Vector3f(7*unit, 30*unit, 9*unit), new Vector3f(9*unit, 30*unit, 9*unit), new Vector3f(9*unit, 24*unit, 9*unit), new Vector3f(7*unit, 24*unit, 9*unit), colour, neckSideTexture);
		// Back
		mesh.addQuad(new Vector3f(9*unit, 30*unit, 7*unit), new Vector3f(7*unit, 30*unit, 7*unit), new Vector3f(7*unit, 24*unit, 7*unit), new Vector3f(9*unit, 24*unit, 7*unit), colour, neckSideTexture);
		// Top
		mesh.addQuad(new Vector3f(7*unit, 30*unit, 7*unit), new Vector3f(9*unit, 30*unit, 7*unit), new Vector3f(9*unit, 30*unit, 9*unit), new Vector3f(7*unit, 30*unit, 9*unit), colour, neckTopTexture);
		// Left edge
		mesh.addQuad(new Vector3f(7*unit, 30*unit, 7*unit), new Vector3f(7*unit, 30*unit, 9*unit), new Vector3f(7*unit, 24*unit, 9*unit), new Vector3f(7*unit, 24*unit, 7*unit), colour, neckSideTexture);
		// Right edge
		mesh.addQuad(new Vector3f(9*unit, 30*unit, 9*unit), new Vector3f(9*unit, 30*unit, 7*unit), new Vector3f(9*unit, 24*unit, 7*unit), new Vector3f(9*unit, 24*unit, 9*unit), colour, neckSideTexture);

                
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.AntiClockwise, angle);
        }
        
        private void buildFeetArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit, float angle, ArmorItem armor) {
            
        }
        
        private void buildLegsArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit, float angle, ArmorItem armor) {
            
        }
        
        private void buildChestArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit, float angle, ArmorItem armor) {
            
        }
        
        private void buildHeadArmorMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit, float angle, ArmorItem armor) {
                String armorMaterial = armor.id.substring("minecraft:".length(), armor.id.indexOf('_'));
                if (armorMaterial.equals("golden")) {
                        armorMaterial = "gold";
                }
                
                SubTexture layer1Texture = texturePack.findTexture(String.format("assets/minecraft/textures/models/armor/%s_layer_1.png", armorMaterial));
                SubTexture layer2Texture = texturePack.findTextureOrDefault(String.format("assets/minecraft/textures/models/armor/%s_layer_2.png", armorMaterial), null);
                SubTexture layer1OverlayTexture = texturePack.findTextureOrDefault(String.format("assets/minecraft/textures/models/armor/%s_layer_1_overlay.png", armorMaterial), null);
                SubTexture layer2OverlayTexture = texturePack.findTextureOrDefault(String.format("assets/minecraft/textures/models/armor/%s_layer_2_overlay.png", armorMaterial), null);
                
                DisplayTag display = armor.getTag(DisplayTag.class);
            
                if (armorMaterial.equals("leather")) {
                        buildHeadArmorLayerMesh(x, y, z, geometry, colour, unit, angle, layer1OverlayTexture, 0);
                        buildHeadArmorLayerMesh(x, y, z, geometry, colour, unit, angle, layer2OverlayTexture, 2);
                        colour = display == null
                                ? new Vector4f(106/255f, 64/255f, 41/255f, 1) // Default brown leather
                                : new Vector4f(((display.color >> 16) & 255)/255f, ((display.color >> 8) & 255)/255f, (display.color & 255)/255f, 1);
                }
		
                buildHeadArmorLayerMesh(x, y, z, geometry, colour, unit, angle, layer1Texture, -1);
                if (layer2Texture != null) {
                        buildHeadArmorLayerMesh(x, y, z, geometry, colour, unit, angle, layer2Texture, 1);
                }
        }
        
        private void buildHeadArmorLayerMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit, float angle, SubTexture texture, int layerIndex) {
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
    
                SubMesh mesh = new SubMesh();

                // Front
		mesh.addDoubleSidedQuad(new Vector3f(4*unit-layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*8, texture.u0+widthTexel*16, texture.v0+heightTexel*16));
		// Back
		mesh.addDoubleSidedQuad(new Vector3f(12*unit+layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*24, texture.v0+heightTexel*8, texture.u0+widthTexel*32, texture.v0+heightTexel*16));
		// Top
		mesh.addDoubleSidedQuad(new Vector3f(4*unit-layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*0, texture.u0+widthTexel*16, texture.v0+heightTexel*8));
		// Left edge
		mesh.addDoubleSidedQuad(new Vector3f(4*unit-layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(4*unit-layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*0, texture.v0+heightTexel*8, texture.u0+widthTexel*8, texture.v0+heightTexel*16));
		// Right edge
		mesh.addDoubleSidedQuad(new Vector3f(12*unit+layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 32*unit+layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 4*unit-layerIndex*EPSILON), new Vector3f(12*unit+layerIndex*EPSILON, 24*unit-layerIndex*EPSILON, 12*unit+layerIndex*EPSILON), colour,
                        new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*8, texture.u0+widthTexel*24, texture.v0+heightTexel*16));
		
                mesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.AntiClockwise, angle);
        }
}