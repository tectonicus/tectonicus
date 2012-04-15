/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.configuration.Configuration;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.configuration.Map;
import tectonicus.configuration.NorthDirection;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.Colour4f;
import tectonicus.util.Vector2f;
import tectonicus.util.Vector3l;

public class ItemRenderer
{
	private final Configuration config;
	
	private final Rasteriser rasteriser;
	
	public ItemRenderer(Configuration config, Rasteriser rasteriser) throws Exception
	{
		this.config = config;
		
		this.rasteriser = rasteriser;
	}
	
	public void renderCompass(Map map, File outFile) throws Exception
	{
		System.out.println("Generating compass image...");
		
		BufferedImage compassImage = null;
		try
		{
		//	File compassFile = new File("/Users/John/TectonicusTests/SmallCompassRose.png");
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
		
		renderItem(map, item, outFile, 2);
	}
	
	public void renderPortal(File outFile, BlockTypeRegistry registry, TexturePack texturePack) throws Exception
	{
		System.out.println("Generating portal image...");
		
		ItemContext context = new ItemContext(texturePack, registry);
		
		Geometry geometry = new Geometry(rasteriser, texturePack.getTexture());
		
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
		
		for (int y=0; y<RawChunk.HEIGHT; y++)
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
						if (x == 0 || y == 0 || z == 0 || x == RawChunk.WIDTH-1 || y == RawChunk.HEIGHT-1 || z == RawChunk.DEPTH-1)
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
		
		BoundingBox bounds = new BoundingBox(new Vector3l(0, 0, 0), 4, 5, 1);
		
		Map placeholderMap = config.getMap(0);
		
		ItemGeometry item = new ItemGeometry(geometry, bounds);
		renderItem(placeholderMap, item, outFile, 4);
	}
	
	private void renderItem(Map map, ItemGeometry item, File outFile, final int numDownsamples)
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
		
		camera.lookAt(lookX, lookY, lookZ, size, map.getCameraAngleRad(), map.getCameraElevationRad());
		camera.apply();
		
		ArrayList<Vector2f> corners = new ArrayList<Vector2f>();
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
		
		final float xSize = Vector3f.sub(topLeftWorld, topRightWorld, null).length();
		final float ySize = Vector3f.sub(topLeftWorld, bottomLeftWorld, null).length();
		float longest = Math.max(xSize, ySize);
		
		camera.lookAt(lookX, lookY, lookZ, longest, map.getCameraAngleRad(), map.getCameraElevationRad());
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
	
	private static ItemGeometry createCompassGeometry(Rasteriser rasteriser, NorthDirection dir, BufferedImage img)
	{
		Geometry geometry = new Geometry(rasteriser, null);
		
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
					
					Vector4f lighter = Vector4f.add(colour, new Vector4f(0.1f, 0.1f, 0.1f, 0), null);
					clamp(lighter, 0, 1);
					
					Vector4f darker = Vector4f.add(colour, new Vector4f(-0.1f, -0.1f, -0.1f, 0), null);
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
		
		BoundingBox bounds = new BoundingBox(new Vector3l(-img.getHeight()/2, 0, -img.getWidth()/2), img.getHeight()+1, 1, img.getWidth()+1);
		
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
		vec.x = (float)Math.min(Math.max(vec.x, min), max);
		vec.y = (float)Math.min(Math.max(vec.y, min), max);
		vec.z = (float)Math.min(Math.max(vec.z, min), max);
		vec.w = (float)Math.min(Math.max(vec.w, min), max);
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
	
	private class ItemContext implements BlockContext
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
		public int getBiomeId(ChunkCoord chunkCoord, int x, int y, int z)
		{
			return 0;
		}

		@Override
		public Colour4f getGrassColour(ChunkCoord chunkCoord, int x, int y, int z)
		{
			return new Colour4f(1, 1, 1, 1);
		}
		
	}
}
