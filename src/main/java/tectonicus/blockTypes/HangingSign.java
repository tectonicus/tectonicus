/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
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
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.SubMesh;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class HangingSign extends Sign
{
	private static final int WIDTH = 14;
	private static final int HEIGHT = 10;
	private static final int THICKNESS = 2;
		
        private final SubTexture frontBaseTexture;
	private final SubTexture backBaseTexture;
	private final SubTexture sideBaseTexture;
	private final SubTexture edgeBaseTexture;

        private final SubTexture attachedChainTexture;
        private final SubTexture chainTexture1;
        private final SubTexture chainTexture2;
        
	public HangingSign(String name, SubTexture texture, final boolean isWall, final boolean obey)
	{
		super(name, texture.texturePackVersion, isWall, obey);
                
                signBottom = 0;
		signDepth = 1.0f / 16.0f * 7;
		width = 1.0f / 16.0f * WIDTH;
		height = 1.0f / 16.0f * HEIGHT;
		thickness = 1.0f / 16.0f * THICKNESS;
                lineHeight = 1.0f / 16.0f * 2.25f;
		
		final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
		
		this.frontTexture = new SubTexture(texture.texture, texture.u0+widthTexel*2, texture.v0+heightTexel*14, texture.u0+widthTexel*16, texture.v0+heightTexel*24);
		this.backTexture = new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*14, texture.u0+widthTexel*28, texture.v0+heightTexel*24);
		this.sideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*2, texture.v0+heightTexel*12, texture.u0+widthTexel*16, texture.v0+heightTexel*13);
		this.edgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0+heightTexel*14, texture.u0+widthTexel*1, texture.v0+heightTexel*24);

                this.frontBaseTexture = new SubTexture(texture.texture, texture.u0+widthTexel*4, texture.v0+heightTexel*4, texture.u0+widthTexel*20, texture.v0+heightTexel*6);
		this.backBaseTexture = new SubTexture(texture.texture, texture.u0+widthTexel*24, texture.v0+heightTexel*4, texture.u0+widthTexel*40, texture.v0+heightTexel*6);
		this.sideBaseTexture = new SubTexture(texture.texture, texture.u0+widthTexel*4, texture.v0, texture.u0+widthTexel*20, texture.v0+heightTexel*4);
		this.edgeBaseTexture = new SubTexture(texture.texture, texture.u0, texture.v0+heightTexel*4, texture.u0+widthTexel*4, texture.v0+heightTexel*6);
                
                this.attachedChainTexture = new SubTexture(texture.texture, texture.u0+widthTexel*15, texture.v0+heightTexel*6, texture.u0+widthTexel*27, texture.v0+heightTexel*12);
                this.chainTexture1 = new SubTexture(texture.texture, texture.u0, texture.v0+heightTexel*6, texture.u0+widthTexel*3, texture.v0+heightTexel*12);
                this.chainTexture2 = new SubTexture(texture.texture, texture.u0+widthTexel*6, texture.v0+heightTexel*6, texture.u0+widthTexel*9, texture.v0+heightTexel*12);
        }
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
                final BlockProperties properties = rawChunk.getBlockState(x, y, z);
                int data = getRotationData(x, y, z, rawChunk, properties);
                Vector4f white = getWhite(x, y, z, world, rawChunk);
                
                float unit = 1.0f / 16.0f;

                SubMesh subMesh = new SubMesh();
                
		// Front
		subMesh.addQuad(new Vector3f(unit, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom, signDepth+thickness), new Vector3f(unit, signBottom, signDepth+thickness), white, frontTexture);
		// Back
		subMesh.addQuad(new Vector3f(width, signBottom+height, signDepth), new Vector3f(unit, signBottom+height, signDepth), new Vector3f(unit, signBottom, signDepth), new Vector3f(width, signBottom, signDepth), white, backTexture);
		// Top
		subMesh.addQuad(new Vector3f(unit, signBottom+height, signDepth), new Vector3f(width, signBottom+height, signDepth), new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(unit, signBottom+height, signDepth+thickness), white, sideTexture);
		// Left edge
		subMesh.addQuad(new Vector3f(unit, signBottom+height, signDepth), new Vector3f(unit, signBottom+height, signDepth+thickness), new Vector3f(unit, signBottom, signDepth+thickness), new Vector3f(unit, signBottom, signDepth), white, edgeTexture);
		// Right edge
		subMesh.addQuad(new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom+height, signDepth), new Vector3f(width, signBottom, signDepth), new Vector3f(width, signBottom, signDepth+thickness), white, edgeTexture);
		
                final float xOffset = x;
		final float yOffset = y;
		final float zOffset = z;
		
		float angle;
                        
                if (isWall) {
                        // Create the sign base
                        
                        // Front
                        subMesh.addQuad(new Vector3f(0 * unit, 16 * unit, signDepth+thickness+unit), new Vector3f(16 * unit, 16 * unit, signDepth+thickness+unit), new Vector3f(16 * unit, 14 * unit, signDepth+thickness+unit), new Vector3f(0 * unit, 14 * unit, signDepth+thickness+unit), white, frontBaseTexture);
                        // Back
                        subMesh.addQuad(new Vector3f(0 * unit, 16 * unit, signDepth-unit), new Vector3f(16 * unit, 16 * unit, signDepth-unit), new Vector3f(16 * unit, 14 * unit, signDepth-unit), new Vector3f(0 * unit, 14 * unit, signDepth-unit), white, backBaseTexture);
                        // Top
                        subMesh.addQuad(new Vector3f(0 * unit, 16 * unit, signDepth-unit), new Vector3f(16 * unit, 16 * unit, signDepth-unit), new Vector3f(16 * unit, 16 * unit, signDepth+thickness+unit), new Vector3f(0 * unit, 16 * unit, signDepth+thickness+unit), white, sideBaseTexture);
                        // Left edge
                        subMesh.addQuad(new Vector3f(0 * unit, 16 * unit, signDepth-unit), new Vector3f(0 * unit, 16 * unit, signDepth+thickness+unit), new Vector3f(0 * unit, 14 * unit, signDepth+thickness+unit), new Vector3f(0 * unit, 14 * unit, signDepth-unit), white, edgeBaseTexture);
                        // Right edge
                        subMesh.addQuad(new Vector3f(16 * unit, 16 * unit, signDepth+thickness+unit), new Vector3f(16 * unit, 16 * unit, signDepth-unit), new Vector3f(16 * unit, 14 * unit, signDepth-unit), new Vector3f(16 * unit, 14 * unit, signDepth+thickness+unit), white, edgeBaseTexture);
                        
                        angle = getRotationAngleForWallSign(data);
                } else {
                        angle = getRotationAngle(data);
                }
                
                // Draw attached chains
                if (properties.containsKey("attached") && properties.get("attached").equals("true")) {
                        subMesh.addDoubleSidedQuad(new Vector3f(2 * unit, 16 * unit, signDepth+thickness/2f), new Vector3f(14 * unit, 16 * unit, signDepth+thickness/2f), new Vector3f(14 * unit, 10 * unit, signDepth+thickness/2f), new Vector3f(2 * unit, 10 * unit, signDepth+thickness/2f), white, attachedChainTexture);
                } else {
                        subMesh.addDoubleSidedQuad(new Vector3f(1.8f * unit, 16 * unit, signDepth+thickness), new Vector3f(4.2f * unit, 16 * unit, signDepth), new Vector3f(4.2f * unit, 10 * unit, signDepth), new Vector3f(1.8f * unit, 10 * unit, signDepth+thickness), white, chainTexture1);
                        subMesh.addDoubleSidedQuad(new Vector3f(1.8f * unit, 16 * unit, signDepth), new Vector3f(4.2f * unit, 16 * unit, signDepth+thickness), new Vector3f(4.2f * unit, 10 * unit, signDepth+thickness), new Vector3f(1.8f * unit, 10 * unit, signDepth), white, chainTexture2);

                        subMesh.addDoubleSidedQuad(new Vector3f(11.8f * unit, 16 * unit, signDepth+thickness), new Vector3f(14.2f * unit, 16 * unit, signDepth), new Vector3f(14.2f * unit, 10 * unit, signDepth), new Vector3f(11.8f * unit, 10 * unit, signDepth+thickness), white, chainTexture1);
                        subMesh.addDoubleSidedQuad(new Vector3f(11.8f * unit, 16 * unit, signDepth), new Vector3f(14.2f * unit, 16 * unit, signDepth+thickness), new Vector3f(14.2f * unit, 10 * unit, signDepth+thickness), new Vector3f(11.8f * unit, 10 * unit, signDepth), white, chainTexture2);
                }
                
                addText(x, y, z, world, rawChunk, geometry, xOffset, yOffset, zOffset, angle);
                
                subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.AlphaTest), xOffset, yOffset, zOffset, ROTATION, angle);
	}
	
}