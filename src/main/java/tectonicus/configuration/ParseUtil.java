/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import tectonicus.configuration.Configuration.Dimension;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.configuration.Configuration.RenderStyle;

public class ParseUtil
{
	public static Mode parseMode(String modeStr)
	{
		modeStr = modeStr.toLowerCase();
		
		Mode mode = Mode.CMD;
		if (modeStr.equals("interactive"))
			mode = Mode.INTERACTIVE;
		else if (modeStr.equals("gui"))
			mode = Mode.GUI;
		else if (modeStr.equals("cmd"))
			mode = Mode.CMD;
		else if (modeStr.equals("players"))
			mode = Mode.PLAYERS;
		else if (modeStr.equals("views"))
			mode = Mode.VIEWS;
		else if (modeStr.equals("profile"))
			mode = Mode.PROFILE;
		
		return mode;
	}
	
	public static RenderStyle parseRenderStyle(String renderStyleStr)
	{
		renderStyleStr = renderStyleStr.toLowerCase();
		
		RenderStyle renderStyle = RenderStyle.REGULAR;
		
		if (renderStyleStr.equalsIgnoreCase("regular"))
			renderStyle = RenderStyle.REGULAR;
		else if (renderStyleStr.equalsIgnoreCase("cave"))
			renderStyle = RenderStyle.CAVE;
		else if (renderStyleStr.equalsIgnoreCase("exploredCaves"))
			renderStyle = RenderStyle.EXPLORED_CAVES;
		else if (renderStyleStr.equalsIgnoreCase("nether"))
			renderStyle = RenderStyle.NETHER;
		
		return renderStyle;
	}
	
	public static Dimension parseDimension(String dimensionStr)
	{
		dimensionStr = dimensionStr.toLowerCase();
		
		Dimension dimension = Dimension.OVERWORLD;
		
		if (dimensionStr.equalsIgnoreCase("terra"))
			dimension = Dimension.OVERWORLD;
		else if (dimensionStr.equalsIgnoreCase("nether"))
			dimension = Dimension.NETHER;
		else if (dimensionStr.equalsIgnoreCase("ender") || dimensionStr.equals("end"))
			dimension = Dimension.END;
		
		return dimension;
	}
	
	public static RasteriserType parseRasteriserType(String rasteriserStr)
	{
		rasteriserStr = rasteriserStr.toLowerCase();
		
		RasteriserType rasteriser = RasteriserType.LWJGL;
		
		if (rasteriserStr.equals("lwjgl"))
			rasteriser = RasteriserType.LWJGL;
		else if (rasteriserStr.equals("processing"))
			rasteriser = RasteriserType.PROCESSING;
		
		return rasteriser;
	}
	
	public static ImageFormat parseImageFormat(String imageStr)
	{
		imageStr = imageStr.toLowerCase();
		
		ImageFormat imageFormat = ImageFormat.Png;
		
		if (imageStr.equalsIgnoreCase("jpg") || imageStr.equalsIgnoreCase("jpeg"))
			imageFormat = ImageFormat.Jpg;
		else if (imageStr.equalsIgnoreCase("gif"))
			imageFormat = ImageFormat.Gif;
		else if (imageStr.equalsIgnoreCase("png"))
			imageFormat = ImageFormat.Png;
		
		return imageFormat;
	}
	
	public static float parseImageCompression(String compressionStr)
	{
		try
		{
			final float imageCompression = Float.parseFloat(compressionStr);
			
			if (imageCompression > 1.0f)
				return 1.0f;
			if (imageCompression < 0.1f)
				return 0.1f;
			
			return imageCompression;
		}
		catch (Exception e) {}
		
		return 0.95f;
	}
	
	public static String parseCustomBlockConfig(String blockConfig)
	{
		return blockConfig;
	}
	
	public static boolean parseUseDefaultBlockConfig(String useDefaults)
	{
		if (useDefaults == null || useDefaults.equals(""))
			return true;
		
		return useDefaults.equalsIgnoreCase("true");
	}
	
	public static int parseDrawDistance(String distanceStr)
	{
		try
		{
			final int drawDistance = Integer.parseInt(distanceStr);
			
			if (drawDistance < 10)
				return 10;
			
			if (drawDistance > 1000)
				return 1000;
			
			return drawDistance;
		}
		catch (Exception e) {}
		
		return 200;
	}
	
	public static int parseFOV(String fovStr)
	{
		try
		{
			final int fov = Integer.parseInt(fovStr);
			
			if (fov < 30)
				return 30;
			
			if (fov > 110)
				return 110;
			
			return fov;
		}
		catch (Exception e) {}
		
		return 70;
	}
	
	public static SignFilter parseSignFilter(String filterStr)
	{
		filterStr = filterStr.toLowerCase();
		
		SignFilter filter = SignFilter.Special;
		
		if (filterStr.equalsIgnoreCase("none")
			|| filterStr.equalsIgnoreCase("off"))
		{
			filter = SignFilter.None;
		}
		else if (filterStr.equalsIgnoreCase("special"))
		{
			filter = SignFilter.Special;
		}
		else if (filterStr.equalsIgnoreCase("all"))
		{
			filter = SignFilter.All;
		}
		else if (filterStr.equalsIgnoreCase("obey"))
		{
			filter = SignFilter.Obey;
		}
		
		return filter;
	}
	
	public static ViewFilterType parseViewFilter(String filterStr)
	{
		filterStr = filterStr.toLowerCase();
		
		ViewFilterType type = ViewFilterType.All;
		
		if (filterStr.equalsIgnoreCase("all"))
		{
			type = ViewFilterType.All;
		}
		else if (filterStr.equalsIgnoreCase("none"))
		{
			type = ViewFilterType.None;
		}
		
		return type;
	}
	
	public static PlayerFilterType parsePlayerFilterType(String playerStr)
	{
		playerStr = playerStr.toLowerCase();
		
		PlayerFilterType playerFilterType = PlayerFilterType.All;
		
		if (playerStr.equalsIgnoreCase("none"))
		{
			playerFilterType = PlayerFilterType.None;
		}
		else if (playerStr.equalsIgnoreCase("ops"))
		{
			playerFilterType = PlayerFilterType.Ops;
		}
		else if (playerStr.equalsIgnoreCase("all"))
		{
			playerFilterType = PlayerFilterType.All;
		}
		else if (playerStr.equalsIgnoreCase("whitelist"))
		{
			playerFilterType = PlayerFilterType.Whitelist;
		}
		else if (playerStr.equalsIgnoreCase("blacklist"))
		{
			playerFilterType = PlayerFilterType.Blacklist;
		}
		
		return playerFilterType;
	}
	
	public static Path parsePlayerFilterFile(String fileStr)
	{
		return StringUtils.isNotEmpty(fileStr) ? Paths.get(fileStr) : Paths.get(".");
	}
	
	public static PortalFilterType parsePortalFilter(String portalFilterStr)
	{
		portalFilterStr = portalFilterStr.toLowerCase();
		
		PortalFilterType portalFilterType = PortalFilterType.All;
		if (portalFilterStr.equalsIgnoreCase("none"))
			portalFilterType = PortalFilterType.None;
		
		return portalFilterType;
	}
	
	public static ChestFilterType parseChestFilter(String chestFilterStr)
	{
		chestFilterStr = chestFilterStr.toLowerCase();
		
		ChestFilterType chestFilterType = ChestFilterType.All;
		if (chestFilterStr.equalsIgnoreCase("none"))
			chestFilterType = ChestFilterType.None;
		else if (chestFilterStr.equalsIgnoreCase("player"))
			chestFilterType = ChestFilterType.Player;
		
		return chestFilterType;
	}
	
	/** Caution: can return null */
	public static LightStyle parseLightStyle(String lightingStr)
	{
		lightingStr = lightingStr.toLowerCase();
		
		LightStyle lightStyle = null;
		
		if (lightingStr.equalsIgnoreCase("day"))
			lightStyle = LightStyle.Day;
		else if (lightingStr.equalsIgnoreCase("night"))
			lightStyle = LightStyle.Night;
		else if (lightingStr.equalsIgnoreCase("cave"))
			lightStyle = LightStyle.Cave;
		else if (lightingStr.equalsIgnoreCase("none"))
			lightStyle = LightStyle.None;
		
		return lightStyle;
	}
	
	public static int parseTileSize(String tileSizeStr)
	{
		try
		{
			final int tileSize = Integer.parseInt(tileSizeStr);
			if (tileSize < 64)
				return 64;	// minimum
			if (tileSize > 2048)
				return 2048; // maximum
			return tileSize;
		}
		catch (Exception e) {}
		
		return 512; // default
	}
	
	public static int parseCameraAngle(String angleStr)
	{
		try
		{
			final int angle = Integer.parseInt(angleStr);
			return angle;
		}
		catch (Exception e) {}
		
		return 45;
	}
	
	public static int parseElevationAngle(String angleStr)
	{
		try
		{
			final int angle = Integer.parseInt(angleStr);
			if (angle > 90)
				return 90;
			if(angle < 10)
				return 10;
			return angle;
		}
		catch (Exception e) {}
		
		return 45;
	}
	
	public static boolean parseIsVerbose(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static String parseSinglePlayerName(String name)
	{
		if (name == null)
			return "";
		
		return name;
	}
	
	public static boolean parseShowSpawn(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseShowPlayers(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseShowBeds(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseShowSigns(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseShowPortals(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseShowViews(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default;
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseEraseOutputDir(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseUseCache(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseUseOldColorPalette(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static File parseOutputDir(String outputDirStr)
	{
		if (outputDirStr == null || outputDirStr.equals(""))
			return new File(".");
		
		return new File(outputDirStr);
	}
	
	public static File parseCacheDir(String cacheDir, File outputDir)
	{
		if (cacheDir == null || cacheDir.equals(""))
			return new File(outputDir, "Cache");
		
		return new File(cacheDir);
	}
	
	public static String parseOutputHtmlName(String name)
	{
		if (name == null || name.equals(""))
			return "map.html";
		
		return name;
	}
	
	public static String parseDefaultSkin(String skin)
	{
		if (skin == null || skin.equals(""))
			return "steve";
		
		return skin;
	}
	
	public static File parseLogFile(String logFileStr)
	{
		if (logFileStr == null || logFileStr.length() == 0)
			return new File("./TectonicusLog.txt");
		
		return new File(logFileStr);
	}
	
	public static int parseNumDownsampleThreads(String numThreadsStr)
	{
		try
		{
			final int numThreads = Integer.parseInt(numThreadsStr);
			if (numThreads >= 1)
				return numThreads;
		}
		catch (Exception e) {}
		
		return Runtime.getRuntime().availableProcessors(); 
	}
	
	public static int parseColourDepth(String depthStr)
	{
		try
		{
			final int colourDepth = Integer.parseInt(depthStr);
			if (colourDepth > 0)
				return colourDepth;
		}
		catch (Exception e) {}
		
		return 32;
	}
	
	public static int parseAlphaBits(String depthStr)
	{
		try
		{
			final int alphaBits = Integer.parseInt(depthStr);
			return alphaBits;
		}
		catch (Exception e) {}
		
		return 8;
	}
	
	public static int parseNumSamples(String depthStr)
	{
		try
		{
			final int samples = Integer.parseInt(depthStr);
			return samples;
		}
		catch (Exception e) {}
		
		return 4;
	}
	
	public static int parseNumZoomLevels(String zoomLevelsStr)
	{
		try
		{
			final int numLevels = Integer.parseInt(zoomLevelsStr);
			if (numLevels < 1)
				return 1;
			return numLevels;
		}
		catch (Exception e) {}
		
		return 8;
	}
	
	public static int parseClosestZoomSize(String sizeStr)
	{
		try
		{
			final int size = Integer.parseInt(sizeStr);
			if (size < 1)
				return 1;
			return size;
		}
		catch (Exception e) {}
		
		return 12;
	}
	
	public static int parseMaxTiles(String maxTilesStr)
	{
		try
		{
			final int maxTiles = Integer.parseInt(maxTilesStr);
			return maxTiles;
		}
		catch (Exception e) {}
		
		return -1;
	}
	
	public static boolean parseUseBiomeColours(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static String parseBackgroundColor (String string, Dimension dimension)
	{
		if (StringUtils.isEmpty(string))
		{
			if (dimension == Dimension.OVERWORLD || dimension == Dimension.NETHER)
				return "#e5e3df";
			else if (dimension == Dimension.END)
				return "#281932";
		}
		
		return string;
	}
	
	public static boolean parseExtractLwjglNatives(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseForceLoadAwt(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseForce32BitNatives(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseForce64BitNatives(String string)
	{
		if (string == null || string.length() == 0)
			return false; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static boolean parseInitiallyVisible(String string)
	{
		if (string == null || string.length() == 0)
			return true; // default
		
		return string.equalsIgnoreCase("true");
	}
	
	public static int parseVersion(String str)
	{
		int version = 0;
		try
		{
			version = Integer.parseInt(str);
		}
		catch (Exception e) {}
		
		return version;
	}
	
	public static NorthDirection parseNorthDirection(String dirStr)
	{
		NorthDirection dir = NorthDirection.MinusZ;
		
		if (dirStr != null)
		{
			dirStr = dirStr.trim().toLowerCase();
			
			if (dirStr.equals("+x"))
			{
				dir = NorthDirection.PlusX;
			}
			else if (dirStr.equals("-x"))
			{
				dir = NorthDirection.MinusX;
			}
			else if (dirStr.equals("+z"))
			{
				dir = NorthDirection.PlusZ;
			}
			else if (dirStr.equals("-z"))
			{
				dir = NorthDirection.MinusZ;
			}
		}
		
		return dir;
	}
}
