/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.jnbt.CompoundTag;
import org.json.JSONException;
import org.json.JSONObject;
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
	
	private final List<Color> colors;
	
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
		
		final Color black = new Color(25, 25, 25, 255);
		final Color red = new Color(153, 51, 51, 255);
		final Color green = new Color(102, 127, 51, 255);
		final Color brown = new Color(102, 76, 51, 255);
		final Color blue = new Color(51, 76, 178, 255);
		final Color purple = new Color(127, 63, 178, 255);
		final Color cyan = new Color(76, 127, 153, 255);
		final Color lightGray = new Color(153, 153, 153, 255);
		final Color gray = new Color(76, 76, 76, 255);
		final Color pink = new Color(242, 127, 165, 255);
		final Color lime = new Color(127, 204, 25, 255);
		final Color yellow = new Color(229, 229, 51, 255);
		final Color lightBlue = new Color(102, 153, 216, 255);
		final Color magenta = new Color(178, 76, 216, 255);
		final Color orange = new Color(216, 127, 51, 255);
		final Color white = new Color(255, 255, 255, 255);
		this.colors = Arrays.asList(black, red, green, brown, blue, purple, cyan, lightGray, gray, pink, lime, yellow, lightBlue, magenta, orange, white);
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
		String[] patternStrings = null;
		for (TileEntity te : rawChunk.getBanners())
		{
			if (te.localX == x && te.localY == y && te.localZ == z)
			{
				baseColor = te.blockData;
				if(!te.motive.equals(""))
				{
					try
					{
						JSONObject p = new JSONObject(te.motive);
						System.out.println(te.motive);
						patternStrings = JSONObject.getNames(p);
						for(String pattern : patternStrings)
						{
							System.out.println(pattern);
						}
						//System.out.println(p);
					} catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
				break;
			}
		}
		
		HashMap<String, BufferedImage> patterns = world.getTexturePack().loadPatterns();
		
		SubTexture testTexture = null;
		try 
		{
			BufferedImage pattern = world.getTexturePack().loadTexture("assets/minecraft/textures/entity/banner/creeper.png");
			BufferedImage pattern2 = world.getTexturePack().loadTexture("assets/minecraft/textures/entity/banner/triangles_bottom.png");
			BufferedImage basePattern = world.getTexturePack().loadTexture("assets/minecraft/textures/entity/banner/base.png");
			final BufferedImage base = world.getTexturePack().loadTexture("assets/minecraft/textures/entity/banner_base.png");
			BufferedImage finalImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D g = finalImage.createGraphics();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			g.drawImage(base, 0, 0, null);
			
			Color currentColor = colors.get(1);
			
			addPattern(base, basePattern, colors.get(baseColor), g);
			if (patternStrings != null)
			{
				for (String singlePattern : patternStrings)
				{
					addPattern(base, patterns.get(singlePattern), colors.get(15), g);
				}
			}
			//addPattern(base, pattern, currentColor, g);
			//addPattern(base, pattern2, colors.get(0), g);
			
			ImageIO.write(finalImage, "png", new File("E:/testworld/Images/finalImage.png"));
			
			testTexture = world.getTexturePack().findTexture(finalImage, "banner_base_"+baseColor);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		final float texel2 = 1.0f / 64.0f;
		final SubTexture frontTexture = new SubTexture(testTexture.texture, testTexture.u0+texel2, testTexture.v0+texel2, testTexture.u0+texel2*21, testTexture.v0+texel2*41);
		final SubTexture backTexture = new SubTexture(testTexture.texture, testTexture.u0+texel2*22, testTexture.v0+texel2, testTexture.u0+texel2*42, testTexture.v0+texel2*41);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
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
		subMesh.addQuad(new Vector3f(texel, bannerHeight, bannerDepth+texel), new Vector3f(width, bannerHeight, bannerDepth+texel), new Vector3f(width, texel*2, bannerDepth+texel), new Vector3f(texel, texel*2, bannerDepth+texel), white, frontTexture);
		
		// Back
		subMesh.addQuad(new Vector3f(width, bannerHeight, bannerDepth), new Vector3f(texel, bannerHeight, bannerDepth), new Vector3f(texel, texel*2, bannerDepth), new Vector3f(width, texel*2, bannerDepth), white, backTexture);
		
		// Top
		subMesh.addQuad(new Vector3f(texel, bannerHeight, bannerDepth), new Vector3f(width, bannerHeight, bannerDepth), new Vector3f(width, bannerHeight, bannerDepth+texel), new Vector3f(texel, bannerHeight, bannerDepth+texel), white, bannerSideTexture);
		
		// Left edge
		subMesh.addQuad(new Vector3f(texel, bannerHeight, bannerDepth), new Vector3f(texel, bannerHeight, bannerDepth+texel), new Vector3f(texel, texel*2, bannerDepth+texel), new Vector3f(texel, texel*2, bannerDepth), white, bannerSideTexture);
		
		// Right edge
		subMesh.addQuad(new Vector3f(width, bannerHeight, bannerDepth+texel), new Vector3f(width, bannerHeight, bannerDepth), new Vector3f(width, texel*2, bannerDepth), new Vector3f(width, texel*2, bannerDepth+texel), white, bannerSideTexture);
		
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
		
		subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset, zOffset, rotation, angle);
	}

	private void addPattern(BufferedImage base, BufferedImage pattern, Color currentColor, Graphics2D g) throws IOException
	{
		BufferedImage maskedImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		
		for (int y = 0; y < 41; y++)
		{
		    for (int x = 0; x < 42; x++)
		    {
		    	Color baseColor = new Color(base.getRGB(x, y));
		    	Color maskColor = new Color(pattern.getRGB(x, y));
		    	Color maskedColor = new Color((baseColor.getRed()*currentColor.getRed())/255, (baseColor.getGreen()*currentColor.getGreen())/255, (baseColor.getBlue()*currentColor.getBlue())/255, maskColor.getRed());
		    	maskedImage.setRGB(x, y, maskedColor.getRGB());
		    }
		}
		
		ImageIO.write(maskedImage, "png", new File("E:/testworld/Images/maskedImage.png"));
		
//		Graphics2D g = base.createGraphics();
//		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g.drawImage(maskedImage, 0, 0, null);
	}
}

