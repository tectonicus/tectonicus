/*
 * Copyright (c) 2024, Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.HashMap;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.DecoratedPotEntity;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;

public class DecoratedPot implements BlockType
{
	private final String name;
	
	private final SubTexture baseTexture;
	private final HashMap<String, SubTexture> textures;
        
        private final SubTexture topTexture;
        private final SubTexture neckTopTexture;
        private final SubTexture neckSideTexture;

	public DecoratedPot(String name, TexturePack texturePack)
	{
		this.name = name;
                
                baseTexture = texturePack.findTexture("assets/minecraft/textures/entity/decorated_pot/decorated_pot_base.png");
                
                textures = new HashMap<>();
                textures.put("minecraft:brick", texturePack.findTexture("assets/minecraft/textures/entity/decorated_pot/decorated_pot_side.png"));
                for (var pattern : new String[] {
                    "angler",
                    "archer",
                    "arms_up",
                    "blade",
                    "brewer",
                    "burn",
                    "danger",
                    "explorer",
                    "friend",
                    "heart",
                    "heartbreak",
                    "howl",
                    "miner",
                    "mourner",
                    "plenty",
                    "prize",
                    "sheaf",
                    "shelter",
                    "skull",
                    "snort"
                }) {
                        textures.put("minecraft:"+pattern+"_pottery_sherd", texturePack.findTexture("assets/minecraft/textures/entity/decorated_pot/"+pattern+"_pottery_pattern.png"));
                }
                
                final float widthTexel = 1.0f / 32.0f;
                final float heightTexel = 1.0f / 32.0f;
                        
                topTexture = new SubTexture(baseTexture.texture, baseTexture.u0+widthTexel*0, baseTexture.v0+heightTexel*13, baseTexture.u0+widthTexel*14, baseTexture.v0+heightTexel*27);
                neckTopTexture = new SubTexture(baseTexture.texture, baseTexture.u0+widthTexel*8, baseTexture.v0+heightTexel*0, baseTexture.u0+widthTexel*16, baseTexture.v0+heightTexel*8);
                neckSideTexture = new SubTexture(baseTexture.texture, baseTexture.u0+widthTexel*0, baseTexture.v0+heightTexel*8, baseTexture.u0+widthTexel*8, baseTexture.v0+heightTexel*11);
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
            	final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, world.getNightLightAdjustment());
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
                
                final float offSet = 1.0f / 16.0f;
                
                String xyz = "x" + x + "y" + y + "z" + z;
                DecoratedPotEntity pot = rawChunk.getDecoratedPots().get(xyz);
                if (pot == null) {
                        // There is no entity when rendering item icons. Use default values...
                        pot = new DecoratedPotEntity(0, 0, 0, 0, 0, 0, "minecraft:brick", "minecraft:brick", "minecraft:brick", "minecraft:brick");
                }
                
                final SubTexture side1Texture = getSideTexture(pot.getSherd1());
                final SubTexture side2Texture = getSideTexture(pot.getSherd2());
                final SubTexture side3Texture = getSideTexture(pot.getSherd3());
                final SubTexture side4Texture = getSideTexture(pot.getSherd4());
        
		SubMesh mesh = new SubMesh();
		
                SubMesh side1Mesh = new SubMesh();
		SubMesh side2Mesh = new SubMesh();
		SubMesh side3Mesh = new SubMesh();
		SubMesh side4Mesh = new SubMesh();
                
                // Neck
                mesh.addQuad(       new Vector3f(offSet*4,  offSet*20,	offSet*4),
                                    new Vector3f(offSet*12, offSet*20,	offSet*4),
                                    new Vector3f(offSet*12, offSet*20,	offSet*12),
                                    new Vector3f(offSet*4,  offSet*20,	offSet*12),
                                    colour,
                                    neckTopTexture);
                mesh.addQuad(       new Vector3f(offSet*4,  offSet*20,  offSet*12),
                                    new Vector3f(offSet*12, offSet*20,  offSet*12),
                                    new Vector3f(offSet*12, offSet*17,  offSet*12),
                                    new Vector3f(offSet*4,  offSet*17,  offSet*12),
                                    colour,
                                    neckSideTexture);
                mesh.addQuad(       new Vector3f(offSet*12, offSet*20,	offSet*12),
                                    new Vector3f(offSet*12, offSet*20,	offSet*4),
                                    new Vector3f(offSet*12, offSet*17,	offSet*4),
                                    new Vector3f(offSet*12, offSet*17,	offSet*12),
                                    colour,
                                    neckSideTexture);
                mesh.addQuad(       new Vector3f(offSet*4,  offSet*20,	offSet*4),
                                    new Vector3f(offSet*4,  offSet*20,	offSet*12),
                                    new Vector3f(offSet*4,  offSet*17,	offSet*12),
                                    new Vector3f(offSet*4,  offSet*17,	offSet*4),
                                    colour,
                                    neckSideTexture);
                mesh.addQuad(       new Vector3f(offSet*12, offSet*20,  offSet*4),
                                    new Vector3f(offSet*4,  offSet*20,  offSet*4),
                                    new Vector3f(offSet*4,  offSet*17,  offSet*4),
                                    new Vector3f(offSet*12, offSet*17,  offSet*4),
                                    colour,
                                    neckSideTexture);
                
                // Top
                mesh.addQuad(       new Vector3f(offSet*1,  offSet*16,	offSet*1),
                                    new Vector3f(offSet*15, offSet*16,	offSet*1),
                                    new Vector3f(offSet*15, offSet*16,	offSet*15),
                                    new Vector3f(offSet*1,  offSet*16,	offSet*15),
                                    colour,
                                    topTexture);

                // Bottom
                mesh.addQuad(       new Vector3f(offSet*1,  offSet*0,	offSet*15),
                                    new Vector3f(offSet*15, offSet*0,	offSet*15),
                                    new Vector3f(offSet*15, offSet*0,	offSet*1),
                                    new Vector3f(offSet*1,  offSet*0,	offSet*1),
                                    colour,
                                    topTexture);
                
                // Back
                side1Mesh.addQuad(  new Vector3f(offSet*1,  offSet*16,  offSet*15),
                                    new Vector3f(offSet*15, offSet*16,  offSet*15),
                                    new Vector3f(offSet*15, offSet*0,   offSet*15),
                                    new Vector3f(offSet*1,  offSet*0,   offSet*15),
                                    colour,
                                    side1Texture);

                // Left
                side2Mesh.addQuad(  new Vector3f(offSet*15, offSet*16,	offSet*15),
                                    new Vector3f(offSet*15, offSet*16,	offSet*1),
                                    new Vector3f(offSet*15, offSet*0,	offSet*1),
                                    new Vector3f(offSet*15, offSet*0,	offSet*15),
                                    colour,
                                    side2Texture);

                // Right
                side3Mesh.addQuad(  new Vector3f(offSet*1,  offSet*16,	offSet*1),
                                    new Vector3f(offSet*1,  offSet*16,	offSet*15),
                                    new Vector3f(offSet*1,  offSet*0,	offSet*15),
                                    new Vector3f(offSet*1,  offSet*0,	offSet*1),
                                    colour,
                                    side3Texture);

                // Front
                side4Mesh.addQuad(  new Vector3f(offSet*15, offSet*16,  offSet*1),
                                    new Vector3f(offSet*1,  offSet*16,  offSet*1),
                                    new Vector3f(offSet*1,  offSet*0,   offSet*1),
                                    new Vector3f(offSet*15, offSet*0,   offSet*1),
                                    colour,
                                    side4Texture);
                
                final float angle = getRotationAngle(x, y, z, rawChunk);
                
                mesh.pushTo(geometry.getMesh(baseTexture.texture, Geometry.MeshType.Solid), x, y, z, SubMesh.Rotation.AntiClockwise, angle);
                
                side1Mesh.pushTo(geometry.getMesh(side1Texture.texture, Geometry.MeshType.Solid), x, y, z, SubMesh.Rotation.AntiClockwise, angle);
                side2Mesh.pushTo(geometry.getMesh(side2Texture.texture, Geometry.MeshType.Solid), x, y, z, SubMesh.Rotation.AntiClockwise, angle);
                side3Mesh.pushTo(geometry.getMesh(side3Texture.texture, Geometry.MeshType.Solid), x, y, z, SubMesh.Rotation.AntiClockwise, angle);
                side4Mesh.pushTo(geometry.getMesh(side4Texture.texture, Geometry.MeshType.Solid), x, y, z, SubMesh.Rotation.AntiClockwise, angle);
	}
        
        private SubTexture getSideTexture(String sherd) {
                SubTexture texture = textures.get(sherd);

                final float widthTexel = 1.0f / 16.0f;
                final float heightTexel = 1.0f / 16.0f;

                return new SubTexture(texture.texture, texture.u0+widthTexel*1, texture.v0+heightTexel*0, texture.u0+widthTexel*15, texture.v0+heightTexel*16);
        }
        
        private static int getRotationAngle(int x, int y, int z, RawChunk rawChunk) {
                final BlockProperties properties = rawChunk.getBlockState(x, y, z);
                
                if (properties == null) {
                        return 0;
                }

                switch (properties.get("facing")) {
                        case "north":
                                return 180;
                        case "west":
                                return 90;
                        case "east":
                                return 270;
                        case "south":
                        default:
                                return 0;
                }    
        }
}