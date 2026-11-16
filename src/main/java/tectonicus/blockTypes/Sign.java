/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.SignTextRenderer;
import tectonicus.texture.SubTexture;

public class Sign implements BlockType
{
        protected static final Rotation ROTATION = Rotation.AntiClockwise;

        private static final int WIDTH = 16;
	private static final int HEIGHT = 12;
	private static final int THICKNESS = 2;
	private static final int POST_HEIGHT = 8;
        
        protected float signBottom;
	protected float signDepth;
	protected float width;
	protected float height;
	protected float thickness;
	
	protected final String name;
        
	protected SubTexture frontTexture;
	protected SubTexture backTexture;
	protected SubTexture sideTexture;
	protected SubTexture edgeTexture;
        
	private SubTexture postTexture;
	
	protected final boolean isWall;
	protected final boolean obey;
	
        protected Sign(String name, final boolean isWall, final boolean obey) {
		this.name = name;
		this.isWall = isWall;
		this.obey = obey;
                
                signBottom = isWall ? 0 : 1.0f / 16.0f * POST_HEIGHT;
                signDepth = isWall ? 0 : 1.0f / 16.0f * 7;
                width = 1.0f / 16.0f * WIDTH;
                height = 1.0f / 16.0f * HEIGHT;
                thickness = 1.0f / 16.0f * THICKNESS;
        }
        
	public Sign(String name, SubTexture texture, final boolean isWall, final boolean obey)
	{
                this(name, isWall, obey);
            
		final float widthTexel = 1.0f / 64.0f;
		final float heightTexel = 1.0f / 32.0f;
		
		this.frontTexture = new SubTexture(texture.texture, texture.u0+widthTexel*2, texture.v0+heightTexel*2, texture.u0+widthTexel*26, texture.v0+heightTexel*14);
		this.backTexture = new SubTexture(texture.texture, texture.u0+widthTexel*28, texture.v0+heightTexel*2, texture.u0+widthTexel*52, texture.v0+heightTexel*14);
		this.sideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*2, texture.v0, texture.u0+widthTexel*26, texture.v0+heightTexel*2);
		this.edgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0+heightTexel*2, texture.u0+widthTexel*2, texture.v0+heightTexel*14);
		this.postTexture = new SubTexture(texture.texture, texture.u0, texture.v0+heightTexel*16, texture.u0+widthTexel*2, texture.v0+heightTexel*30);
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
                final BlockProperties properties = rawChunk.getBlockState(x, y, z);
                int data = getRotationData(x, y, z, rawChunk, properties);
                Vector4f white = getWhite(x, y, z, world, rawChunk);
                
		SubMesh subMesh = new SubMesh();
		
		// Front
		subMesh.addQuad(new Vector3f(0, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom, signDepth+thickness), new Vector3f(0, signBottom, signDepth+thickness), white, frontTexture);
		// Back
		subMesh.addQuad(new Vector3f(width, signBottom+height, signDepth), new Vector3f(0, signBottom+height, signDepth), new Vector3f(0, signBottom, signDepth), new Vector3f(width, signBottom, signDepth), white, backTexture);
		// Top
		subMesh.addQuad(new Vector3f(0, signBottom+height, signDepth), new Vector3f(width, signBottom+height, signDepth), new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(0, signBottom+height, signDepth+thickness), white, sideTexture);
		// Left edge
		subMesh.addQuad(new Vector3f(0, signBottom+height, signDepth), new Vector3f(0, signBottom+height, signDepth+thickness), new Vector3f(0, signBottom, signDepth+thickness), new Vector3f(0, signBottom, signDepth), white, edgeTexture);
		// Right edge
		subMesh.addQuad(new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom+height, signDepth), new Vector3f(width, signBottom, signDepth), new Vector3f(width, signBottom, signDepth+thickness), white, edgeTexture);
		
		final float xOffset = x;
		final float yOffset = y + (1.0f / 16.0f);
		final float zOffset = z;
		
		float angle;
		
                if (isWall)
                {
                        angle = getRotationAngleForWallSign(data);
                }
                else
                {
                        // Add a post
                        
                        final float postHeight = 1.0f / 16.0f * POST_HEIGHT;
                        final float postLeft = 1.0f / 16.0f * 7;
                        final float postRight = 1.0f / 16.0f * 9;
                        
                        // East face
                        subMesh.addQuad(new Vector3f(postRight, postHeight, postLeft), new Vector3f(postLeft, postHeight, postLeft), new Vector3f(postLeft, 0, postLeft), new Vector3f(postRight, 0, postLeft), white, postTexture);
                        // West face
                        subMesh.addQuad(new Vector3f(postLeft, postHeight, postRight), new Vector3f(postRight, postHeight, postRight), new Vector3f(postRight, 0, postRight), new Vector3f(postLeft, 0, postRight), white, postTexture);
                        // North face
                        subMesh.addQuad(new Vector3f(postLeft, postHeight, postLeft), new Vector3f(postLeft, postHeight, postRight), new Vector3f(postLeft, 0, postRight), new Vector3f(postLeft, 0, postLeft), white, postTexture);
                        // South face
                        subMesh.addQuad(new Vector3f(postRight, postHeight, postRight), new Vector3f(postRight, postHeight, postLeft), new Vector3f(postRight, 0, postLeft), new Vector3f(postRight, 0, postRight), white, postTexture);

                        angle = getRotationAngle(data);
                }
		
                addText(x, y, z, world, rawChunk, geometry, yOffset, angle);
		
		subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset, zOffset, ROTATION, angle);
	}
        
        protected static int getRotationData(int x, int y, int z, RawChunk rawChunk, BlockProperties properties) {
                int data = rawChunk.getBlockData(x, y, z);
		
                if (properties != null && properties.containsKey("facing")) {
			final String facing = properties.get("facing");
			switch (facing) {
				case "north":
					data = 2;
					break;
				case "south":
					data = 3;
					break;
				case "west":
					data = 4;
					break;
				case "east":
					data = 5;
					break;
				default:
			}
		}
		
                if (properties != null && properties.containsKey("rotation")) {
			data = Integer.parseInt(properties.get("rotation"));
		}
                
                return data;
        }
        
        protected static Vector4f getWhite(final int x, final int y, final int z, BlockContext world, RawChunk rawChunk) {
                final float lightness = world.getLight(rawChunk, x, y, z, LightFace.Top, world.getNightLightAdjustment());
		return new Vector4f(lightness, lightness, lightness, 1);
        }
        
        protected static float getRotationAngle(int data) {
                return 90 / 4.0f * data;
        }
        
        protected static float getRotationAngleForWallSign(int data) {
            switch (data) {
                case 2: // Facing east                    
                    return 180;
                case 4: // Facing north                    
                    return 90;
                case 5: // Facing south
                    return 270;
                case 3: // Facing west (built this way)
                default:
                    return 0;
            }                
        }
        
	protected void addText(int x, int y, int z, BlockContext world, RawChunk chunk, Geometry geometry, float yOffset, float angle) {
		if (obey) return;
		boolean hanging = this instanceof HangingSign;
		float centerX = (width + (hanging ? 1f / 16f : 0)) / 2;
		float centerY = yOffset - y + signBottom + height / 2;
		SignTextRenderer.addText(chunk.getSign(x, y, z), world, chunk, geometry, hanging, centerX, centerY,
				signDepth + thickness + 0.002f, signDepth - 0.002f, angle);
	}
        
}
