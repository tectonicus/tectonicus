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
package tectonicus.texture;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.renderer.Font;
import tectonicus.texture.ZipStack.ZipStackEntry;

public class TexturePack
{
	private Rasteriser rasteriser;
	
	private Texture vignetteTexture;
	
	private BufferedImage itemSheet;
	private BufferedImage iconSheet;
	
	private BufferedImage grassLookupImage;
	private BufferedImage foliageLookupImage;
	
	private Font font;
	
	private ZipStack zipStack;
	
	private Map<String, PackTexture> loadedPackTextures;
	
	public TexturePack(Rasteriser rasteriser, File minecraftJar, File texturePack)
	{
		if (!minecraftJar.exists())
			throw new RuntimeException("Couldn't find minecraft.jar at "+minecraftJar.getAbsolutePath());
	
		this.rasteriser = rasteriser;
		
		loadedPackTextures = new HashMap<String, PackTexture>();
		
		try
		{
			zipStack = new ZipStack(minecraftJar, texturePack);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't open jar files for texture reading", e);
		}
		
		try
		{
			findTexture("terrain.png[0, 0]");
			
		/*	ZipStackEntry terrainEntry = zipStack.getEntry("terrain.png");
			if (terrainEntry != null)
			{
				BufferedImage terrainImage = copy( ImageIO.read(terrainEntry.getInputStream()) );
				
				BufferedImage[] mipmaps = PackTexture.generateTileMips(terrainImage);
				
				terrainTexture = rasteriser.createTexture(mipmaps, TextureFilter.NEAREST);
				
				final float tileU = 1.0f / 16.0f;
				final float tileV = 1.0f / 16.0f;
				
				final float uNudge = tileU / 64.0f;
				final float vNudge = tileV / 64.0f;
				
				for (int tileX=0; tileX<16; tileX++)
				{
					for (int tileY=0; tileY<16; tileY++)
					{
						final float u0 = tileX * tileU + uNudge;
						final float u1 = (tileX+1) * tileU - uNudge;
						
						final float v0 = tileY * tileV + vNudge;
						final float v1 = (tileY+1) * tileV - vNudge;
						
						SubTexture sub = new SubTexture(terrainTexture, u0, v0, u1, v1);
						tiles.put(new Point(tileX, tileY), sub);
					}
				}
			}
			else
			{
				ZipStackEntry serverClass = zipStack.getEntry("net/minecraft/server/MinecraftServer.class");
				final boolean isServerJar = serverClass != null;
				
				ZipStackEntry launcherClass = zipStack.getEntry("net/minecraft/MinecraftLauncher.class");
				final boolean isLauncherJar = launcherClass != null;
				
				if (isServerJar)
				{
					throw new RuntimeException("Couldn't find terrain.png in "+minecraftJar.getName()+", make sure you're using the minecraft client jar, not the server jar!");
				}
				else if (isLauncherJar)
				{
					throw new RuntimeException("Couldn't find terrain.png in "+minecraftJar.getName()+", make sure you're using the minecraft client jar, not the client launcher jar!");
				}
				else
				{
					throw new RuntimeException("Couldn't find terrain.png in "+formatPaths(minecraftJar, texturePack));
				}
			}
		*/
			
			ZipStackEntry vignetteEntry = zipStack.getEntry("misc/vignette.png");
			if (vignetteEntry != null)
			{
				BufferedImage vignetteImage = copy( ImageIO.read(vignetteEntry.getInputStream()) );
				vignetteTexture = rasteriser.createTexture(vignetteImage, TextureFilter.LINEAR);
			}
			
			ZipStackEntry itemsEntry = zipStack.getEntry("gui/items.png");
			if (itemsEntry != null)
			{
				itemSheet = copy( ImageIO.read( itemsEntry.getInputStream() ) );
			}
			else
			{
				throw new RuntimeException("Couldn't find items.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry iconsEntry = zipStack.getEntry("gui/icons.png");
			if (iconsEntry != null)
			{
				iconSheet = copy( ImageIO.read( iconsEntry.getInputStream() ) );
			}
			else
			{
				throw new RuntimeException("Couldn't find icons.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry grassEntry = zipStack.getEntry("misc/grasscolor.png");
			if (grassEntry != null)
			{
				grassLookupImage = copy( ImageIO.read( grassEntry.getInputStream() ) );
			//	ImageIO.write(grassLookupImage, "png", new File("/Users/John/grass.png"));
			}
			else
			{
				throw new RuntimeException("Couldn't find grasscolor.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry foliageEntry = zipStack.getEntry("misc/foliagecolor.png");
			if (foliageEntry != null)
			{
				foliageLookupImage = copy( ImageIO.read( foliageEntry.getInputStream() ) );
			}
			else
			{
				throw new RuntimeException("Couldn't find foliagecolor.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry fontEntry = zipStack.getEntry("font/default.png");
			ZipStackEntry fontTextEntry = zipStack.getEntry("font.txt");
			if (fontEntry != null && fontTextEntry != null)
			{
				BufferedImage fontSheet = ImageIO.read( fontEntry.getInputStream() );
				InputStream textIn = fontTextEntry.getInputStream();
				
				font = new Font(rasteriser, fontSheet, textIn);
			}
			else
			{
				throw new RuntimeException("Couldn't find font resources in "+formatPaths(minecraftJar, texturePack));
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't load textures from "+formatPaths(minecraftJar, texturePack), e);
		}
	}
	
	private String formatPaths(File first, File second)
	{
		String result = "";
		
		if (first != null)
			result += first.getAbsolutePath();
		
		result += " ";
		
		if (second != null)
			result += second.getAbsolutePath();
		
		return result.trim();
	}
	
	// todo: refactor to remove this? why is it still needed?
	public Texture getTexture()
	{
	//	return terrainTexture;
		return findTexture("terrain").texture;
	}
	
	public SubTexture findTexture(String texturePath)
	{
		SubTexture result = null;
		
		TextureRequest request = parseRequest(texturePath);
		
		// First see if we already have it loaded
	//	result = loadedSubTextures.get(request);
		
		// If not already loaded, then load it and cache it
	//	if (result != null)
		{
			PackTexture tex = findTexture(request); // find existing or load
			
			result = tex.find(request); // find existing or load
			assert (result != null);
			
		//	loadedSubTextures.put(request, result);
		}
		
		return result;
	}
	
	private TextureRequest parseRequest(String texturePath)
	{
		// texture path could be:
		// terrain[0, 1]
		// terrain(0, 1, 2, 3)
		// path/file.ext
		// path/file.ext[0, 1]
		// path/file.ext(0, 1, 2, 3)
		
		texturePath = texturePath.trim();
		
		String path;
		String params;
		if (texturePath.indexOf('[') != -1)
		{
			final int split = texturePath.indexOf('[');
			path = texturePath.substring(0, split).trim();
			params = texturePath.substring(split).trim();
		}
		else if (texturePath.indexOf('(') != -1)
		{
			final int split = texturePath.indexOf('(');
			path = texturePath.substring(0, split).trim();
			params = texturePath.substring(split).trim();
		}
		else
		{
			path = texturePath;
			params = "";
		}
		
		// The name 'texture' is a synonym for terrain.png
		if (path.equals("terrain"))
			path = "terrain.png";
		
		return new TextureRequest(path, params);
	}
	
	private PackTexture findTexture(TextureRequest request)
	{
		PackTexture tex = loadedPackTextures.get(request.path);
		
		if (tex == null)
		{
			BufferedImage img = loadTexture(request.path);
			if (img != null)
			{
				tex = new PackTexture(rasteriser, request.path, img);
			
				loadedPackTextures.put(request.path, tex);
			}
			else
			{
				System.err.println("Error: Couldn't load "+request.path);
			}
		}
		
		return tex;
	}
	
	private BufferedImage loadTexture(String path)
	{
		InputStream in = null;
		
		try
		{
			// Check texture pack and minecraft jar
			ZipStack.ZipStackEntry entry = zipStack.getEntry(path);
			if (entry != null)
			{
				in = entry.getInputStream();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (in == null)
		{
			try
			{
				// Check classpath
				in = getClass().getClassLoader().getResourceAsStream(path);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if (in == null)
		{
			try
			{
				// Check local file system
				in = new FileInputStream(new File(path));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		BufferedImage img = null;
		
		if (in != null)
		{
			try
			{
		
				img = ImageIO.read(in);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					in.close();
				}
				catch (Exception e) {}
			}
		}
		
		return copy( img );
	}
	
	public Texture getVignetteTexture()
	{
		return vignetteTexture;
	}
	
	
	public SubTexture getSubTile(final int tileX, final int tileY)
	{
	//	return tiles.get( new Point(tileX, tileY) );
		return findTexture("terrain["+tileX+", "+tileY+"]");
	}
	
	
	public BufferedImage getItem(final int itemTileX, final int itemTileY)
	{
		// item sheet is a 16x16 grid of images, so figure out actual size in pixels
		
		final int itemWidth = itemSheet.getWidth() / 16;
		final int itemHeight = itemSheet.getHeight() / 16;
		
		final int x = itemTileX * itemWidth;
		final int y = itemTileY * itemHeight;
		return itemSheet.getSubimage(x, y, itemWidth, itemHeight);
	}
	
	public BufferedImage getIcon(final int virtualX, final int virtualY, final int width, final int height)
	{
		Point actual = iconVirtualToActual(virtualX, virtualY);
		Point size = iconVirtualToActual(width, height);
		
		return iconSheet.getSubimage(actual.x, actual.y, size.x, size.y);
	}
	
	public Font getFont()
	{
		return font;
	}
	
	public Color getGrassColour(final int x, final int y)
	{
		final int actualX = x & 0xff;
		final int actualY = y & 0xff;
		
		final int rgb = grassLookupImage.getRGB(actualX, actualY);
		return new Color(rgb);
		/*
		final float xPer = (float)actualX / (float)(grassLookupImage.getWidth()-1);
		final float yPer = (float)actualY / (float)(grassLookupImage.getHeight()-1);
		
		final int red = (int)(xPer * 255);
		final int green = (int)(yPer * 255);
		return new Color(red, green, 0); */
	}
	
	public Color getFoliageColour(final int x, final int y)
	{
		final int actualX = clamp(x, 0, foliageLookupImage.getWidth()-1);
		final int actualY = clamp(y, 0, foliageLookupImage.getHeight()-1);
		
		return new Color( foliageLookupImage.getRGB(actualX, actualY) );
	}
	
	private static int clamp(final int value, final int min, final int max)
	{
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	/** Makes a copy of the input image. Also converts to INT_ARGB so that we're always
	 *  working in the same colour space.
	 */
	public static BufferedImage copy(BufferedImage in)
	{
		if (in == null)
			return null;
		
		BufferedImage img = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		for (int x=0; x<in.getWidth(); x++)
		{
			for (int y=0; y<in.getHeight(); y++)
			{
				img.setRGB(x, y, in.getRGB(x, y));
			}
		}
		
		return img;		
	}	
	
	private Point iconVirtualToActual(final int virtualX, final int virtualY)
	{
		final float normX = (float)virtualX / 256.0f;
		final float normY = (float)virtualY / 256.0f;
		
		final int actualX = Math.round(normX * iconSheet.getWidth());
		final int actualY = Math.round(normY * iconSheet.getHeight());
		
		return new Point(actualX, actualY);
	}
}
