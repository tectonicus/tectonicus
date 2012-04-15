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
package tectonicus.configuration;

import java.io.File;

import tectonicus.configuration.Configuration.Dimension;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.configuration.Configuration.RenderStyle;

public class ParseUtil
{
	public static Mode parseMode(String modeStr)
	{
		modeStr = modeStr.toLowerCase();
		
		Mode mode = Mode.CommandLine;
		if (modeStr.equals("interactive"))
			mode = Mode.Interactive;
		else if (modeStr.equals("gui"))
			mode = Mode.Gui;
		else if (modeStr.equals("cmd"))
			mode = Mode.CommandLine;
		else if (modeStr.equals("players"))
			mode = Mode.ExportPlayers;
		else if (modeStr.equals("views"))
			mode = Mode.RenderViews;
		else if (modeStr.equals("profile"))
			mode = Mode.Profile;
		
		return mode;
	}
	
	public static RenderStyle parseRenderStyle(String renderStyleStr)
	{
		renderStyleStr = renderStyleStr.toLowerCase();
		
		RenderStyle renderStyle = RenderStyle.Regular;
		
		if (renderStyleStr.equalsIgnoreCase("regular"))
			renderStyle = RenderStyle.Regular;
		else if (renderStyleStr.equalsIgnoreCase("cave"))
			renderStyle = RenderStyle.Cave;
		else if (renderStyleStr.equalsIgnoreCase("exploredCaves"))
			renderStyle = RenderStyle.ExploredCaves;
		else if (renderStyleStr.equalsIgnoreCase("nether"))
			renderStyle = RenderStyle.Nether;
		
		return renderStyle;
	}
	
	public static Dimension parseDimension(String dimensionStr)
	{
		dimensionStr = dimensionStr.toLowerCase();
		
		Dimension dimension = Dimension.Terra;
		
		if (dimensionStr.equalsIgnoreCase("terra"))
			dimension = Dimension.Terra;
		else if (dimensionStr.equalsIgnoreCase("nether"))
			dimension = Dimension.Nether;
		else if (dimensionStr.equalsIgnoreCase("ender") || dimensionStr.equals("end"))
			dimension = Dimension.Ender;
		
		return dimension;
	}
	
	public static RasteriserType parseRasteriserType(String rasteriserStr)
	{
		rasteriserStr = rasteriserStr.toLowerCase();
		
		RasteriserType rasteriser = RasteriserType.Lwjgl;
		
		if (rasteriserStr.equals("lwjgl"))
			rasteriser = RasteriserType.Lwjgl;
		else if (rasteriserStr.equals("processing"))
			rasteriser = RasteriserType.Processing;
		
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
	
	public static File parsePlayerFilterFile(String fileStr)
	{
		if (fileStr == null || fileStr.length() == 0)
			return new File(".");
		
		return new File(fileStr);
	}
	
	public static PortalFilterType parsePortalFilter(String portalFilterStr)
	{
		portalFilterStr = portalFilterStr.toLowerCase();
		
		PortalFilterType portalFilterType = PortalFilterType.All;
		if (portalFilterStr.equalsIgnoreCase("none"))
			portalFilterType = PortalFilterType.None;
		
		return portalFilterType;
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
		
		return 24;
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
