/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.Version;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BannerEntity;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.world.Colors;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public class Banner implements BlockType
{
	private static final int WIDTH = 15;
	private static final int HEIGHT = 2;
	private static final int THICKNESS = 2;
	private static final int POST_HEIGHT = 28;

	private final String id;
	private final String name;
	
	private final Map<String, BufferedImage> patternImages;
	
	private final SubTexture bannerSideTexture;
	private final SubTexture sideTexture;
	private final SubTexture sideTexture2;
	private final SubTexture edgeTexture;
	private final SubTexture topTexture;
	
	private final boolean hasPost;

	private final Version texturePackVersion;

	public Banner(String id, String name, SubTexture texture, final boolean hasPost, Map<String, BufferedImage> patternImages)
	{
		this.id = id;
		this.name = name;
		this.hasPost = hasPost;

		final float texel = 1.0f / 64.0f;

		this.bannerSideTexture = new SubTexture(texture.texture, texture.u0, texture.v0+texel, texture.u0+texel, texture.v0+texel*41);
		this.sideTexture = new SubTexture(texture.texture, texture.u0+texel*50, texture.v0+texel*2, texture.u0+texel*52, texture.v0+texel*43);
		this.sideTexture2 = new SubTexture(texture.texture, texture.u0+texel*48, texture.v0+texel*2, texture.u0+texel*50, texture.v0+texel*43);
		this.topTexture = new SubTexture(texture.texture, texture.u0+texel*2, texture.v0+texel*42, texture.u0+texel*22, texture.v0+texel*44);
		this.edgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0+texel*44, texture.u0+texel*2, texture.v0+texel*46);

		this.patternImages = patternImages;

		this.texturePackVersion = texture.getTexturePackVersion();
	}

	public Banner(String name, SubTexture texture, final boolean hasPost, Map<String, BufferedImage> patternImages)
	{
		this(StringUtils.EMPTY, name, texture, hasPost, patternImages);
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
		int data = rawChunk.getBlockData(x, y, z);
		final BlockProperties properties = rawChunk.getBlockState(x, y, z);
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
		
		SubMesh subMesh = new SubMesh();
		String xyz = "x" + x + "y" + y + "z" + z;
		BannerEntity entity = rawChunk.getBanners().get(xyz);
		Colors baseColor = Colors.byId(15-entity.getBaseColor());
		if (StringUtils.isNotEmpty(id)) {
			String bannerId = id.replace("minecraft:", "").replace("_wall", "").replace("_banner", "");
			baseColor = Colors.byName(bannerId);
		}
		List<Pattern> patterns = entity.getPatterns();

		//TODO: Only run this code if texture has not already been created
		final BufferedImage base = patternImages.get("base");
		BufferedImage finalImage = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = finalImage.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g.drawImage(base, 0, 0, null);

		StringBuilder identifier = new StringBuilder();
		identifier.append("banner_base_").append(baseColor.getName());

		if (texturePackVersion.getNumVersion() <= Version.VERSION_14.getNumVersion()) {
			addPattern(base, patternImages.get("baseMask"), baseColor.getColor(), g);
		} else {
			addPattern(patternImages.get("baseMask"), baseColor.getColor(), g);
		}

		if (!patterns.isEmpty())
		{
			for (Pattern pattern : patterns)
			{
				if (StringUtils.isNotEmpty(id)) {
					if (texturePackVersion == Version.VERSION_13 || texturePackVersion == Version.VERSION_14) {
						addPattern(base, patternImages.get(pattern.pattern), Colors.byId(pattern.color).getColor(), g);
					} else {
						addPattern(patternImages.get(pattern.pattern), Colors.byId(pattern.color).getColor(), g);
					}
				} else {
					addPattern(base, patternImages.get(pattern.pattern), Colors.byId(15-pattern.color).getColor(), g);
				}
				identifier.append(pattern.toString());
			}
		}
		
		SubTexture texture = world.getTexturePack().findTexture(finalImage, identifier.toString());
		final float texel2 = 1.0f / 64.0f;
		final SubTexture frontTexture = new SubTexture(texture.texture, texture.u0+texel2, texture.v0+texel2, texture.u0+texel2*21, texture.v0+texel2*41);
		final SubTexture backTexture = new SubTexture(texture.texture, texture.u0+texel2*22, texture.v0+texel2, texture.u0+texel2*42, texture.v0+texel2*41);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float texel = 1.0f / 16.0f;
		final float signBottom = 1.0f / 16.0f * POST_HEIGHT;

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
			yOffset = y - 1.0f;
			
			if (data == 2)
			{
				// Facing north
				rotation = Rotation.Clockwise;
				angle = 180;
			}
			// data == 3 Facing south
			// ...built this way
			else if (data == 4)
			{
				// Facing west
				rotation = Rotation.AntiClockwise;
				angle = 90;
				
			}
			else if (data == 5)
			{
				// Facing east
				rotation = Rotation.Clockwise;
				angle = 90;
			}
		}
		
		subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset, zOffset, rotation, angle);
	}



	private void addPattern(BufferedImage pattern, Color color, Graphics2D g) {
		//tint(p, Colors.byId(pattern.color).getColor());
		if (pattern != null) {
			tintFast(pattern, color);
			g.drawImage(pattern, 0, 0, null);
		}
	}

	private void tint(BufferedImage image, Color color) {
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				Color pixelColor = new Color(image.getRGB(x, y), true);
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();
				int a = pixelColor.getAlpha();
				int rgba = (a << 24) | (r << 16) | (g << 8) | b;
				image.setRGB(x, y, rgba);
			}
		}
	}

	//I haven't actually tested if this method is faster but I put Fast in the name just in case :)
	private void tintFast(BufferedImage image, Color color) {
		final float heightRatio = 41.0f/64.0f;
		final float widthRatio = 42.0f/64.0f;
		//We use this smaller width and height because banner patterns don't take up all of the 64x64 image
		final int height = (int) (image.getHeight() * heightRatio);
		final int width = (int) (image.getWidth() * widthRatio);

		int newRgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

		int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);

		int alphaMask = 0xff000000;
		for (int i = 0; i < rgb.length; i++) {
			rgb[i] = (rgb[i] & alphaMask) | newRgb;
		}
		image.setRGB(0, 0, width, height, rgb, 0, width);
	}

	private void addPattern(BufferedImage base, BufferedImage pattern, Color currentColor, Graphics2D g)
	{
		BufferedImage maskedImage = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		final float heightRatio = 41.0f/64.0f;
		final float widthRatio = 42.0f/64.0f;
		final int height = (int) (base.getHeight() * heightRatio);
		final int width = (int) (base.getWidth() * widthRatio);
		
		try 
		{
			for (int y = 0; y < height; y++)
			{
			    for (int x = 0; x < width; x++)
			    {
			    	Color baseColor = new Color(base.getRGB(x, y));
			    	Color maskColor = new Color(pattern.getRGB(x, y));
			    	Color maskedColor = new Color((baseColor.getRed()*currentColor.getRed())/255, (baseColor.getGreen()*currentColor.getGreen())/255, (baseColor.getBlue()*currentColor.getBlue())/255, maskColor.getRed());
			    	maskedImage.setRGB(x, y, maskedColor.getRGB());
			    }
			}
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			e.printStackTrace();
		}

		g.drawImage(maskedImage, 0, 0, null);
	}
	
	public static class Pattern
	{
		String pattern;
		int color;
		
		public Pattern(String pattern, int color)
		{
			this.pattern = pattern;
			this.color = color;
		}
		
		@Override
		public String toString()
		{
			return this.pattern + this.color;
		}
	}
}

