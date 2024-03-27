/*
 * Copyright (c) 2023 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.configuration.Configuration;
import tectonicus.exceptions.MissingAssetException;
import tectonicus.exceptions.MissingMinecraftJarException;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.raw.Biome;
import tectonicus.raw.Biomes;
import tectonicus.raw.BiomesOld;
import tectonicus.renderer.Font;
import tectonicus.util.Colour4f;
import tectonicus.util.FileUtils;
import tectonicus.util.ImageUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tectonicus.Version.VERSIONS_6_TO_8;
import static tectonicus.Version.VERSIONS_9_TO_11;
import static tectonicus.Version.VERSION_12;
import static tectonicus.Version.VERSION_13;
import static tectonicus.Version.VERSION_14;
import static tectonicus.Version.VERSION_20;
import static tectonicus.Version.VERSION_4;
import static tectonicus.Version.VERSION_5;
import static tectonicus.Version.VERSION_RV;
import static tectonicus.Version.VERSION_UNKNOWN;
import static tectonicus.util.ImageUtils.copy;

@Log4j2
public class TexturePack
{
	private final Version version;
	
	private final Rasteriser rasteriser;
	
	private Texture vignetteTexture;
	
	private BufferedImage itemSheet;
	private final BufferedImage emptyHeart;
	private final BufferedImage halfHeart;
	private final BufferedImage fullHeart;
	private final BufferedImage emptyFood;
	private final BufferedImage halfFood;
	private final BufferedImage fullFood;
	private final BufferedImage emptyAir;
	private final BufferedImage fullAir;
        private final BufferedImage chestImage;
	
	private final BufferedImage grassLookupImage;
	private final BufferedImage foliageLookupImage;
	private final Map<Biomes, Colour4f> grassColors = new EnumMap<>(Biomes.class);
	private final Map<Biomes, Colour4f> foliageColors = new EnumMap<>(Biomes.class);
	private final Map<BiomesOld, Colour4f> grassColorsOld = new EnumMap<>(BiomesOld.class);
	private final Map<BiomesOld, Colour4f> foliageColorsOld = new EnumMap<>(BiomesOld.class);
	
	private final Font font;
	
	private final ZipStack zipStack;
	
	private final Map<String, PackTexture> loadedPackTextures;

	public TexturePack(Rasteriser rasteriser, File minecraftJar, File texturePack, List<File> modJars, Configuration args)
	{
		if (!minecraftJar.exists())
			throw new MissingMinecraftJarException("Couldn't find minecraft.jar at " + minecraftJar.getAbsolutePath());
	
		this.rasteriser = rasteriser;
		
		loadedPackTextures = new HashMap<>();

		try
		{
			int worldVersion = Minecraft.getWorldVersion();
			if (args.isUsingProgrammerArt() && worldVersion >= VERSION_14.getNumVersion()) {
				//TODO: currently we are overwriting the value of texturepack if useProgrammerArt is true, we need to implement an ordered list of texture packs instead
				texturePack = new File(Minecraft.findMinecraftDir(), "assets/objects/e4/e49420da40aa1cac6d85838e28a4b82f429ff1a1");
			}
			zipStack = new ZipStack(minecraftJar, texturePack, modJars);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't open jar files for texture reading", e);
		}

		// pack.mcmeta gives us the resource pack information (some older versions of Minecraft also have pack.mcmeta 1.13-1.16)
		PackMcmeta packMcMeta = new PackMcmeta();
		if (zipStack.hasFile("pack.mcmeta")) {
			try {
				ObjectReader packMcmetaReader = FileUtils.getOBJECT_MAPPER().readerFor(PackMcmeta.class);
				packMcMeta = packMcmetaReader.readValue(zipStack.getStream("pack.mcmeta"));
			} catch (IOException e) {
				log.error("Failed to read pack.mcmeta file.", e);
			}
		}

		// version.json gives us the Minecraft jar information (added in 1.14)
		VersionJson versionJson = new VersionJson();
		if (zipStack.hasFile("version.json")) {
			try {
				ObjectReader versionJsonReader = FileUtils.getOBJECT_MAPPER().readerFor(VersionJson.class);
				versionJson = versionJsonReader.readValue(zipStack.getStream("version.json"));
			} catch (IOException e) {
				log.error("Failed to read version.json file.", e);
			}
		}

		if (versionJson.getPackVersion() != null) {
                        String name = versionJson.getName();
                        name = name.substring(0, name.lastIndexOf("."));
			version = Version.byName(name);
		} else if (packMcMeta.getPack().getPackVersion() == 4 && zipStack.hasFile("assets/minecraft/textures/block/acacia_door_bottom.png")) {
			version = VERSION_13;
		} else if (zipStack.hasFile("assets/minecraft/textures/blocks/concrete_lime.png")) {
			version = VERSION_12;
		} else if (zipStack.hasFile("assets/minecraft/textures/blocks/bed_head_top.png")) {
			version = VERSIONS_9_TO_11;
		} else if (zipStack.hasFile("assets/minecraft/textures/blocks/usb_charger_side.png")) {
			version = VERSION_RV;
		} else if (packMcMeta.getPack().getPackVersion() == 1 && zipStack.hasFile("assets/minecraft/textures/blocks/redstone_dust_cross.png")) {
			version = VERSIONS_6_TO_8;
		} else if (zipStack.hasFile("textures/blocks/activatorRail.png")) {
			version = VERSION_5;
		} else if (zipStack.hasFile("terrain.png")) {
			version = VERSION_4;
		} else {
			version = VERSION_UNKNOWN;
		}

		log.debug("Texture pack version: {}", version);
		if (versionJson.getPackVersion() != null) {
			log.debug("Pack version: {}", versionJson.getPackVersion().getResource());
		}

		try
		{
			if (version == VERSION_4)
				findTexture("terrain.png[0, 0]");

			String path;
			if(version.getNumVersion() >= VERSIONS_6_TO_8.getNumVersion())
				path = "assets/minecraft/textures/";
			else
				path = "";
			
			try {
				BufferedImage vignetteImage = copy( ImageIO.read(zipStack.getStream(path + "misc/vignette.png")) );
				vignetteTexture = rasteriser.createTexture(vignetteImage, TextureFilter.LINEAR);
			} catch (Exception e) {
				
			}

			//TODO: try this alternate way of loading these images
//			ZipStackEntry vignetteEntry = zipStack.getEntry(path + "misc/vignette.png");
//			if (vignetteEntry != null)
//			{
//				BufferedImage vignetteImage = copy( ImageIO.read(vignetteEntry.getInputStream()) );
//				vignetteTexture = rasteriser.createTexture(vignetteImage, TextureFilter.LINEAR);
//			}
			
			loadBedTextures();
			loadShulkerTextures();
			loadMissingTexture();

			if (version.getNumVersion() >= VERSION_14.getNumVersion()) {
				loadPaintingTextures();
			}

			if (version == VERSION_4) {
				try {
					itemSheet = copy(ImageIO.read(zipStack.getStream(path + "gui/items.png")));
				} catch (IllegalArgumentException e) {
					log.warn("Could not find items.png.  This is only required if using a Minecraft 1.4 or older jar file.");
				}
			}

                        // From version 1.20.2 resource pack version was increased to 18 and there was a following change:
                        // All textures containing multiple sprites in a sheet for GUI have been split into individual sprites under textures/gui/sprites (automated by Slicer tool).
                        if (versionJson.getPackVersion() != null && versionJson.getPackVersion().getResource() >= 18) {
                                emptyHeart = loadTexture(path + "gui/sprites/hud/heart/container.png", minecraftJar, texturePack);
                                halfHeart = loadTexture(path + "gui/sprites/hud/heart/half.png", minecraftJar, texturePack);
                                fullHeart = loadTexture(path + "gui/sprites/hud/heart/full.png", minecraftJar, texturePack);

                                emptyFood = loadTexture(path + "gui/sprites/hud/food_empty.png", minecraftJar, texturePack);
                                halfFood = loadTexture(path + "gui/sprites/hud/food_half.png", minecraftJar, texturePack);
                                fullFood = loadTexture(path + "gui/sprites/hud/food_full.png", minecraftJar, texturePack);

                                emptyAir = loadTexture(path + "gui/sprites/hud/air_bursting.png", minecraftJar, texturePack);
                                fullAir = loadTexture(path + "gui/sprites/hud/air.png", minecraftJar, texturePack);
                        } else {
                                BufferedImage iconSheet = loadTexture(path + "gui/icons.png", minecraftJar, texturePack);
                            
                                emptyHeart = getIcon(iconSheet, 16, 0, 9, 9);
                                halfHeart = getIcon(iconSheet, 61, 0, 9, 9);
                                fullHeart = getIcon(iconSheet, 52, 0, 9, 9);

                                emptyFood = getIcon(iconSheet, 16, 27, 9, 9);
                                halfFood = getIcon(iconSheet, 61, 27, 9, 9);
                                fullFood = getIcon(iconSheet, 52, 27, 9, 9);

                                emptyAir = getIcon(iconSheet, 25, 18, 9, 9);
                                fullAir = getIcon(iconSheet, 16, 18, 9, 9);
                        }                        
			
			try {
				InputStream imgStream = zipStack.getStream(path + "gui/container.png");
				if (imgStream == null)
					imgStream = zipStack.getStream(path + "gui/container/generic_54.png");
				chestImage = copy( ImageIO.read( imgStream ) );
			} catch (IllegalArgumentException e) {
				throw new MissingAssetException("Couldn't find generic_54.png in "+formatPaths(minecraftJar, texturePack));
			}

			try {
				InputStream imgStream = zipStack.getStream(path + "misc/grasscolor.png");
				if (imgStream == null)
					imgStream = zipStack.getStream(path + "colormap/grass.png");
				grassLookupImage = copy( ImageIO.read( imgStream ) );
			} catch (IllegalArgumentException e) {
				throw new MissingAssetException("Couldn't find grasscolor.png in "+formatPaths(minecraftJar, texturePack));
			}
			
			try {
				InputStream imgStream = zipStack.getStream(path + "misc/foliagecolor.png");
				if (imgStream == null)
					imgStream = zipStack.getStream(path + "colormap/foliage.png");
				foliageLookupImage = copy( ImageIO.read( imgStream ) );
			} catch (IllegalArgumentException e) {
				throw new MissingAssetException("Couldn't find foliagecolor.png in "+formatPaths(minecraftJar, texturePack));
			}

			loadBiomeColors();
			
			//TODO: The font stuff needs a lot of work
			try {
				InputStream imgStream = zipStack.getStream(path + "font/default.png");
				if (imgStream == null)
					imgStream = zipStack.getStream(path + "font/ascii.png");
				InputStream textIn = this.getClass().getResourceAsStream("/font.txt");
				BufferedImage fontSheet = ImageIO.read( imgStream );
				font = new Font(rasteriser, fontSheet, textIn);
			} catch (IllegalArgumentException e) {
				throw new MissingAssetException("Couldn't find font resources in "+formatPaths(minecraftJar, texturePack));
			}
		}
		catch (Exception e)
		{
			throw new MissingAssetException("Couldn't load textures from "+formatPaths(minecraftJar, texturePack), e);
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

	public PackTexture getTexture(String path) {
		return loadedPackTextures.get(path);
	}

	public SubTexture findTextureOrDefault(String texturePath, SubTexture defaultTexture)
	{
		SubTexture result;

		TextureRequest request = parseRequest(texturePath);

		PackTexture tex = findTexture(request, false); // find existing PackTexture or load

		if (tex != null) {
			result = tex.find(request, version); // find existing SubTexture or load
			assert (result != null);
		} else {
			result = defaultTexture;
		}

		return result;
	}
        
        public SubTexture findPalettedTexture(String texturePath, String palettePath, String keyPalettePath) {
		if (texturePath == null || palettePath == null || keyPalettePath == null)
			return null;

		TextureRequest textureRequest = parseRequest(texturePath);
		TextureRequest paletteRequest = parseRequest(palettePath);
		TextureRequest keyPaletteRequest = parseRequest(keyPalettePath);

		PackTexture texture = findTexture(textureRequest, true); // find existing PackTexture or load
		PackTexture palette = findTexture(paletteRequest, true); // find existing PackTexture or load
		PackTexture keyPalette = findTexture(keyPaletteRequest, true); // find existing PackTexture or load
                
                if (texture != null && palette != null && keyPalette != null) {
                        PackTexture palettedTexture = applyPalette(texture, palette, keyPalette); // find existing PackTexture or apply palette and load
                        return palettedTexture.getFullTexture();
                }

		return null;
        }

	public SubTexture findTexture(String texturePath) {
		if (texturePath == null)
			return null;

		SubTexture result = null;

		TextureRequest request = parseRequest(texturePath);

		PackTexture tex = findTexture(request, true); // find existing PackTexture or load

		if (tex != null) {
			result = tex.find(request, version); // find existing SubTexture or load
			assert (result != null);
		}
		
		return result;
	}

	// Used by new rendering system
	public SubTexture getSubTexture(String texturePath) {
		if (texturePath == null)
			return null;

		SubTexture result = null;

		TextureRequest request = new TextureRequest("assets/minecraft/textures/" + texturePath, "");

		PackTexture tex = findTexture(request, true); // find existing PackTexture or load

		if (tex != null) {
			result = tex.find(request, version); // find existing SubTexture or load
			assert (result != null);
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

		return new TextureRequest(getTexturePathPrefix(path), params);
	}

	public String getTexturePathPrefix(String path) {
		if (path.contains("/") || path.contains("\\")) {
			return path;
		}

		String pathPrefix = "";
		if (path.equals("terrain") && version == VERSION_4) {  //MC 1.4 (or older) texture packs
			pathPrefix = "terrain.png";
		} else if (version == VERSION_5) { //MC 1.5 texture packs
			pathPrefix = "textures/blocks/" + path;
		}  else if ((version == VERSIONS_6_TO_8 || version == VERSIONS_9_TO_11 || version == VERSION_12)) { //MC 1.6-1.12 texture packs
			pathPrefix = "assets/minecraft/textures/blocks/" + path;
		} else if (version.getNumVersion() >= VERSION_13.getNumVersion()) { //MC 1.13+ texture packs
			pathPrefix = "assets/minecraft/textures/block/" + path;
		}

		return pathPrefix;
	}
        
        private PackTexture applyPalette(PackTexture texture, PackTexture palette, PackTexture keyPalette) {
                String path = texture.getPath()+palette.getPath()+keyPalette.getPath();
                PackTexture tex = loadedPackTextures.get(path);
                
                if (tex == null) {
                        BufferedImage textureImage = ImageUtils.copy(texture.getImage());
                        BufferedImage paletteImage = palette.getImage();
                        BufferedImage keyPaletteImage = keyPalette.getImage();
                        
                        Map<Integer, Integer> paletteMap = new HashMap<>();
                        for (int x=0; x<paletteImage.getWidth(); x++) {
                                int keyColour = keyPaletteImage.getRGB(x, 0);
                                int palettedColour = paletteImage.getRGB(x, 0);
                                paletteMap.put(keyColour, palettedColour);
                        }
                        
                        for (int x=0; x<textureImage.getWidth(); x++)
                        {
                                for (int y=0; y<textureImage.getHeight(); y++)
                                {
                                        int colour = textureImage.getRGB(x, y);
                                        if (colour != 0) {
                                                colour = paletteMap.get(colour);
                                                textureImage.setRGB(x, y, colour);
                                        }
                                }
                        }
                                            
                        tex = new PackTexture(rasteriser, path, textureImage);
                        loadedPackTextures.put(path, tex);
                }
                
                return tex;
        }
	
	private PackTexture findTexture(TextureRequest request, boolean logMissingTextures) {
		PackTexture tex = loadedPackTextures.get(request.path);
		
		if (tex == null) {
			try {
				IIOImage image = loadImage(request.path);
				if (image != null) {
					boolean convertedTransparency = false;
					BufferedImage bufferedImage = (BufferedImage) image.getRenderedImage();
					Color transparentColor = getTransparentColor(image);
					if (transparentColor != null) {
						bufferedImage = ImageUtils.convertSimpleTransparencyToARGB(bufferedImage, transparentColor);
						convertedTransparency = true;
					}

					if (convertedTransparency) { // Already in ARGB format so no need to copy image
						tex = new PackTexture(rasteriser, request.path, bufferedImage, true, false);
						log.trace(request.path + " contains transparency");
					} else {
						BufferedImage argbImage = copy(bufferedImage);
						ImageUtils.normalizeAlpha(argbImage); // We need to do this to handle some resource pack textures
						ImageUtils.Opacity opacity = ImageUtils.testOpacity(argbImage);
						if (opacity == ImageUtils.Opacity.TRANSPARENT) {
							tex = new PackTexture(rasteriser, request.path, argbImage, true, false);
							log.trace(request.path + " contains transparency");
						} else if (opacity == ImageUtils.Opacity.TRANSLUCENT) {
							tex = new PackTexture(rasteriser, request.path, argbImage, false, true);
							log.trace(request.path + " contains translucency");
						} else {
							tex = new PackTexture(rasteriser, request.path, argbImage);
						}
					}
					loadedPackTextures.put(request.path, tex);
				}
			} catch (FileNotFoundException e) {
                                if (logMissingTextures) {
                                        log.warn("\nThe texture file '" + request.path + "' could not be found.");
                                }
			}
		}
		
		return tex;
	}
	
	public SubTexture findTexture(BufferedImage img, String path) {
		return loadedPackTextures.computeIfAbsent(path, p -> new PackTexture(rasteriser, p, img)).getFullTexture();
	}

	public BufferedImage loadTexture(String path) throws FileNotFoundException {
		return copy((BufferedImage) loadImage(path).getRenderedImage());
	}
        
        private BufferedImage loadTexture(String path, File minecraftJar, File texturePack) throws MissingAssetException, IOException {
                try {
                        return copy( ImageIO.read( zipStack.getStream(path) ) );
                } catch (IllegalArgumentException e) {
                        throw new MissingAssetException("Couldn't find "+path+" in "+formatPaths(minecraftJar, texturePack));
                }
        }

	public IIOImage loadImage(String path) throws FileNotFoundException {
		InputStream in = null;

		try { // Check texture pack and minecraft jar
			InputStream stream = zipStack.getStream(path);
			if (stream != null) {
				in = stream;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (in == null) {
			try { // Check classpath
				in = getClass().getClassLoader().getResourceAsStream(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (in == null) { // Check computer
			Path filePath = Paths.get(path);
			if (Files.exists(filePath)) {
				in = new FileInputStream(path);
			} else {
				throw new FileNotFoundException(String.format("File %s not found", path));
			}
		}

		IIOImage image = null;
		try {
			ImageReader imageReader = ImageIO.getImageReadersByFormatName("png").next();
			imageReader.setInput(ImageIO.createImageInputStream(in));
			image = imageReader.readAll(0, imageReader.getDefaultReadParam());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (Exception ignored) {
			}
		}

		return image;
	}

	public boolean fileExists(String path) {
		boolean hasFile = zipStack.hasFile(path); // Check texture pack and minecraft jar

		if (!hasFile) {
			URL u = TexturePack.class.getResource(path); //Check classpath
			if (u != null) {
				hasFile = true;
			}
		}

		if (!hasFile) { // Check computer
			hasFile = Files.exists(Paths.get(path));
		}

		return hasFile;
	}

	public Color getTransparentColor(IIOImage image) {
		Color transparentColor = null;

		//TODO: remove this when we move to Java 11
		/* Because of Java 8 ImageIO png reader limitations we have to check the tRNS chunk to figure out transparency (this is fixed in Java 11 https://bugs.openjdk.java.net/browse/JDK-6788458)
		 * This is necessary because some resource packs like to save their textures in all kinds of png formats including some without alpha channels
		 * See http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html and Harald K's answer here https://stackoverflow.com/questions/31763853/trying-to-read-a-png-and-xuggler */
		NodeList tRNS = ((IIOMetadataNode) image.getMetadata().getAsTree("javax_imageio_png_1.0")).getElementsByTagName("tRNS");
		if (tRNS.getLength() > 0) {
			Node node = tRNS.item(0).getFirstChild();
			String nodeName = node.getNodeName();
			if (nodeName.equals("tRNS_RGB")) {
				NamedNodeMap attributes = node.getAttributes();
				int red = Integer.parseInt(attributes.getNamedItem("red").getNodeValue());
				int green = Integer.parseInt(attributes.getNamedItem("green").getNodeValue());
				int blue = Integer.parseInt(attributes.getNamedItem("blue").getNodeValue());
				transparentColor = new Color(red, green, blue, 255);
			} else if (nodeName.equals("tRNS_Grayscale")) {
				int gray = Integer.parseInt(node.getAttributes().getNamedItem("gray").getNodeValue());
				transparentColor = new Color(gray, gray, gray, 255);
			} // 'tRNS_Palette' is also a possibility but seems to work without any manual changes needed
		}

		return transparentColor;
	}
	
	public Map<String, BufferedImage> loadPatterns()
	{
		Map<String, BufferedImage> patterns = new HashMap<>();
		Map<String, String> codes = new HashMap<>();
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
		codes.put("globe.png", "glb");
		codes.put("piglin.png", "pig");

			
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipStack.getBaseFileName()), null);
			DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("assets/minecraft/textures/entity/banner"));)
		{
			for (Path entry : entries) {
				String fileName = entry.getFileName().toString();
				if (codes.containsKey(fileName)) { //Only add files that are in our codes map
					patterns.put(codes.get(fileName), loadTexture(entry.toString()));
				}
			}
			
			Path basePattern = fs.getPath("assets/minecraft/textures/entity/banner_base.png");
			patterns.put(codes.get(basePattern.getFileName().toString()), loadTexture(basePattern.toString()));
		}
		catch (IOException e)
		{
			log.warn("No banner patterns found. You may be using an older Minecraft jar file");
		}

		return patterns;
	}
	
	private void loadBedTextures()
	{		
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipStack.getBaseFileName()), null);
				DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("assets/minecraft/textures/entity/bed"));)
		{
			for (Path entry : entries)
			{
				String filename = entry.getFileName().toString();
				String color = filename.substring(0, filename.lastIndexOf('.'));
				findTexture(loadTexture(entry.toString()), "bed_"+color);
			}
		}
		catch (IOException e)
		{
			log.error("No bed textures found. You may be using an older Minecraft jar file");
		}
	}

	private void loadPaintingTextures()
	{
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipStack.getBaseFileName()), null);
			 DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("assets/minecraft/textures/painting"));)
		{
			for (Path entry : entries)
			{
				String filename = entry.getFileName().toString();
				String name = filename.substring(0, filename.lastIndexOf('.'));
				findTexture(loadTexture(entry.toString()), name);
			}
		}
		catch (IOException e)
		{
			log.error("No painting textures found. You may be using an older Minecraft jar file");
		}
	}
	
	private void loadShulkerTextures()
	{		
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipStack.getBaseFileName()), null);
				DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("assets/minecraft/textures/entity/shulker")))
		{
			for (Path entry : entries)
			{
				String filename = entry.getFileName().toString();
				if (filename.contains("shulker"))
				{
					String color = filename.substring(0, filename.lastIndexOf('.')).replace("shulker_", "");
					BufferedImage img = loadTexture(entry.toString());
					if (color.equals("shulker")) {
						findTexture(img, color);
					} else {
						findTexture(img, color + "_shulker");
					}
					
					int height = img.getHeight();
					int textureSize = height/4;
					int topTileHeight = (int) (height*(12.0f/64.0f));
					int bottomImgX = (int) (height*(48.0f/64.0f));
					int bottomImgY = (int) (height*(36.0f/64.0f));
					
					//Create side texture
					BufferedImage finalImage = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = finalImage.createGraphics();
					g.drawImage(img.getSubimage(0, textureSize, textureSize, topTileHeight), 0, 0, null);
					g.drawImage(img.getSubimage(bottomImgX, bottomImgY, textureSize, textureSize), 0, 0, null);
					if (color.equals("shulker")) {
						findTexture(finalImage, color + "_side");
					} else {
						findTexture(finalImage, color + "_shulker_side");
					}
				}
			}
		}
		catch (IOException e)
		{
			log.error("No shulker textures found. You may be using an older Minecraft jar file");
		}
	}

	private void loadMissingTexture() {
		BufferedImage outImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)outImg.getGraphics();

		g.setColor(new Color(248, 0, 248)); //purple
		g.fillRect(8, 0, 8, 8);
		g.fillRect(0, 8, 8, 8);
		g.setColor(new Color(0, 0, 0)); //black
		g.fillRect(0, 0, 8, 8);
		g.fillRect(8, 8, 8, 8);

		findTexture(outImg, "missing_texture");
	}

	private void loadBiomeColors() {
		log.info("Loading biome colors");
		for (Biomes biome : Biomes.values()) {
			//These are hardcoded in Minecraft
			if(biome == Biomes.BADLANDS || biome == Biomes.ERODED_BADLANDS || biome == Biomes.WOODED_BADLANDS) {
				grassColors.put(biome, new Colour4f(144, 129, 77));
				foliageColors.put(biome, new Colour4f(158, 129, 77));
				continue;
			} else if(biome == Biomes.SWAMP) {
				foliageColors.put(biome, new Colour4f(106, 112, 57));
				grassColors.put(biome, new Colour4f(106, 112, 57));
				continue;
			}

			Point colorCoords = biome.getColorCoords();
			grassColors.put(biome, new Colour4f(getGrassColour(colorCoords.x, colorCoords.y)));
			foliageColors.put(biome, new Colour4f(getFoliageColour(colorCoords.x, colorCoords.y)));

			if(biome == Biomes.DARK_FOREST) {
				grassColors.replace(biome, new Colour4f((grassColors.get(biome).toInt() & 16711422) + 2634762 >> 1));
			}
		}

		for (BiomesOld biome : BiomesOld.values()) {
			if(biome == BiomesOld.BADLANDS || biome == BiomesOld.ERODED_BADLANDS || biome == BiomesOld.BADLANDS_PLATEAU
					|| biome == BiomesOld.WOODED_BADLANDS_PLATEAU || biome == BiomesOld.MODIFIED_BADLANDS_PLATEAU
					|| biome == BiomesOld.MODIFIED_WOODED_BADLANDS_PLATEAU) {
				grassColorsOld.put(biome, new Colour4f(144, 129, 77));
				foliageColorsOld.put(biome, new Colour4f(158, 129, 77));
				continue;
			} else if(biome == BiomesOld.SWAMP) {
				foliageColorsOld.put(biome, new Colour4f(106, 112, 57));
				grassColorsOld.put(biome, new Colour4f(106, 112, 57));
				continue;
			}

			Point colorCoords = biome.getColorCoords();
			grassColorsOld.put(biome, new Colour4f(getGrassColour(colorCoords.x, colorCoords.y)));
			foliageColorsOld.put(biome, new Colour4f(getFoliageColour(colorCoords.x, colorCoords.y)));

			if(biome == BiomesOld.DARK_FOREST || biome == BiomesOld.DARK_FOREST_HILLS) {
				grassColorsOld.replace(biome, new Colour4f((grassColorsOld.get(biome).toInt() & 16711422) + 2634762 >> 1));
			}
		}
	}

	public Colour4f getGrassColor(Biome biome) {
		if (biome instanceof Biomes) {
			return grassColors.get(biome);
		} else if (biome instanceof BiomesOld) {
			return grassColorsOld.get(biome);
		} else {
			return grassColors.get(Biomes.THE_VOID);
		}
	}

	public Colour4f getFoliageColor(Biome biome) {
		if (biome instanceof Biomes) {
			return foliageColors.get(biome);
		} else if (biome instanceof BiomesOld) {
			return foliageColorsOld.get(biome);
		} else {
			return foliageColors.get(Biomes.THE_VOID);
		}
	}

	public Color getGrassColour(final int x, final int y) {
		final int actualX = x & 0xff;
		final int actualY = y & 0xff;

		return new Color(grassLookupImage.getRGB(actualX, actualY));
	}

	public Color getFoliageColour(final int x, final int y) {
		return new Color(foliageLookupImage.getRGB(x, y));
	}

	public ZipStack getZipStack()
	{
		return zipStack;
	}

	public Version getVersion()
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
	
	private BufferedImage getIcon(final BufferedImage iconSheet, final int virtualX, final int virtualY, final int width, final int height)
	{
		Point actual = iconVirtualToActual(iconSheet, virtualX, virtualY);
		Point size = iconVirtualToActual(iconSheet, width, height);
		
		return iconSheet.getSubimage(actual.x, actual.y, size.x, size.y);
	}
        
        public BufferedImage getEmptyHeartImage() {
                return emptyHeart;
        }

        public BufferedImage getHalfHeartImage() {
                BufferedImage composedHalf = ImageUtils.copy(emptyHeart);
                composedHalf.getGraphics().drawImage(halfHeart, 0, 0, halfHeart.getWidth(), halfHeart.getHeight(), null);
                return composedHalf;
        }

        public BufferedImage getFullHeartImage() {
                BufferedImage composedFull = ImageUtils.copy(emptyHeart);
                composedFull.getGraphics().drawImage(fullHeart, 0, 0, fullHeart.getWidth(), fullHeart.getHeight(), null);
                return composedFull;
        }

        public BufferedImage getEmptyFoodImage() {
                return emptyFood;
        }

        public BufferedImage getHalfFoodImage() {
                BufferedImage composedHalf = ImageUtils.copy(emptyFood);
                composedHalf.getGraphics().drawImage(halfFood, 0, 0, halfFood.getWidth(), halfFood.getHeight(), null);
                return composedHalf;
        }

        public BufferedImage getFullFoodImage() {
                BufferedImage composedFull = ImageUtils.copy(emptyFood);
                composedFull.getGraphics().drawImage(fullFood, 0, 0, fullFood.getWidth(), fullFood.getHeight(), null);
                return composedFull;
        }
        
        public BufferedImage getFullAirImage() {
                return fullAir;
        }

        public BufferedImage getEmptyAirImage() {
                return emptyAir;
        }

        public BufferedImage getChestImage()
	{
		int factor = (int) (chestImage.getWidth() / 256.0f);
		BufferedImage top = chestImage.getSubimage(0, 0, factor*176, factor*71);
		BufferedImage bottom = chestImage.getSubimage(0, factor*215, factor*176, factor*7);
		
		BufferedImage finalImg = new BufferedImage(factor*176, factor*78, BufferedImage.TYPE_INT_ARGB);
		finalImg.getGraphics().drawImage(top, 0, 0, null);
		finalImg.getGraphics().drawImage(bottom, 0, factor*71, null);
		return finalImg;
	}
	
	public Font getFont()
	{
		return font;
	}
	
	private Point iconVirtualToActual(final BufferedImage iconSheet, final int virtualX, final int virtualY)
	{
		final float normX = virtualX / 256.0f;
		final float normY = virtualY / 256.0f;
		
		final int actualX = Math.round(normX * iconSheet.getWidth());
		final int actualY = Math.round(normY * iconSheet.getHeight());
		
		return new Point(actualX, actualY);
	}
}
