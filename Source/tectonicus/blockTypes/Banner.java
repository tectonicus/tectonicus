/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.raw.TileEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Banner implements BlockType
{
	private static final int WIDTH = 15;
	private static final int HEIGHT = 2;
	private static final int THICKNESS = 2;
	private static final int POST_HEIGHT = 28;
	
	private final String name;
	
	private SubTexture frontTexture, bannerSideTexture;
	private SubTexture sideTexture, sideTexture2;
	private SubTexture edgeTexture;
	private SubTexture topTexture;
	
	private final boolean hasPost;
	
	public Banner(String name, SubTexture texture, final boolean hasPost)
	{
		this.name = name;
		this.hasPost = hasPost;
		
		final float texel = 1.0f / 64.0f;
		
		this.frontTexture = new SubTexture(texture.texture, texture.u0+texel, texture.v0+texel, texture.u0+texel*21, texture.v0+texel*41);
		this.bannerSideTexture = new SubTexture(texture.texture, texture.u0, texture.v0+texel, texture.u0+texel, texture.v0+texel*41);
		this.sideTexture = new SubTexture(texture.texture, texture.u0+texel*50, texture.v0+texel*2, texture.u0+texel*52, texture.v0+texel*43);
		this.sideTexture2 = new SubTexture(texture.texture, texture.u0+texel*48, texture.v0+texel*2, texture.u0+texel*50, texture.v0+texel*43);
		this.topTexture = new SubTexture(texture.texture, texture.u0+texel*2, texture.v0+texel*42, texture.u0+texel*22, texture.v0+texel*44);
		this.edgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0+texel*44, texture.u0+texel*2, texture.v0+texel*46);
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
		int baseColor = 0;
		for (TileEntity te : rawChunk.getBanners())
		{
			if (te.localX == x && te.localY == y && te.localZ == z)
			{
				baseColor = te.blockData;
				break;
			}
		}
		
		Colour4f color;
		if (baseColor == 0)
			color = new Colour4f(25f/255f, 25f/255f, 25f/255f, 1);  // Black
		else if (baseColor == 1)
			color = new Colour4f(153f/255f, 51f/255f, 51f/255f, 1); // Red
		else if (baseColor == 2)
			color = new Colour4f(102f/255f, 127f/255f, 51f/255f, 1); // Green
		else if (baseColor == 3)
			color = new Colour4f(102f/255f, 76f/255f, 51f/255f, 1); // Brown
		else if (baseColor == 4)
			color = new Colour4f(51f/255f, 76f/255f, 178f/255f, 1); // Blue
		else if (baseColor == 5)
			color = new Colour4f(127f/255f, 63f/255f, 178f/255f, 1); // Purple
		else if (baseColor == 6)
			color = new Colour4f(76f/255f, 127f/255f, 153f/255f, 1); // Cyan
		else if (baseColor == 7)
			color = new Colour4f(153f/255f, 153f/255f, 153f/255f, 1); // Light Gray
		else if (baseColor == 8)
			color = new Colour4f(76f/255f, 76f/255f, 76f/255f, 1); // Gray
		else if (baseColor == 9)
			color = new Colour4f(242f/255f, 127f/255f, 165f/255f, 1); // Pink
		else if (baseColor == 10)
			color = new Colour4f(127f/255f, 204f/255f, 25f/255f, 1); // Lime
		else if (baseColor == 11)
			color = new Colour4f(229f/255f, 229f/255f, 51f/255f, 1); // Yellow
		else if (baseColor == 12)
			color = new Colour4f(102f/255f, 153f/255f, 216f/255f, 1); // Light Blue
		else if (baseColor == 13)
			color = new Colour4f(178f/255f, 76f/255f, 216f/255f, 1); // Magenta
		else if (baseColor == 14)
			color = new Colour4f(216f/255f, 127f/255f, 51f/255f, 1); // Orange
		else
			color = new Colour4f(1, 1, 1, 1); // White
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		Vector4f bannerColor = new Vector4f(color.r*lightness, color.g*lightness, color.b*lightness, 1);
		
		final float texel = 1.0f / 16.0f;
		final float signBottom = 1.0f / 16.0f * POST_HEIGHT;

		//final float signBottom = hasPost ? 1.0f / 16.0f * POST_HEIGHT : 0;
		final float signDepth = hasPost ? 1.0f / 16.0f * 7 : 0;
		final float width = 1.0f / 16.0f * WIDTH;
		final float height = 1.0f / 16.0f * HEIGHT;
		final float thickness = 1.0f / 16.0f * THICKNESS;
		
		final float postHeight = 1.0f / 16.0f * POST_HEIGHT;
		final float postLeft = 1.0f / 16.0f * 7;
		final float postRight = 1.0f / 16.0f * 9;
		
		final float bannerHeight = postHeight + height;
		final float bannerDepth = signDepth + thickness + 0.01f;
		
		/* Top of post */
		// Front
		subMesh.addQuad(new Vector3f(texel, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom, signDepth+thickness), new Vector3f(texel, signBottom, signDepth+thickness), white, topTexture);
		
		// Back
		subMesh.addQuad(new Vector3f(width, signBottom+height, signDepth), new Vector3f(texel, signBottom+height, signDepth), new Vector3f(texel, signBottom, signDepth), new Vector3f(width, signBottom, signDepth), white, topTexture);
		
		// Top
		subMesh.addQuad(new Vector3f(texel, signBottom+height, signDepth), new Vector3f(width, signBottom+height, signDepth), new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(texel, signBottom+height, signDepth+thickness), white, topTexture);
		
		// Left edge
		subMesh.addQuad(new Vector3f(texel, signBottom+height, signDepth), new Vector3f(texel, signBottom+height, signDepth+thickness), new Vector3f(texel, signBottom, signDepth+thickness), new Vector3f(texel, signBottom, signDepth), white, edgeTexture);
		
		// Right edge
		subMesh.addQuad(new Vector3f(width, signBottom+height, signDepth+thickness), new Vector3f(width, signBottom+height, signDepth), new Vector3f(width, signBottom, signDepth), new Vector3f(width, signBottom, signDepth+thickness), white, edgeTexture);
		

		/* Banner */
		// Front
		subMesh.addQuad(new Vector3f(texel, bannerHeight, bannerDepth+texel), new Vector3f(width, bannerHeight, bannerDepth+texel), new Vector3f(width, texel*2, bannerDepth+texel), new Vector3f(texel, texel*2, bannerDepth+texel), bannerColor, frontTexture);
		
		// Back
		subMesh.addQuad(new Vector3f(width, bannerHeight, bannerDepth), new Vector3f(texel, bannerHeight, bannerDepth), new Vector3f(texel, texel*2, bannerDepth), new Vector3f(width, texel*2, bannerDepth), bannerColor, frontTexture);
		
		// Top
		subMesh.addQuad(new Vector3f(texel, bannerHeight, bannerDepth), new Vector3f(width, bannerHeight, bannerDepth), new Vector3f(width, bannerHeight, bannerDepth+texel), new Vector3f(texel, bannerHeight, bannerDepth+texel), bannerColor, bannerSideTexture);
		
		// Left edge
		subMesh.addQuad(new Vector3f(texel, bannerHeight, bannerDepth), new Vector3f(texel, bannerHeight, bannerDepth+texel), new Vector3f(texel, texel*2, bannerDepth+texel), new Vector3f(texel, texel*2, bannerDepth), bannerColor, bannerSideTexture);
		
		// Right edge
		subMesh.addQuad(new Vector3f(width, bannerHeight, bannerDepth+texel), new Vector3f(width, bannerHeight, bannerDepth), new Vector3f(width, texel*2, bannerDepth), new Vector3f(width, texel*2, bannerDepth+texel), bannerColor, bannerSideTexture);
		
		final float xOffset = x;
		final float yOffset;
		final float zOffset = z;
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		if (hasPost)
		{
			yOffset = y;
			// Add a post
			
			// North face
			subMesh.addQuad(new Vector3f(postRight, postHeight, postLeft), new Vector3f(postLeft, postHeight, postLeft), new Vector3f(postLeft, 0, postLeft), new Vector3f(postRight, 0, postLeft), white, sideTexture);
			
			// South face
			subMesh.addQuad(new Vector3f(postLeft, postHeight, postRight), new Vector3f(postRight, postHeight, postRight), new Vector3f(postRight, 0, postRight), new Vector3f(postLeft, 0, postRight), white, sideTexture);
			
			// West face
			subMesh.addQuad(new Vector3f(postLeft, postHeight, postLeft), new Vector3f(postLeft, postHeight, postRight), new Vector3f(postLeft, 0, postRight), new Vector3f(postLeft, 0, postLeft), white, sideTexture2);
			
			// East face
			subMesh.addQuad(new Vector3f(postRight, postHeight, postRight), new Vector3f(postRight, postHeight, postLeft), new Vector3f(postRight, 0, postLeft), new Vector3f(postRight, 0, postRight), white, sideTexture2);
			
			rotation = Rotation.AntiClockwise;
			angle = 90 / 4.0f * data;
		}
		else
		{
			yOffset = y - 1;
			
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
		
		
		
		subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset, zOffset, rotation, angle);
	}
	
}

