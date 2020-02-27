/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static tectonicus.configuration.ParseUtil.*;
import tectonicus.Minecraft;
import tectonicus.configuration.Configuration.Dimension;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.CircularWorldSubsetFactory;
import tectonicus.world.subset.FullWorldSubsetFactory;
import tectonicus.world.subset.WorldSubsetFactory;

public class XmlConfigurationParser
{
	private static final int VERSION = 2;
	
	public static MutableConfiguration parseConfiguration(File file) throws Exception
	{
		MutableConfiguration config = new MutableConfiguration();
		
		System.out.println("Parsing config from "+file.getAbsolutePath());
		
		Element root = loadXml(file, "tectonicus");
		if (root == null)
			throw new RuntimeException("Couldn't get root config node from "+file.getAbsolutePath());
		
		final int version = ParseUtil.parseVersion( getString(root, "version"));
		if (version != VERSION)
			throw new RuntimeException("Old or incompatible config file. Please update to the latest format");
		
		// Parse base config
		Element configNode = getChild(root, "config");
		if (configNode != null)
		{
			Mode mode = parseMode( getString(configNode, "mode") );
			config.setMode(mode);
			
			File minecraftJar;
			if (configNode.hasAttribute("minecraftJar"))
			{
				minecraftJar = new File( configNode.getAttribute("minecraftJar") );
			}
			else
			{
				// Not specified, try and guess where it is
				minecraftJar = Minecraft.findMinecraftJar();
			}
			config.setMinecraftJar(minecraftJar);
			
			File texturePack = null;
			if (configNode.hasAttribute("texturePack"))
			{
				texturePack = new File(getString(configNode, "texturePack"));
			}
			config.setTexturePack(texturePack);
			
			boolean useOldColorPalette = false;
			if (configNode.hasAttribute("useOldColorPalette"))
			{
				useOldColorPalette = parseUseOldColorPalette(getString(configNode, "useOldColorPalette"));
			}
			config.setUseOldColorPalette(useOldColorPalette);
			Minecraft.setUseOldColorPalette(useOldColorPalette);
			
			File outputDir = parseOutputDir( getString(configNode, "outputDir") );
			config.setOutputDir(outputDir);
			
			String outputHtml = parseOutputHtmlName( getString(configNode, "outputHtmlName") );
			config.setOutputHtmlName(outputHtml);
			
			String defaultSkin = parseDefaultSkin( getString(configNode, "defaultSkin") );
			config.setDefaultSkin(defaultSkin);
			
			config.setNumZoomLevels( parseNumZoomLevels( getString(configNode, "numZoomLevels") ) );
			
			config.setSinglePlayerName( parseSinglePlayerName( getString(configNode, "singlePlayerName") ) );
			
			final int numDownsampleThreads = parseNumDownsampleThreads( getString(configNode, "numDownsampleThreads") );
			config.setNumDownsampleThreads(numDownsampleThreads);
			
			config.setEraseOutputDir( parseEraseOutputDir( getString(configNode, "eraseOutputDir") ) );
			
			config.setUseCache( parseUseCache( getString(configNode, "useCache") ) );
			
			config.setCacheDir( parseCacheDir( getString(configNode, "cacheDir"), config.outputDir() ) );
			
			config.setLogFile( parseLogFile( getString(configNode, "logFile") ) );
			
			config.setSpawnInitiallyVisible( parseShowSpawn( getString(configNode, "spawnInitiallyVisible")) );
			config.setPlayersInitiallyVisible( parseShowPlayers( getString(configNode, "playersInitiallyVisible")) );
			config.setBedsInitiallyVisible( parseShowBeds( getString(configNode, "bedsInitiallyVisible")) );
			config.setSignsInitiallyVisible( parseShowSigns( getString(configNode, "signsInitiallyVisible")) );
			config.setPortalsInitiallyVisible( parseShowPortals( getString(configNode, "portalsInitiallyVisible")) );
			config.setViewsInitiallyVisible( parseShowViews( getString(configNode, "viewsInitiallyVisible")) );
		}
		
		// Parse rasteriser config
		Element rasteriserNode = getChild(root, "rasteriser");
		if (rasteriserNode != null)
		{
			RasteriserType rasteriser = parseRasteriserType( getString(rasteriserNode, "type") );
			config.setRasteriserType(rasteriser);
			
			config.setColourDepth( parseColourDepth( getString(rasteriserNode, "colourDepth") ) );
			config.setAlphaBits( parseAlphaBits( getString(rasteriserNode, "alphaBits") ) );
			config.setNumSamples( parseNumSamples( getString(rasteriserNode, "numSamples") ) );
			
			final int tileSize = parseTileSize( getString(rasteriserNode, "tileSize") );
			config.setTileSize( tileSize );
		}
		
		int mapNumber = 0;
		char layerNumber = 'A';
		
		Element[] maps = getChildren(root, "map");
		for (Element mapElement : maps)
		{
			MutableMap map = new MutableMap("Map"+mapNumber);
			mapNumber++;
			
			map.setName( getString(mapElement, "name") );
			
			map.setWorldDir( new File(getString(mapElement, "worldDir")) );
			
			map.setClosestZoomSize( parseClosestZoomSize(getString(mapElement, "closestZoomSize")));
			
			final Dimension dimension = parseDimension(getString(mapElement, "dimension"));
			map.setDimension(dimension);
			
			final int cameraAngle = parseCameraAngle( getString(mapElement, "cameraAngle") );
			map.setCameraAngleDeg(cameraAngle);
			
			final int elevationAngle = parseElevationAngle( getString(mapElement, "cameraElevation") );
			map.setCameraElevationDeg(elevationAngle);
			
			Element subsetNode = getChild(mapElement, "subset");
			map.setWorldSubsetFactory( parseWorldSubset(subsetNode) );
			
			final boolean useBiomeColours = parseUseBiomeColours(getString(mapElement, "useBiomeColours"));
			map.setUseBiomeColours(useBiomeColours);
			
			map.setNorthDirection( parseNorthDirection( getString(mapElement, "north")));
			map.setCustomCompassRose( getString(mapElement, "compassRose"));
			
			// Sign filter
			Element signsNode = getChild(mapElement, "signs");
			if (signsNode != null)
			{
				SignFilter signFilter = parseSignFilter( getString(signsNode, "filter") );
				map.setSignFilter(signFilter);
			}
			
			Element viewsNode = getChild(mapElement, "views");
			if (viewsNode != null)
			{
				ViewFilterType viewFilterType = parseViewFilter( getString(viewsNode, "filter"));
				map.setViewFilter( new ViewFilter(viewFilterType) );
				
				MutableViewConfig viewConfig = map.getViewConfig();
				
				ImageFormat imageFormat = parseImageFormat( getString(viewsNode, "imageFormat") );
				viewConfig.setImageFormat(imageFormat);
				
				final float compression = parseImageCompression( getString(viewsNode, "imageCompressionLevel") );
				viewConfig.setImageCompressionLevel(compression);
				
				final int drawDistance = parseDrawDistance( getString(viewsNode, "drawDistance") );
				viewConfig.setViewDistance(drawDistance);
				
				final int fov = parseFOV( getString(viewsNode, "fov") );
				viewConfig.setFOV(fov);
			}
			
			// Player filter
			Element playersNode = getChild(mapElement, "players");
			if (playersNode != null)
			{
				PlayerFilterType playerFilterType = parsePlayerFilterType( getString(playersNode, "filter") );
				Path playerFilterFile = parsePlayerFilterFile( getString(playersNode, "playerFilterFile") );
				
				PlayerFilter playerFilter = new PlayerFilter(playerFilterType, playerFilterFile, map.getWorldDir().toPath());
				map.setPlayerFilter(playerFilter);
			}
			
			// Portal filter
			Element portalsNode = getChild(mapElement, "portals");
			if (portalsNode != null)
			{
				PortalFilterType portalFilterType = parsePortalFilter( getString(portalsNode, "filter") );
				map.setPortalFilter( new PortalFilter(portalFilterType) );
			}
			
			// Chest filter
			Element chestsNode = getChild(mapElement, "chests");
			if (chestsNode != null)
			{
				ChestFilterType chestFilterType = parseChestFilter( getString(chestsNode, "filter") );
				map.setChestFilter( new ChestFilter(chestFilterType) );
			}
			
			Element modsElement = getChild(mapElement, "mods");
			if (modsElement != null)
			{
				List<File> modJars = new ArrayList<File>();
				Element[] mods = getChildren(modsElement, "mod");
				for (Element mod : mods)
				{
					modJars.add(new File(getString(mod, "path")));
				}
				
				map.setModJars(modJars);
			}
			
			Element[] layers = getChildren(mapElement, "layer");
			for (Element layerElement : layers)
			{
				MutableLayer layer = new MutableLayer("Layer"+layerNumber, map.getId());
				layerNumber++;
				
				layer.setName( getString(layerElement, "name") );
				
				LightStyle style = parseLightStyle(getString(layerElement, "lighting") );
				if (style == null)
					style = LightStyle.Day;
				layer.setLightStyle(style);
				
				layer.setRenderStyle( parseRenderStyle(getString(layerElement, "renderStyle")));
				
				layer.setImageFormat( parseImageFormat(getString(layerElement, "imageFormat")));
				layer.setImageCompressionLevel( parseImageCompression(getString(layerElement, "imageCompressionLevel")));
				layer.setBackgroundColor( parseBackgroundColor( getString(layerElement, "backgroundColor"), dimension ) );
				
				layer.setUseDefaultBlockConfig( parseUseDefaultBlockConfig( getString(layerElement, "useDefaultBlocks")));
				
				layer.setCustomBlockConfig( parseCustomBlockConfig(getString(layerElement, "customBlocks")) );
				map.addLayer(layer);
			}
			
			config.addMap(map);
		}
		/*
		Element layerNode = getChild(root, "layer");
		if (layerNode != null)
		{
			Dimension dimension = parseDimension( getString(layerNode, "dimension") );
			config.setDimension(dimension);
			
			RenderStyle renderStyle = parseRenderStyle( getString(layerNode, "renderStyle") );
			config.setRenderStyle(renderStyle);
			
			File dimensionDir;
			if (dimension == Dimension.Terra)
			{
				dimensionDir = config.worldDir();
			}
			else if (dimension == Dimension.Nether)
			{
				dimensionDir = new File(config.worldDir(), "DIM-1");
			}
			else
			{
				dimensionDir = config.worldDir();
			}
			config.setDimensionDir(dimensionDir);
			
			config.setWorldSubsetFactory( new FullWorldSubsetFactory() );
			
			final int cameraAngle = parseCameraAngle( getString(layerNode, "cameraAngle") );
			config.setCameraAngleDeg(cameraAngle);
			
			final int elevationAngle = parseElevationAngle( getString(layerNode, "cameraElevation") );
			config.setCameraElevationDeg(elevationAngle);
			
			ImageFormat imageFormat = parseImageFormat( getString(layerNode, "imageFormat") );
			config.setImageFormat(imageFormat);
			
			final float imageCompression = parseImageCompression( getString(layerNode, "imageCompressionLevel") );
			config.setImageCompressionLevel(imageCompression);
			
			LightStyle lightStyle = parseLightStyle( getString(layerNode, "lighting") );
			if (lightStyle != null)
				config.setLightStyle(lightStyle);
			
			config.setNumZoomLevels( parseNumZoomLevels( getString(layerNode, "numZoomLevels") ) );
			
			config.setClosestZoomSize( parseClosestZoomSize( getString(layerNode, "closestZoomSize") ) );
			
			config.setUseBiomeColours( parseUseBiomeColours( getString(layerNode, "useBiomeColours") ) );
			
			String playerName = getString(layerNode, "singlePlayerName");
			if (playerName != null && playerName.length() > 0)
				config.setSinglePlayerName(playerName);
			
			Element signsNode = getChild(layerNode, "signs");
			if (signsNode != null)
			{
				SignFilter signFilter = parseSignFilter( getString(signsNode, "filter") );
				config.setSignFilter(signFilter);
				
				config.setSignsInitiallyVisible( parseInitiallyVisible( getString(signsNode, "initiallyVisible") ) );
			}
			
			Element playersNode = getChild(layerNode, "players");
			if (playersNode != null)
			{
				PlayerFilterType playerFilterType = parsePlayerFilterType( getString(playersNode, "filter") );
				File playerFilterFile = parsePlayerFilterFile( getString(playersNode, "playerFilterFile") );
				
				PlayerFilter playerFilter = new PlayerFilter(playerFilterType, playerFilterFile);
				config.setPlayerFilter(playerFilter);
				
				config.setPlayersInitiallyVisible( parseInitiallyVisible( getString(playersNode, "initiallyVisible") ) );
			}
			
			Element portalsNode = getChild(layerNode, "portals");
			if (portalsNode != null)
			{
				PortalFilterType portalFilterType = parsePortalFilter( getString(portalsNode, "filter") );
				config.setPortalFilter( new PortalFilter(portalFilterType) );
				
				config.setPortalsInitiallyVisible( parseInitiallyVisible( getString(portalsNode, "initiallyVisible") ) );
			}
			
			Element spawnNode = getChild(layerNode, "spawn");
			if (spawnNode != null)
			{
				config.setShowPlayerSpawn( parseShowSpawn( getString(spawnNode, "showSpawn") ) );
				
				config.setSpawnInitiallyVisible( parseInitiallyVisible( getString(spawnNode, "initiallyVisible") ) );
			}
			
			Element bedsNode = getChild(layerNode, "beds");
			if (bedsNode != null)
			{
				config.setBedsInitiallyVisible( parseInitiallyVisible( getString(bedsNode, "initiallyVisible") ) );	
			}
		}
		*/
		
		Element tweeksNode = getChild(root, "tweeks");
		if (tweeksNode != null)
		{
			config.setForceLoadAwt( parseForceLoadAwt( getString(tweeksNode, "forceLoadAwt")) );
		}
		
		Element debugNode = getChild(root, "debug");
		if (debugNode != null)
		{
			config.setMaxTiles( parseMaxTiles( getString(debugNode, "maxTiles") ) );
		}
		
		return config;
	}
	
	private static Element loadXml(File file, String rootName)
	{
		try
		{
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			Document doc = docBuilder.parse(file);
			NodeList nodeList = doc.getElementsByTagName(rootName);
			Element root = (Element)nodeList.item(0);
			return root;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static Element getChild(Element parent, String name)
	{
		NodeList list = parent.getChildNodes();
		for (int i=0; i<list.getLength(); i++)
		{
			Node n = list.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				if (n.getNodeName().equals(name))
				{
					return (Element)n;
				}
			}
		}
		
		return null;	
	}
	
	public static Element[] getChildren(Element parent, String name)
	{
		ArrayList<Element> result = new ArrayList<Element>();
		
		NodeList list = parent.getChildNodes();
		for (int i=0; i<list.getLength(); i++)
		{
			Node n = list.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				if (n.getNodeName().equals(name))
				{
					result.add( (Element)n );
				}
			}
		}
		
		return result.toArray(new Element[0]);
	}
	
	public static String getString(Element element, String attributeName)
	{
		if (element == null || attributeName == null)
			return "";
		
		return element.getAttribute(attributeName);
	}
	
	private static WorldSubsetFactory parseWorldSubset(Element subsetNode)
	{
		if (subsetNode != null)
		{
			Element circularNode = getChild(subsetNode, "CircularSubset");
			if (circularNode != null)
			{
				long radius = 0;
				try
				{
					radius = Long.parseLong(circularNode.getAttribute("radius"));
				}
				catch (Exception e) {}
				
				Vector3l origin = null;
				if (circularNode.hasAttribute("origin"))
				{
					String originStr = circularNode.getAttribute("origin");
					final int commaPos = originStr.indexOf(',');
					if (commaPos != -1 && commaPos < originStr.length()-1)
					{
						String xStr = originStr.substring(0, commaPos).trim();
						String zStr = originStr.substring(commaPos+1).trim();
						
						try
						{
							final long x = Long.parseLong(xStr);
							final long z = Long.parseLong(zStr);
							origin = new Vector3l(x, 0, z);
						}
						catch (Exception e) {}
					}
				}
				if (radius > 0)
				{
					CircularWorldSubsetFactory factory = new CircularWorldSubsetFactory();
					factory.setOrigin(origin);
					factory.setRadius(radius);
					return factory;
				}
			}
		}
		
		return new FullWorldSubsetFactory();
	}
}
