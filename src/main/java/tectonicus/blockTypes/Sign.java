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
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.TextLayout;
import tectonicus.Version;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;
import tectonicus.world.Colors;

import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static tectonicus.Version.VERSION_RV;

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
        protected float lineHeight;
	
	protected final String name;
        
	protected SubTexture frontTexture;
	protected SubTexture backTexture;
	protected SubTexture sideTexture;
	protected SubTexture edgeTexture;
        
	private SubTexture postTexture;
	
	protected final boolean isWall;
	protected final boolean obey;
	
	private Version texturePackVersion;
	
        protected Sign(String name, Version texturePackVersion, final boolean isWall, final boolean obey) {
		this.name = name;
		this.isWall = isWall;
		this.obey = obey;
		this.texturePackVersion = texturePackVersion;
                
                signBottom = isWall ? 0 : 1.0f / 16.0f * POST_HEIGHT;
                signDepth = isWall ? 0 : 1.0f / 16.0f * 7;
                width = 1.0f / 16.0f * WIDTH;
                height = 1.0f / 16.0f * HEIGHT;
                thickness = 1.0f / 16.0f * THICKNESS;
                lineHeight = 1.0f / 16.0f * 2.6f;
        }
        
	public Sign(String name, SubTexture texture, final boolean isWall, final boolean obey)
	{
                this(name, texture.texturePackVersion, isWall, obey);
            
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
		
                addText(x, y, z, world, rawChunk, geometry, xOffset, yOffset, zOffset, angle);
		
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
                final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, world.getNightLightAdjustment());
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
        
        protected void addText(int x, int y, int z, BlockContext world, RawChunk rawChunk, Geometry geometry, float xOffset, float yOffset, float zOffset, float angle) {
		// Add the text
		if (!obey)
		{
                        String xyz = "x" + x + "y" + y + "z" + z;
                        SignEntity sign = rawChunk.getSigns().get(xyz);

                        Mesh textMesh = geometry.getMesh(world.getTexturePack().getFont().getTexture(), Geometry.MeshType.AlphaTest);
			
			final float epsilon = 0.001f;
						
			final Vector4f color;
			if (texturePackVersion == VERSION_RV) {
				color = new Vector4f(50/255f, 183/255f, 50/255f, 1);
			}
			else {
				Colour4f signColor = Colors.byName(sign.getColor()).getColorNormalized();
				color = new Vector4f(signColor.getR(), signColor.getG(), signColor.getB(), 1);
			}
			
			TextLayout text1 = new TextLayout(world.getTexturePack().getFont());
			text1.setText(unescapeJava(sign.getText1()), width/2f, signBottom+height - lineHeight * 1, signDepth+thickness+epsilon, true, color);
			
			TextLayout text2 = new TextLayout(world.getTexturePack().getFont());
			text2.setText(unescapeJava(sign.getText2()), width/2f, signBottom+height - lineHeight * 2, signDepth+thickness+epsilon, true, color);
			
			TextLayout text3 = new TextLayout(world.getTexturePack().getFont());
			text3.setText(unescapeJava(sign.getText3()), width/2f, signBottom+height - lineHeight * 3, signDepth+thickness+epsilon, true, color);
			
			TextLayout text4 = new TextLayout(world.getTexturePack().getFont());
			text4.setText(unescapeJava(sign.getText4()), width/2f, signBottom+height - lineHeight * 4, signDepth+thickness+epsilon, true, color);
			
			text1.pushTo(textMesh, xOffset, yOffset, zOffset, ROTATION, angle);
			text2.pushTo(textMesh, xOffset, yOffset, zOffset, ROTATION, angle);
			text3.pushTo(textMesh, xOffset, yOffset, zOffset, ROTATION, angle);
			text4.pushTo(textMesh, xOffset, yOffset, zOffset, ROTATION, angle);
		}
        }
        
}
