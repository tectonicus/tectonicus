/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import tectonicus.Minecraft;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.renderer.Font;
import tectonicus.texture.ZipStack.ZipStackEntry;

public class TexturePack
{
	private String version;
	
	private Rasteriser rasteriser;
	
	private Texture vignetteTexture;
	
	private BufferedImage itemSheet;
	private BufferedImage iconSheet;
	private BufferedImage chestImage;
	
	private BufferedImage grassLookupImage;
	private BufferedImage foliageLookupImage;
	
	private Font font;
	
	private ZipStack zipStack;
	
	private Map<String, PackTexture> loadedPackTextures;
	
	public TexturePack(Rasteriser rasteriser, File minecraftJar, File texturePack, List<File> modJars)
	{
		if (!minecraftJar.exists())
			throw new RuntimeException("Couldn't find minecraft.jar at "+minecraftJar.getAbsolutePath());
	
		this.rasteriser = rasteriser;
		
		loadedPackTextures = new HashMap<String, PackTexture>();
		
		//this.version = Minecraft.getTexturePackVersion(minecraftJar);
		
		try
		{
			zipStack = new ZipStack(minecraftJar, texturePack, modJars);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't open jar files for texture reading", e);
		}
		
		//ZipStackEntry terrainEntry = zipStack.getEntry("terrain.png");
		//TODO: Clean up this version stuff
		if (zipStack.getEntry("terrain.png") != null)
		{
			this.version = "1.4";
			Minecraft.setMinecraftVersion(1.4f);
		}
		else if(zipStack.getEntry("textures/blocks/activatorRail.png") != null)
		{
			this.version = "1.5";
			Minecraft.setMinecraftVersion(1.5f);
		}
		else if(zipStack.getEntry("assets/minecraft/textures/blocks/redstone_dust_cross.png") != null)
		{
			this.version = "1.678";
			Minecraft.setMinecraftVersion(1.6f);
			if(zipStack.getEntry("assets/minecraft/textures/blocks/stone_andesite.png") != null)
				Minecraft.setMinecraftVersion(1.8f);
		}
		else if(zipStack.getEntry("assets/minecraft/textures/blocks/usb_charger_side.png") != null)
		{
			this.version = "1.RV";
			Minecraft.setMinecraftVersion(1.9f);
		}
		else
		{
			this.version = "1.9+";
			Minecraft.setMinecraftVersion(1.9f);
		}
		
		try
		{
			if (this.version == "1.4")
				findTexture("terrain.png[0, 0]");
			else if (this.version == "1.5")
			{
				// Load each individual texture file?
			}
			
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
			String path;
			if(Minecraft.getMinecraftVersion() >= 1.6f)
				path = "assets/minecraft/textures/";
			else
				path = "";
			
			ZipStackEntry vignetteEntry = zipStack.getEntry(path + "misc/vignette.png");
			if (vignetteEntry != null)
			{
				BufferedImage vignetteImage = copy( ImageIO.read(vignetteEntry.getInputStream()) );
				vignetteTexture = rasteriser.createTexture(vignetteImage, TextureFilter.LINEAR);
			}
			
			//TODO: For MC 1.5, do we need to load each individual item into the TexturePack object?
			if (version == "1.4")
			{
				ZipStackEntry itemsEntry = zipStack.getEntry("gui/items.png");
				if (itemsEntry != null)
				{
					itemSheet = copy( ImageIO.read( itemsEntry.getInputStream() ) );
				}
				else
				{
					throw new RuntimeException("Couldn't find items.png in "+formatPaths(minecraftJar, texturePack));
				}
			}
				
			ZipStackEntry iconsEntry = zipStack.getEntry(path + "gui/icons.png");
			if (iconsEntry != null)
			{
				iconSheet = copy( ImageIO.read( iconsEntry.getInputStream() ) );
			}
			else
			{
				throw new RuntimeException("Couldn't find icons.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry chestEntry = zipStack.getEntry(path + "gui/container/generic_54.png");
			if (chestEntry != null)
			{
				chestImage = copy( ImageIO.read( chestEntry.getInputStream() ) );
			}
			else
			{
				throw new RuntimeException("Couldn't find generic_54.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry grassEntry;
			if (version == "1.4" || version == "1.5")
				grassEntry = zipStack.getEntry(path + "misc/grasscolor.png");
			else
				grassEntry = zipStack.getEntry(path + "colormap/grass.png");
			
			if (grassEntry != null)
			{
				grassLookupImage = copy( ImageIO.read( grassEntry.getInputStream() ) );
			//	ImageIO.write(grassLookupImage, "png", new File("/Users/John/grass.png"));
			}
			else
			{
				throw new RuntimeException("Couldn't find grasscolor.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry foliageEntry;
			if (version == "1.4" || version == "1.5")
				foliageEntry = zipStack.getEntry(path + "misc/foliagecolor.png");
			else
				foliageEntry = zipStack.getEntry(path + "colormap/foliage.png");
			
			if (foliageEntry != null)
			{
				foliageLookupImage = copy( ImageIO.read( foliageEntry.getInputStream() ) );
			}
			else
			{
				throw new RuntimeException("Couldn't find foliagecolor.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			ZipStackEntry fontEntry;
			if (version == "1.4" || version == "1.5")
				fontEntry = zipStack.getEntry(path + "font/default.png");
			else
				fontEntry = zipStack.getEntry(path + "font/ascii.png");
			//ZipStackEntry fontTextEntry = zipStack.getEntry("font.txt");
			InputStream textIn = this.getClass().getResourceAsStream("/font.txt");
			
			if (fontEntry != null && textIn != null)
			{
				BufferedImage fontSheet = ImageIO.read( fontEntry.getInputStream() );
				//InputStream textIn = fontTextEntry.getInputStream();
				
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
	
	// TODO: refactor to remove this? why is it still needed?
	public Texture getTexture()
	{
	//	return terrainTexture;
		/*if (this.version == "1.4")
			return findTexture("terrain").texture;
		else*/
			return null;
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
			result.texturePackVersion = this.version;
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
		
		// Minecraft 1.5
		// texture path could be:
		// file.ext  -assume it's located in textures/blocks/
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
		
		// The name 'terrain' is a synonym for terrain.png
		if (path.equals("terrain") && version == "1.4")  //MC 1.4 texture packs
			path = "terrain.png";
		else if (!path.contains("/") && !path.contains("\\") && version == "1.5") //MC 1.5 texture packs
			path = "textures/blocks/" + path;
		else if (!path.contains("/") && !path.contains("\\") && Minecraft.getMinecraftVersion() >= 1.6f) //MC 1.6+ texture packs
			path = "assets/minecraft/textures/blocks/" + path;
		
		return new TextureRequest(path, params);
	}
	
	private PackTexture findTexture(TextureRequest request)
	{
		PackTexture tex = loadedPackTextures.get(request.path);
		
		if (tex == null)
		{
			BufferedImage img;
			try 
			{
				img = loadTexture(request.path);
				tex = new PackTexture(rasteriser, request.path, img);
				loadedPackTextures.put(request.path, tex);
			} 
			catch (FileNotFoundException e)
			{
				System.err.println("\nThe texture file '" + request.path + "' could not be found.");
			}
		}
		
		return tex;
	}
	
	public SubTexture findTexture(BufferedImage img, String path)
	{
		PackTexture tex = loadedPackTextures.get(path);
		
		if (tex == null)
		{			
			tex = new PackTexture(rasteriser, path, img);
			
			loadedPackTextures.put(path, tex);
		}
		
		return tex.getFullTexture();
	}
	
	public BufferedImage loadTexture(String path) throws FileNotFoundException
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
			in = new FileInputStream(new File(path));
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
	
	public HashMap<String, BufferedImage> loadPatterns()
	{
		HashMap<String, BufferedImage> patterns = new HashMap<String, BufferedImage>();
		HashMap<String, String> codes = new HashMap<String, String>();
		codes.put("banner_base.png", "base");
		codes.put("base.png", "baseMask");
		codes.put("border.png", "bo");
		codes.put("bricks.png", "bri");
		codes.put("circle.png", "mc");
		codes.put("creeper.png", "cre");
		codes.put("cross.png", "cr");
		codes.put("curly_border.png", "cbo");
		codes.put("diagonal_left.png", "ld");
		codes.put("diagonal_right.png", "rd");
		codes.put("diagonal_up_left.png", "lud");
		codes.put("diagonal_up_right.png", "rud");
		codes.put("flower.png", "flo");
		codes.put("gradient.png", "gra");
		codes.put("gradient_up.png", "gru");
		codes.put("half_horizontal.png", "hh");
		codes.put("half_horizontal_bottom.png", "hhb");
		codes.put("half_vertical.png", "vh");
		codes.put("half_vertical_right.png", "vhr");
		codes.put("mojang.png", "moj");
		codes.put("rhombus.png", "mr");
		codes.put("skull.png", "sku");
		codes.put("small_stripes.png", "ss");
		codes.put("square_bottom_left.png", "bl");
		codes.put("square_bottom_right.png", "br");
		codes.put("square_top_left.png", "tl");
		codes.put("square_top_right.png", "tr");
		codes.put("straight_cross.png", "sc");
		codes.put("stripe_bottom.png", "bs");
		codes.put("stripe_center.png", "cs");
		codes.put("stripe_downleft.png", "dls");
		codes.put("stripe_downright.png", "drs");
		codes.put("stripe_left.png", "ls");
		codes.put("stripe_middle.png", "ms");
		codes.put("stripe_right.png", "rs");
		codes.put("stripe_top.png", "ts");
		codes.put("triangle_bottom.png", "bt");
		codes.put("triangle_top.png", "tt");
		codes.put("triangles_bottom.png", "bts");
		codes.put("triangles_top.png", "tts");
		
		
		ZipFile jarFile;
		try 
		{
			jarFile = new ZipFile(Minecraft.getMinecraftJarLocation());
			
			ZipEntry ze = null;
			for (Enumeration<? extends ZipEntry> e = jarFile.entries(); e.hasMoreElements();)
			{
				ze = e.nextElement();
				
				if (ze.getName().contains("assets/minecraft/textures/entity/banner/") || ze.getName().equals("assets/minecraft/textures/entity/banner_base.png"))
				{
					String fileName = Paths.get(ze.getName()).getFileName().toString();					
					patterns.put(codes.get(fileName), loadTexture(ze.getName()));
				}
			}
			System.out.println("Number of patterns loaded: " + patterns.size());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return patterns;
	}
	
	public String getVersion()
	{
		return version;
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
	
	public BufferedImage getItem(String fileName)
	{
		// in MC 1.5 items are already separate images, we load them so we can resize them
		try {
			return loadTexture(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public BufferedImage getIcon(final int virtualX, final int virtualY, final int width, final int height)
	{
		Point actual = iconVirtualToActual(virtualX, virtualY);
		Point size = iconVirtualToActual(width, height);
		
		return iconSheet.getSubimage(actual.x, actual.y, size.x, size.y);
	}
	
	public BufferedImage getChestImage()
	{
		System.out.println("width: "+ chestImage.getWidth());
		System.out.println("height: "+ chestImage.getHeight());
		BufferedImage top = chestImage.getSubimage(0, 0, 176, 71);
		BufferedImage bottom = chestImage.getSubimage(0, 215, 176, 7);
		
		BufferedImage finalImg = new BufferedImage(176, 78, BufferedImage.TYPE_INT_ARGB);
		finalImg.getGraphics().drawImage(top, 0, 0, null);
		finalImg.getGraphics().drawImage(bottom, 0, 71, null);
		return finalImg;
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
