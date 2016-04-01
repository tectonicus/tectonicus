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
import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.TextLayout;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.raw.RawSign;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Sign implements BlockType
{
	private static final int WIDTH = 16;
	private static final int HEIGHT = 12;
	private static final int THICKNESS = 2;
	private static final int POST_HEIGHT = 8;
	
	private final String name;
	
	private SubTexture frontTexture, backTexture;
	private SubTexture sideTexture;
	private SubTexture edgeTexture;
	private SubTexture postTexture;
	
	private final boolean hasPost;
	private final boolean obey;
	
	private String texturePackVersion;
	
	public Sign(String name, SubTexture texture, final boolean hasPost, boolean obey)
	{
		this.name = name;
		this.hasPost = hasPost;
		this.obey = obey;
		this.texturePackVersion = texture.texturePackVersion;
		
		final float widthTexel = 1.0f / texture.texture.getWidth();
		final float heightTexel = 1.0f / texture.texture.getHeight();
		
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
		final int data = rawChunk.getBlockData(x, y, z);
		
		SubMesh subMesh = new SubMesh();
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float signBottom = hasPost ? 1.0f / 16.0f * POST_HEIGHT : 0;
		final float signDepth = hasPost ? 1.0f / 16.0f * 7 : 0;
		final float width = 1.0f / 16.0f * WIDTH;
		final float height = 1.0f / 16.0f * HEIGHT;
		final float thickness = 1.0f / 16.0f * THICKNESS;
		
		final float postHeight = 1.0f / 16.0f * POST_HEIGHT;
		final float postLeft = 1.0f / 16.0f * 7;
		final float postRight = 1.0f / 16.0f * 9;
		
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
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		if (hasPost)
		{
			// Add a post
			
			// East face
			subMesh.addQuad(new Vector3f(postRight, postHeight, postLeft), new Vector3f(postLeft, postHeight, postLeft), new Vector3f(postLeft, 0, postLeft), new Vector3f(postRight, 0, postLeft), white, postTexture);
			
			// West face
			subMesh.addQuad(new Vector3f(postLeft, postHeight, postRight), new Vector3f(postRight, postHeight, postRight), new Vector3f(postRight, 0, postRight), new Vector3f(postLeft, 0, postRight), white, postTexture);
			
			// North face
			subMesh.addQuad(new Vector3f(postLeft, postHeight, postLeft), new Vector3f(postLeft, postHeight, postRight), new Vector3f(postLeft, 0, postRight), new Vector3f(postLeft, 0, postLeft), white, postTexture);
			
			// South face
			subMesh.addQuad(new Vector3f(postRight, postHeight, postRight), new Vector3f(postRight, postHeight, postLeft), new Vector3f(postRight, 0, postLeft), new Vector3f(postRight, 0, postRight), white, postTexture);
			
			rotation = Rotation.AntiClockwise;
			angle = 90 / 4.0f * data;
		}
		else
		{
			if (data == 2)
			{
				// Facing east
				rotation = Rotation.Clockwise;
				angle = 180;
			}
			else if (data == 3)
			{
				// Facing west
				// ...built this way
				
			}
			else if (data == 4)
			{
				// Facing north
				rotation = Rotation.AntiClockwise;
				angle = 90;
				
			}
			else if (data == 5)
			{
				rotation = Rotation.Clockwise;
				angle = 90;
			}
		}
		
		// Add the text
		if (!obey)
		{
			for (RawSign s : rawChunk.getSigns())
			{
				if (s.localX == x && s.localY == y && s.localZ == z)
				{
					Mesh textMesh = geometry.getMesh(world.getTexturePack().getFont().getTexture(), Geometry.MeshType.AlphaTest);
					
					final float epsilon = 0.001f;
					final float lineHeight = 1.0f / 16.0f * 2.6f;
					
					final Vector4f color;
					if (texturePackVersion == "1.RV")
						color = new Vector4f(50f/255f, 183f/255f, 50f/255f, 1);
					else
						color = new Vector4f(0, 0, 0, 1);
					
					TextLayout text1 = new TextLayout(world.getTexturePack().getFont());
					text1.setText(unescapeJava(s.text1), width/2f, signBottom+height - lineHeight * 1, signDepth+thickness+epsilon, true, color);
					
					TextLayout text2 = new TextLayout(world.getTexturePack().getFont());
					text2.setText(unescapeJava(s.text2), width/2f, signBottom+height - lineHeight * 2, signDepth+thickness+epsilon, true, color);
					
					TextLayout text3 = new TextLayout(world.getTexturePack().getFont());
					text3.setText(unescapeJava(s.text3), width/2f, signBottom+height - lineHeight * 3, signDepth+thickness+epsilon, true, color);
					
					TextLayout text4 = new TextLayout(world.getTexturePack().getFont());
					text4.setText(unescapeJava(s.text4), width/2f, signBottom+height - lineHeight * 4, signDepth+thickness+epsilon, true, color);
					
					text1.pushTo(textMesh, xOffset, yOffset, zOffset, rotation, angle);
					text2.pushTo(textMesh, xOffset, yOffset, zOffset, rotation, angle);
					text3.pushTo(textMesh, xOffset, yOffset, zOffset, rotation, angle);
					text4.pushTo(textMesh, xOffset, yOffset, zOffset, rotation, angle);
					
					break;
				}
			}
		}
		
		subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset, zOffset, rotation, angle);
	}
	
}
