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
import tectonicus.raw.ArmorStandEntity;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;

public class ArmorStand implements BlockType
{
	private final String name;
        
        private final SubTexture texture;
	
        private final SubTexture baseTopTexture;
        private final SubTexture baseSideTexture;

        public ArmorStand(String name, TexturePack texturePack)
	{
		this.name = name;
                
                texture = texturePack.findTexture("assets/minecraft/textures/entity/armorstand/wood.png");
                
                final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 64.0f;
                
		baseTopTexture = new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*32, texture.u0+widthTexel*24, texture.v0+heightTexel*44);
		baseSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*44, texture.u0+widthTexel*24, texture.v0+heightTexel*45);
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

                        buildBaseMesh(x, y, z, geometry, colour, unit);
                }
	}
        
        private void buildBaseMesh(int x, int y, int z, Geometry geometry, Vector4f colour, float unit) {
                SubMesh baseMesh = new SubMesh();
		
		// Front
		baseMesh.addDoubleSidedQuad(new Vector3f(2*unit, 1*unit, 14*unit), new Vector3f(14*unit, 1*unit, 14*unit), new Vector3f(14*unit, 0*unit, 14*unit), new Vector3f(2*unit, 0*unit, 14*unit), colour, baseSideTexture);
		// Back
		baseMesh.addDoubleSidedQuad(new Vector3f(14*unit, 1*unit, 2*unit), new Vector3f(2*unit, 1*unit, 2*unit), new Vector3f(2*unit, 0*unit, 2*unit), new Vector3f(14*unit, 0*unit, 2*unit), colour, baseSideTexture);
		// Top
		baseMesh.addDoubleSidedQuad(new Vector3f(2*unit, 1*unit, 2*unit), new Vector3f(14*unit, 1*unit, 2*unit), new Vector3f(14*unit, 1*unit, 14*unit), new Vector3f(2*unit, 1*unit, 14*unit), colour, baseTopTexture);
		// Left edge
		baseMesh.addDoubleSidedQuad(new Vector3f(2*unit, 1*unit, 2*unit), new Vector3f(2*unit, 1*unit, 14*unit), new Vector3f(2*unit, 0*unit, 14*unit), new Vector3f(2*unit, 0*unit, 2*unit), colour, baseSideTexture);
		// Right edge
		baseMesh.addDoubleSidedQuad(new Vector3f(14*unit, 1*unit, 14*unit), new Vector3f(14*unit, 1*unit, 2*unit), new Vector3f(14*unit, 0*unit, 2*unit), new Vector3f(14*unit, 0*unit, 14*unit), colour, baseSideTexture);
		
                baseMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
        }
}