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

import static tectonicus.configuration.ParseUtil.*;

import tectonicus.Minecraft;
import tectonicus.configuration.Configuration.Dimension;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.configuration.Configuration.RenderStyle;

public class CommandLineParser
{

	
	public static MutableConfiguration parseCommandLine(String[] args) throws Exception
	{
		ArgParser parser = new ArgParser(args);
		
		if (parser.isEmpty())
			return null;
		
		MutableConfiguration config = new MutableConfiguration();
		MutableMap map = new MutableMap("Map0");
		MutableLayer layer = new MutableLayer("LayerA", map.getId());
		map.addLayer(layer);
		config.addMap(map);
		
		System.out.println("Parsing old style command line...");
		
		Mode mode = parseMode( parser.getString("mode", "") );
		config.setMode(mode);
		
		RenderStyle renderStyle = parseRenderStyle( parser.getString("renderStyle", "") );
		layer.setRenderStyle(renderStyle);
		
		// If rendering caves then default the light style to cave style
		if (renderStyle == RenderStyle.Cave)
			layer.setLightStyle(LightStyle.Cave);
		
		// Find the world dir
		File worldDir;
		if (parser.hasValue("worldDir"))
		{
			String path = parser.get("worldDir");
			if (path.equals("1") || path.equals("2") || path.equals("3") || path.equals("4") || path.equals("5"))
			{
				worldDir = new File(Minecraft.findMinecraftDir(), "saves/World"+path);
			}
			else
			{
				worldDir = new File(path);
			}
		}
		else
		{
			worldDir = new File(".");
		}
		map.setWorldDir(worldDir);
		
		final int cameraAngle = parseCameraAngle( parser.getString("cameraAngle", "") );
		map.setCameraAngleDeg(cameraAngle);
		
		final int cameraElevation = parseElevationAngle( parser.getString("cameraElevation", "") );
		map.setCameraElevationDeg(cameraElevation);
		
		// Find the dimension to render
		Dimension dimension = parseDimension( parser.getString("dimension", "") );
		map.setDimension(dimension);
		
		RasteriserType rasteriser = parseRasteriserType( parser.getString("rasteriser", "") );
		config.setRasteriserType(rasteriser);
		
		config.setIsVerbose( parseIsVerbose( parser.getString("verbose", "") ) );
		
		File minecraftJar;
		if (parser.hasValue("minecraftJar"))
		{
			minecraftJar = parser.getFile("minecraftJar", new File("."));
		}
		else
		{
			// Not specified, try and guess where it is
			
			minecraftJar = Minecraft.findMinecraftJar();
		}
		config.setMinecraftJar(minecraftJar);
		
		File texturePack = parser.getFile("texturePack", null);
		config.setTexturePack(texturePack);
		
		final int tileSize = parseTileSize( parser.getString("tileSize", "") );
		config.setTileSize( tileSize );
			
		ImageFormat imageFormat = parseImageFormat( parser.getString("imageFormat", "") );
		layer.setImageFormat(imageFormat);
		
		final float imageCompression = parseImageCompression( parser.getString("imageCompressionLevel", "") );
		layer.setImageCompressionLevel(imageCompression);
		
		SignFilter signFilter = parseSignFilter( parser.getString("signs", "") );
		map.setSignFilter(signFilter);
		
		config.setShowPlayerSpawn( parseShowSpawn( parser.getString("showSpawn", "") ) );
		
		PlayerFilterType playerFilterType = parsePlayerFilterType( parser.getString("players", "") );
		File playerFilterFile = parsePlayerFilterFile( parser.getString("playerFilterFile", "") );
		PlayerFilter playerFilter = new PlayerFilter(playerFilterType, playerFilterFile);
		map.setPlayerFilter(playerFilter);
		
		PortalFilterType portalFilterType = parsePortalFilter( parser.getString("portals", "") );
		map.setPortalFilter( new PortalFilter(portalFilterType) );
		
		config.setExtractLwjglNatives( parseExtractLwjglNatives( parser.getString("extractLwjglNatives", "") ) );
		
		config.setEraseOutputDir( parseEraseOutputDir( parser.getString("eraseOutputDir", "") ) );
		
		File outputDir = parseOutputDir( parser.get("outputDir") );
		config.setOutputDir(outputDir);
		
		File logFile = parseLogFile( parser.getString("logFile", "") );
		config.setLogFile(logFile);
		
		config.setUseCache( parseUseCache(parser.getString("useCache", "") ) );
		
		File cacheDir = parseCacheDir(parser.getString("cacheDir", ""), config.outputDir());
		config.setCacheDir(cacheDir);
		
		
		String htmlName = parseOutputHtmlName( parser.getString("outputHtmlName", "") );
		config.setOutputHtmlName(htmlName);
		
		LightStyle lightStyle = parseLightStyle( parser.getString("lighting", "") );
		if (lightStyle != null)
			layer.setLightStyle(lightStyle);
		
		config.setColourDepth( parseColourDepth( parser.getString("colourDepth", "") ) );
		config.setAlphaBits( parseAlphaBits( parser.getString("alphaBits", "") ) );
		config.setNumSamples( parseNumSamples( parser.getString("numSamples", "") ) );
		
		config.setNumZoomLevels( parseNumZoomLevels( parser.getString("numZoomLevels", "") ) );
		
		map.setClosestZoomSize( parseClosestZoomSize( parser.getString("closestZoomSize", "") ) );
		
		map.setNorthDirection( parseNorthDirection( parser.getString("north", "")));
		map.setCustomCompassRose( parser.getString("compassRose", null));
		
		config.setMaxTiles( parseMaxTiles( parser.getString("maxTiles", "") ) );
		
		config.setForceLoadAwt( parseForceLoadAwt( parser.getString("forceLoadAwt", "") ) );
		config.setForce32BitNatives( parseForce32BitNatives( parser.getString("force32BitNatives", "") ) );
		config.setForce64BitNatives( parseForce64BitNatives( parser.getString("force64BitNatives", "") ) );
		
		map.setUseBiomeColours( parseUseBiomeColours( parser.getString("useBiomeColours", "") ) );
		
		config.setSignsInitiallyVisible( parseInitiallyVisible( parser.getString("signsInitiallyVisible", "") ) );
		config.setPlayersInitiallyVisible( parseInitiallyVisible( parser.getString("playersInitiallyVisible", "") ) );
		config.setPortalsInitiallyVisible( parseInitiallyVisible( parser.getString("portalsInitiallyVisible", "") ) );
		config.setBedsInitiallyVisible( parseInitiallyVisible( parser.getString("bedsInitiallyVisible", "") ) );
		config.setSpawnInitiallyVisible( parseInitiallyVisible( parser.getString("spawnInitiallyVisible", "") ) );
		
		final int numDownsampleThreads = parseNumDownsampleThreads( parser.getString("numDownsampleThreads", "") );
		config.setNumDownsampleThreads(numDownsampleThreads);
		
		return config;
	}
}
