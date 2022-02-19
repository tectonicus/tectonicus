/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.blockTypes.BlockRegistry;
import tectonicus.blockTypes.BlockStateWrapper;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.configuration.Map;
import tectonicus.configuration.NorthDirection;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BedEntity;
import tectonicus.raw.Biome;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.Colour4f;
import tectonicus.util.Vector2f;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ItemRenderer
{	
	private final Rasteriser rasteriser;
	
	public ItemRenderer(Rasteriser rasteriser) throws Exception
	{	
		this.rasteriser = rasteriser;
	}
	
	public void renderCompass(Map map, File outFile) throws Exception
	{
		System.out.println("Generating compass image...");
		
		BufferedImage compassImage = null;
		try
		{
			File compassFile = map.getCustomCompassRose();
			if (compassFile != null)
			{
				compassImage = ImageIO.read(compassFile);
			}
		}
		catch (Exception e)
		{
			System.err.println("Error while trying to read custom compass rose image: "+e);
			System.err.println("Tried to read:"+map.getCustomCompassRose().getAbsolutePath());
			e.printStackTrace();
		}
		
		if (compassImage == null)
			compassImage = ImageIO.read( getClass().getClassLoader().getResourceAsStream("Images/Compass.png") );
		
		ItemGeometry item = createCompassGeometry(rasteriser, map.getNorthDirection(), compassImage);
		
		renderItem(item, outFile, 2, map.getCameraAngleRad(), map.getCameraElevationRad());
	}
	
	public void renderPortal(File outFile, BlockTypeRegistry registry, TexturePack texturePack) throws Exception
	{
		System.out.println("Generating portal image...");
		
		ItemContext context = new ItemContext(texturePack, registry);
		
		Geometry geometry = new Geometry(rasteriser);
		
		RawChunk rawChunk = new RawChunk();
		
		// Bottom row
		rawChunk.setBlockId(0, 0, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(1, 0, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(2, 0, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(3, 0, 0, (byte)BlockIds.OBSIDIAN);
		
		// First collumn
		rawChunk.setBlockId(0, 1, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(0, 2, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(0, 3, 0, (byte)BlockIds.OBSIDIAN);
		
		// Second collumn
		rawChunk.setBlockId(3, 1, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(3, 2, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(3, 3, 0, (byte)BlockIds.OBSIDIAN);
		
		// Top row
		rawChunk.setBlockId(0, 4, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(1, 4, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(2, 4, 0, (byte)BlockIds.OBSIDIAN);
		rawChunk.setBlockId(3, 4, 0, (byte)BlockIds.OBSIDIAN);
		
		for (int y=0; y< Minecraft.getChunkHeight(); y++)
		{
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					final int blockId = rawChunk.getBlockId(x, y, z);
					final int blockData = rawChunk.getBlockData(x, y, z);
					
					BlockType type = registry.find(blockId, blockData);
					if (type != null)
					{
						if (x == 0 || y == 0 || z == 0 || x == RawChunk.WIDTH-1 || y == Minecraft.getChunkHeight()-1 || z == RawChunk.DEPTH-1)
						{
							type.addEdgeGeometry(x, y, z, context, registry, rawChunk, geometry);
						}
						else
						{
							type.addInteriorGeometry(x, y, z, context, registry, rawChunk, geometry);
						}
					}
				}
			}
		}
		
		BoundingBox bounds = new BoundingBox(new Vector3f(0, 0, 0), 4, 5, 1);

		ItemGeometry item = new ItemGeometry(geometry, bounds);
		renderItem(item, outFile, 4, getAngleRad(55), getAngleRad(35));
	}
	
	public void renderBlock(File outFile, BlockTypeRegistry registry, TexturePack texturePack, int blockId, int blockData) throws Exception
	{		
		ItemContext context = new ItemContext(texturePack, registry);
		
		Geometry geometry = new Geometry(rasteriser);
		
		RawChunk rawChunk = new RawChunk();
		
		rawChunk.setBlockId(0, 0, 0, (byte)blockId);
		rawChunk.setBlockData(0, 0, 0, (byte)blockData);
		rawChunk.setBlockLight(0, 0, 0, (byte)16);
		rawChunk.setSkyLight(0, 0, 0, (byte) 16);
		
		BlockType type = registry.find(blockId, blockData);
		if (type != null)
		{
			type.addInteriorGeometry(0, 0, 0, context, registry, rawChunk, geometry);
		}
		
		BoundingBox bounds = new BoundingBox(new Vector3f(0, 0.1f, 0), 1, 1, 1);

		ItemGeometry item = new ItemGeometry(geometry, bounds);
		renderItem(item, outFile, 4, getAngleRad(45), getAngleRad(25));
	}
	
	public void renderSign(File outFile, BlockTypeRegistry registry, TexturePack texturePack, int blockId, int blockData) throws Exception
	{		
		System.out.println("Generating sign icon...");
		
		ItemContext context = new ItemContext(texturePack, registry);
		
		Geometry geometry = new Geometry(rasteriser);
		
		RawChunk rawChunk = new RawChunk();
		
		rawChunk.setBlockId(0, 0, 0, (byte)blockId);
		rawChunk.setBlockData(0, 0, 0, (byte)blockData);
		rawChunk.setBlockLight(0, 0, 0, (byte)16);
		rawChunk.setSkyLight(0, 0, 0, (byte) 16);
		HashMap<String, SignEntity> signs = new HashMap<>();
		signs.put("x0y0z0", new SignEntity(0, 0, 0, 0, 0, 0, "", "Tectonicus", "", "", blockData, "black"));
		rawChunk.setSigns(signs);
		
		BlockType type = registry.find(blockId, blockData);
		if (type != null)
		{
			type.addInteriorGeometry(0, 0, 0, context, registry, rawChunk, geometry);
		}
		
		BoundingBox bounds = new BoundingBox(new Vector3f(0, 0.4f, 0), 1, 1, 0);

		ItemGeometry item = new ItemGeometry(geometry, bounds);
		renderItem(item, outFile, 4, getAngleRad(45), getAngleRad(25));
	}
	
	public void renderBed(File outFile, BlockTypeRegistry registry, TexturePack texturePack) throws Exception
	{
		System.out.println("Generating bed icon...");
		
		ItemContext context = new ItemContext(texturePack, registry);
		
		Geometry geometry = new Geometry(rasteriser);
		
		RawChunk rawChunk = new RawChunk();
		
		rawChunk.setBlockId(0, 0, 0, (byte)BlockIds.BED);
		rawChunk.setBlockData(0, 0, 0, (byte)10);
		rawChunk.setBlockLight(0, 0, 0, (byte)16);
		rawChunk.setSkyLight(0, 0, 0, (byte) 16);
		rawChunk.setBlockId(0, 0, 1, (byte)BlockIds.BED);
		rawChunk.setBlockData(0, 0, 1, (byte)6);
		rawChunk.setBlockLight(0, 0, 1, (byte)16);
		rawChunk.setSkyLight(0, 0, 1, (byte) 16);
		HashMap<String, BedEntity> beds = new HashMap<>();
		beds.put("x0y0z0", new BedEntity(0, 0, 0, 0, 0, 0, 14));
		beds.put("x0y0z1", new BedEntity(0, 0, 1, 0, 0, 1, 14));
		rawChunk.setBeds(beds);
		
		BlockType type = registry.find(BlockIds.BED, 10);
		if (type != null)
		{
			type.addInteriorGeometry(0, 0, 0, context, registry, rawChunk, geometry);
			type.addInteriorGeometry(0, 0, 1, context, registry, rawChunk, geometry);
		}
		
		BoundingBox bounds = new BoundingBox(new Vector3f(-1, -0.5f, 0), 2, 1, 0);
				
		ItemGeometry item = new ItemGeometry(geometry, bounds);
		renderItem(item, outFile, 4, getAngleRad(65), getAngleRad(35));
	}
	
	private void renderItem(ItemGeometry item, File outFile, final int numDownsamples, final float cameraAngle, final float cameraElevationAngle)
	{	
		Geometry geometry = item.geometry;
		BoundingBox bounds = item.bounds;
		
		Color colourKey = new Color(255, 0, 128);
		
		rasteriser.beginFrame();
		rasteriser.resetState();
		rasteriser.clear(colourKey);
		rasteriser.clearDepthBuffer();
		
		OrthoCamera camera = new OrthoCamera(rasteriser, 512, 512);
		final float lookX = bounds.getCenterX();
		final float lookY = bounds.getCenterY();
		final float lookZ = bounds.getCenterZ();
		final float size = (float)Math.sqrt(bounds.getWidth()*bounds.getWidth() + bounds.getHeight()*bounds.getHeight());
		
		camera.lookAt(lookX, lookY, lookZ, size, cameraAngle, cameraElevationAngle);
		camera.apply();
		
		ArrayList<Vector2f> corners = new ArrayList<>();
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x,						bounds.getOrigin().y,	bounds.getOrigin().z) ) );
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x+bounds.getWidth(),	bounds.getOrigin().y,	bounds.getOrigin().z) ) );
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x, 					bounds.getOrigin().y,	bounds.getOrigin().z+bounds.getDepth()) ) );
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x+bounds.getWidth(),	bounds.getOrigin().y,	bounds.getOrigin().z+bounds.getDepth()) ) );
		
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x,						bounds.getOrigin().y+bounds.getHeight(),	bounds.getOrigin().z) ) );
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x+bounds.getWidth(),	bounds.getOrigin().y+bounds.getHeight(),	bounds.getOrigin().z) ) );
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x, 					bounds.getOrigin().y+bounds.getHeight(),	bounds.getOrigin().z+bounds.getDepth()) ) );
		corners.add( camera.projectf(new Vector3f(bounds.getOrigin().x+bounds.getWidth(),	bounds.getOrigin().y+bounds.getHeight(),	bounds.getOrigin().z+bounds.getDepth()) ) );
		
		float minX = Integer.MAX_VALUE;
		float minY = Integer.MAX_VALUE;
		float maxX = Integer.MIN_VALUE;
		float maxY = Integer.MIN_VALUE;
		for (Vector2f p : corners)
		{
			minX = Math.min(minX, p.x);
			maxX = Math.max(maxX, p.x);
			
			minY = Math.min(minY, p.y);
			maxY = Math.max(maxY, p.y);
		}
		
		Vector3f topLeftWorld = camera.unproject(new Vector2f(minX, minY));
		Vector3f topRightWorld = camera.unproject(new Vector2f(maxX, minY));
		Vector3f bottomLeftWorld = camera.unproject(new Vector2f(minX, maxY));
		
		final float xSize = topLeftWorld.sub(topRightWorld, new Vector3f()).length();
		final float ySize = topLeftWorld.sub(bottomLeftWorld, new Vector3f()).length();
		float longest = Math.max(xSize, ySize);
		
		camera.lookAt(lookX, lookY, lookZ, longest, cameraAngle, cameraElevationAngle);
		camera.apply();
		
		geometry.finalise();
		
		{
			rasteriser.enableAlphaTest(false);
			rasteriser.enableBlending(false);
			rasteriser.enableDepthWriting(true);
			
			geometry.drawSolidSurfaces(0, 0, 0);
		}
		
		{
			rasteriser.enableAlphaTest(true);
			rasteriser.enableBlending(false);
			rasteriser.enableDepthWriting(true);
			
			geometry.drawAlphaTestedSurfaces(0, 0, 0);
		}
		
		{
			rasteriser.enableAlphaTest(false);
			rasteriser.enableBlending(true);
			rasteriser.enableDepthWriting(false);
			
			geometry.drawTransparentSurfaces(0, 0, 0);
		}
		
		// Reset to default
		rasteriser.enableAlphaTest(false);
		rasteriser.enableBlending(false);
		rasteriser.enableDepthWriting(true);
		
		BufferedImage outImg = rasteriser.takeScreenshot(0, 0, 512, 512, ImageFormat.Png);
		
		filterColourKey(outImg, colourKey);
		
		for (int i=0; i<numDownsamples; i++)
		{
			outImg = downsample(outImg);
		}
		
		Screenshot.write(outFile, outImg, ImageFormat.Png, 1.0f);
	}
	
	private float getAngleRad(int angle)
	{
		final float normalised = (float)angle / 360.0f;
		return normalised * (float)Math.PI * 2.0f;
	}
	
	private static ItemGeometry createCompassGeometry(Rasteriser rasteriser, NorthDirection dir, BufferedImage img)
	{
		Geometry geometry = new Geometry(rasteriser);
		
		SubMesh mesh = new SubMesh();
		
		for (int x=0; x<img.getWidth(); x++)
		{
			for (int y=0; y<img.getHeight(); y++)
			{
				final int rgb = img.getRGB(x, y);
				final int alpha = (rgb>>24) & 0xFF;
				
				if (alpha > 128)
				{
					Color c = new Color(img.getRGB(x, y));
					Vector4f colour = new Vector4f(c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, 1.0f);

					Vector4f lighter = new Vector4f();
					colour.add(new Vector4f(0.1f, 0.1f, 0.1f, 0), lighter);
					clamp(lighter, 0, 1);

					Vector4f darker = new Vector4f();
					colour.add(new Vector4f(-0.1f, -0.1f, -0.1f, 0), darker);
					clamp(darker, 0, 1);
					
					final float xx = y - (img.getHeight()/2);
					final float yy = 0;
					final float zz = img.getWidth()-x - (img.getWidth()/2);
					
					Vector3f p0 = new Vector3f(xx,   yy, zz);
					Vector3f p1 = new Vector3f(xx+1, yy, zz);
					Vector3f p2 = new Vector3f(xx+1, yy, zz+1);
					Vector3f p3 = new Vector3f(xx,   yy, zz+1);
					
					Vector3f p4 = new Vector3f(xx,   yy+1, zz);
					Vector3f p5 = new Vector3f(xx+1, yy+1, zz);
					Vector3f p6 = new Vector3f(xx+1, yy+1, zz+1);
					Vector3f p7 = new Vector3f(xx,   yy+1, zz+1);
					
					SubTexture tex = new SubTexture(null, 0, 0, 0, 0);
					
					// Bottom, top
					mesh.addQuad(p1, p0, p3, p2, lighter, tex);
					mesh.addQuad(p4, p5, p6, p7, lighter, tex);
					
					// Sides
					mesh.addQuad(p7, p6, p2, p3, darker, tex);
					mesh.addQuad(p6, p5, p1, p2, colour, tex);
					
					mesh.addQuad(p5, p4, p0, p1, darker, tex);
					mesh.addQuad(p4, p7, p3, p0, colour, tex);
				}
			}
		}
		
		float compassRotation = 0;
		if (dir == NorthDirection.PlusX)
		{
			compassRotation = 180;
		}
		else if (dir == NorthDirection.MinusX)
		{
			compassRotation = 0;
		}
		else if (dir == NorthDirection.PlusZ)
		{
			compassRotation = 90;
		}
		else if (dir == NorthDirection.MinusZ)
		{
			compassRotation = 270;
		}
		
		mesh.pushTo(geometry.getBaseMesh(), 0, 0, 0, Rotation.Clockwise, compassRotation);
		
		BoundingBox bounds = new BoundingBox(new Vector3f(-img.getHeight()/2, 0, -img.getWidth()/2), img.getHeight()+1, 1, img.getWidth()+1);
		
		return new ItemGeometry(geometry, bounds);
	}
	
	
	public static void filterColourKey(BufferedImage image, Color colourKey)
	{
		for (int x=0; x<image.getWidth(); x++)
		{
			for (int y=0; y<image.getHeight(); y++)
			{
				final int rgb = image.getRGB(x, y);
				
				final int dRed = Math.abs(getRed(rgb) - colourKey.getRed());
				final int dGreen = Math.abs(getGreen(rgb) - colourKey.getGreen());
				final int dBlue = Math.abs(getBlue(rgb) - colourKey.getBlue());
				
				final int delta = dRed + dGreen + dBlue;
				
				// If the total colour delta is lower than the threshold then mark
				// the pixel as transparent
				if (delta < 3)
				{
					image.setRGB(x, y, 0x0);
				}
			}
		}
	}
	
	private static int getRed(final int rgba)
	{
		return (rgba >> 16) & 0xFF;
	}
	
	private static int getGreen(final int rgba)
	{
		return (rgba >> 8) & 0xFF;
	}
	
	private static int getBlue(final int rgba)
	{
		return (rgba) & 0xFF;
	}
	
	public static void clamp(Vector4f vec, final float min, final float max)
	{
		vec.x = Math.min(Math.max(vec.x, min), max);
		vec.y = Math.min(Math.max(vec.y, min), max);
		vec.z = Math.min(Math.max(vec.z, min), max);
		vec.w = Math.min(Math.max(vec.w, min), max);
	}
	
	public static BufferedImage downsample(BufferedImage src)
	{
		BufferedImage out = new BufferedImage(src.getWidth()/2, src.getHeight()/2, BufferedImage.TYPE_4BYTE_ABGR);
		
		Graphics2D g = (Graphics2D)out.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
		
		return out;
	}
	
	private static class ItemGeometry
	{
		private Geometry geometry;
		private BoundingBox bounds;
		
		public ItemGeometry(Geometry g, BoundingBox b)
		{
			this.geometry = g;
			this.bounds = b;
		}
	}
	
	private class ItemContext implements BlockContext  //TODO: why does this implement BlockContext when it does nothing with any of the implemented methods?
	{
		private TexturePack texturePack;
		private BlockTypeRegistry registry;
		
		public ItemContext(TexturePack texturePack, BlockTypeRegistry registry)
		{
			this.texturePack = texturePack;
			this.registry = registry;
		}

		@Override
		public int getBlockId(ChunkCoord chunkCoord, int x, int y, int z)
		{
			return 0;
		}

		@Override
		public BlockType getBlockType(ChunkCoord chunkCoord, int x, int y, int z)
		{
			return registry.find(0, 0);
		}

		@Override
		public BlockStateWrapper getBlock(ChunkCoord chunkCoord, int x, int y, int z) { return null; }

		@Override
		public BlockStateWrapper getBlock(RawChunk rawChunk, int x, int y, int z) { return null; }

		@Override
		public BlockProperties getBlockState(ChunkCoord chunkCoord, int x, int y, int z) { return null; }
		
		@Override
		public float getLight(ChunkCoord chunkCoord, int x, int y, int z, LightFace face)
		{
			return 1;
		}

		@Override
		public LightStyle getLightStyle()
		{
			return LightStyle.Day;
		}

		@Override
		public TexturePack getTexturePack()
		{
			return texturePack;
		}

		@Override
		public Biome getBiome(ChunkCoord chunkCoord, int x, int y, int z)
		{
			return BiomesOld.OCEAN;
		}

		@Override
		public Colour4f getGrassColor(ChunkCoord chunkCoord, int x, int y, int z) {
			return new Colour4f(1, 1, 1, 1);
		}

		@Override
		public Colour4f getFoliageColor(ChunkCoord chunkCoord, int x, int y, int z) {
			return new Colour4f(1, 1, 1, 1);
		}

		@Override
		public Colour4f getWaterColor(RawChunk rawChunk, int x, int y, int z)
		{
			return new Colour4f(1, 1, 1, 1);
		}

		@Override
		public PlayerSkinCache getPlayerSkinCache()
		{
			return null;
		}
		
		@Override
		public BlockRegistry getModelRegistry()
		{
			return null;
		}
	}
}
