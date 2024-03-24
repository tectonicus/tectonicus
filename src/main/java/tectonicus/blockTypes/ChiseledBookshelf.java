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
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class ChiseledBookshelf implements BlockType
{
	private final String name;
        
        private final SubTexture emptyTexture;
        private final SubTexture occupiedTexture;
        private final SubTexture sideTexture;
        private final SubTexture topTexture;
	
	public ChiseledBookshelf(String name, TexturePack texturePack)
	{
		this.name = name;
                
                emptyTexture = texturePack.findTexture("assets/minecraft/textures/block/chiseled_bookshelf_empty.png");
                occupiedTexture = texturePack.findTexture("assets/minecraft/textures/block/chiseled_bookshelf_occupied.png");
                sideTexture = texturePack.findTexture("assets/minecraft/textures/block/chiseled_bookshelf_side.png");
                topTexture = texturePack.findTexture("assets/minecraft/textures/block/chiseled_bookshelf_top.png");
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
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
                final Colour4f colour = new Colour4f(1, 1, 1, 1);
            
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		Mesh emptyMesh = geometry.getMesh(emptyTexture.texture, Geometry.MeshType.Solid);
                
                SubTexture northTexture = sideTexture;
                SubTexture southTexture = sideTexture;
                SubTexture westTexture = sideTexture;
                SubTexture eastTexture = sideTexture;
                
                Mesh northMesh = sideMesh;
                Mesh southMesh = sideMesh;
                Mesh westMesh = sideMesh;
                Mesh eastMesh = sideMesh;
                
                final BlockProperties properties = rawChunk.getBlockState(x, y, z);
                if (properties != null && properties.containsKey("facing")) {
			final String facing = properties.get("facing");
			switch (facing) {
				case "north":
                                        eastTexture = emptyTexture;
                                        eastMesh = emptyMesh;                                        
					break;
				case "south":
                                        westTexture = emptyTexture;
                                        westMesh = emptyMesh;                                        
					break;
				case "west":
                                        northTexture = emptyTexture;
                                        northMesh = emptyMesh;                                        
					break;
				case "east":
                                        southTexture = emptyTexture;
                                        southMesh = emptyMesh;                                        
					break;
			}
		}

                BlockUtil.addTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, rawChunk, northMesh, x, y, z, colour, northTexture, registry);
		BlockUtil.addSouth(world, rawChunk, southMesh, x, y, z, colour, southTexture, registry);
		BlockUtil.addEast(world, rawChunk, eastMesh, x, y, z, colour, westTexture, registry);
		BlockUtil.addWest(world, rawChunk, westMesh, x, y, z, colour, eastTexture, registry);
                
                if (isSlotOccupied(0, properties)) {
                        drawOccupiedSlot(x, y, z, geometry, properties, 1, 1);
                }
                if (isSlotOccupied(1, properties)) {
                        drawOccupiedSlot(x, y, z, geometry, properties, 6, 1);
                }
                if (isSlotOccupied(2, properties)) {
                        drawOccupiedSlot(x, y, z, geometry, properties, 11, 1);
                }
                if (isSlotOccupied(3, properties)) {
                        drawOccupiedSlot(x, y, z, geometry, properties, 1, 9);
                }
                if (isSlotOccupied(4, properties)) {
                        drawOccupiedSlot(x, y, z, geometry, properties, 6, 9);
                }
                if (isSlotOccupied(5, properties)) {
                        drawOccupiedSlot(x, y, z, geometry, properties, 11, 9);
                }
	}
        
        private static Boolean isSlotOccupied(int slotIndex, BlockProperties properties) {
                final String propertyName = String.format("slot_%d_occupied", slotIndex);
            
                if (properties != null && properties.containsKey(propertyName)) {
			final String slotOccupied = properties.get(propertyName);
                        return "true".equals(slotOccupied);
                }

                return false;
        }
        
        private void drawOccupiedSlot(int x, int y, int z, Geometry geometry, BlockProperties properties, int wOffset, int hOffset) {
                Vector4f white = new Vector4f(1, 1, 1, 1);
                float angle = getRotationDataFromFacing(properties);
                
                final float widthTexel = 1.0f / 16.0f;
		final float heightTexel = 1.0f / 16.0f;

                SubTexture texture = new SubTexture(occupiedTexture.texture, occupiedTexture.u0+widthTexel*wOffset, occupiedTexture.v0+heightTexel*hOffset, occupiedTexture.u0+widthTexel*(wOffset+4), occupiedTexture.v0+heightTexel*(hOffset+6));
                
                final float unit = 1.0f / 16.0f;
                final float epsilon = 0.005f / 16.0f; // So that book texture and front texture are not on the same plane
                
                SubMesh subMesh = new SubMesh();
		subMesh.addQuad(new Vector3f(wOffset*unit, (16-hOffset)*unit, 16*unit+epsilon),
                                new Vector3f((wOffset+4)*unit, (16-hOffset)*unit, 16*unit+epsilon),
                                new Vector3f((wOffset+4)*unit, (16-hOffset-6)*unit, 16*unit+epsilon),
                                new Vector3f(wOffset*unit, (16-hOffset-6)*unit, 16*unit+epsilon),
                                white, texture);
		subMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.AntiClockwise, angle);
        }
        
        private static float getRotationDataFromFacing(BlockProperties properties) {
                int data = 0;
		
                if (properties != null && properties.containsKey("facing")) {
			final String facing = properties.get("facing");
			switch (facing) {
				case "north":
					data = 180;
					break;
				case "south":
					data = 0;
					break;
				case "west":
					data = 90;
					break;
				case "east":
					data = 270;
					break;
				default:
			}
		}
                
                return data;
        }
}