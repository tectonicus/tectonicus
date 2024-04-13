/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tectonicus.Minecraft;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.CircularWorldSubset;
import tectonicus.world.subset.FullWorldSubset;
import tectonicus.world.subset.WorldSubset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static tectonicus.configuration.ParseUtil.parseAlphaBits;
import static tectonicus.configuration.ParseUtil.parseBackgroundColor;
import static tectonicus.configuration.ParseUtil.parseCacheDir;
import static tectonicus.configuration.ParseUtil.parseCameraAngle;
import static tectonicus.configuration.ParseUtil.parseChestFilter;
import static tectonicus.configuration.ParseUtil.parseClosestZoomSize;
import static tectonicus.configuration.ParseUtil.parseColourDepth;
import static tectonicus.configuration.ParseUtil.parseCustomBlockConfig;
import static tectonicus.configuration.ParseUtil.parseDefaultSkin;
import static tectonicus.configuration.ParseUtil.parseDimension;
import static tectonicus.configuration.ParseUtil.parseDrawDistance;
import static tectonicus.configuration.ParseUtil.parseElevationAngle;
import static tectonicus.configuration.ParseUtil.parseFOV;
import static tectonicus.configuration.ParseUtil.parseHeight;
import static tectonicus.configuration.ParseUtil.parseImageCompression;
import static tectonicus.configuration.ParseUtil.parseImageFormat;
import static tectonicus.configuration.ParseUtil.parseLightStyle;
import static tectonicus.configuration.ParseUtil.parseLogFile;
import static tectonicus.configuration.ParseUtil.parseMaxTiles;
import static tectonicus.configuration.ParseUtil.parseMode;
import static tectonicus.configuration.ParseUtil.parseNorthDirection;
import static tectonicus.configuration.ParseUtil.parseNumDownsampleThreads;
import static tectonicus.configuration.ParseUtil.parseNumSamples;
import static tectonicus.configuration.ParseUtil.parseNumZoomLevels;
import static tectonicus.configuration.ParseUtil.parseOrigin;
import static tectonicus.configuration.ParseUtil.parseOutputDir;
import static tectonicus.configuration.ParseUtil.parsePlayerFilterFile;
import static tectonicus.configuration.ParseUtil.parsePlayerFilterType;
import static tectonicus.configuration.ParseUtil.parsePortalFilter;
import static tectonicus.configuration.ParseUtil.parseRasteriserType;
import static tectonicus.configuration.ParseUtil.parseRenderStyle;
import static tectonicus.configuration.ParseUtil.parseSignFilter;
import static tectonicus.configuration.ParseUtil.parseSinglePlayerName;
import static tectonicus.configuration.ParseUtil.parseTileSize;
import static tectonicus.configuration.ParseUtil.parseUseDefaultBlockConfig;
import static tectonicus.configuration.ParseUtil.parseViewFilter;
import static tectonicus.configuration.ParseUtil.parseWidth;

@Log4j2
@UtilityClass
public class XmlConfigurationParser
{
	private static final int VERSION = 2;
	
	public static MutableConfiguration parseConfiguration(File file) throws Exception
	{
		MutableConfiguration config = new MutableConfiguration();
		
		log.info("Parsing config from {}", file.getAbsolutePath());
		
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
			
			File minecraftJar = null;
			if (configNode.hasAttribute("minecraftJar"))
			{
				minecraftJar = new File( configNode.getAttribute("minecraftJar") );
			}
			config.setMinecraftJar(minecraftJar);
			
			File texturePack = null;
			if (configNode.hasAttribute("texturePack"))
			{
				texturePack = new File(getString(configNode, "texturePack"));
			}
			config.setTexturePack(texturePack);

			config.setUsingProgrammerArt(getBoolean(configNode, "useProgrammerArt", false));

			boolean useOldColorPalette = false;
			if (configNode.hasAttribute("useOldColorPalette"))
			{
				useOldColorPalette = getBoolean(configNode, "useOldColorPalette", false);
			}
			config.setUseOldColorPalette(useOldColorPalette);
			Minecraft.setUseOldColorPalette(useOldColorPalette);
			
			File outputDir = parseOutputDir( getString(configNode, "outputDir") );
			config.setOutputDir(outputDir);

			config.setOutputHtmlName(getString(configNode, "outputHtmlName", "map.html"));
			config.setHtmlTitle(getString(configNode, "htmlTitle", "Tectonicus Map"));
			
			String defaultSkin = parseDefaultSkin( getString(configNode, "defaultSkin") );
			config.setDefaultSkin(defaultSkin);
			
			config.setNumZoomLevels( parseNumZoomLevels( getString(configNode, "numZoomLevels") ) );
			
			config.setSinglePlayerName( parseSinglePlayerName( getString(configNode, "singlePlayerName") ) );
			
			final int numDownsampleThreads = parseNumDownsampleThreads( getString(configNode, "numDownsampleThreads") );
			config.setNumDownsampleThreads(numDownsampleThreads);
			
			config.setEraseOutputDir(getBoolean(configNode, "eraseOutputDir", false));
			
			config.setUseCache(getBoolean(configNode, "useCache", true));
			
			config.setCacheDir( parseCacheDir( getString(configNode, "cacheDir"), config.getOutputDir() ) );
			
			config.setLogFile( parseLogFile( getString(configNode, "logFile") ) );
			String logLevel = getString(configNode, "loggingLevel");
			if (StringUtils.isEmpty(logLevel)) {
				logLevel = getString(configNode, "logLevel");
			}
			config.setLoggingLevel(Level.toLevel(logLevel));

			config.setSpawnInitiallyVisible(getBoolean(configNode, "spawnInitiallyVisible", true));
			config.setPlayersInitiallyVisible(getBoolean(configNode, "playersInitiallyVisible", true));
			config.setBedsInitiallyVisible(getBoolean(configNode, "bedsInitiallyVisible", true));
			config.setRespawnAnchorsInitiallyVisible(getBoolean(configNode, "respawnAnchorsInitiallyVisible", true));
			config.setSignsInitiallyVisible(getBoolean(configNode, "signsInitiallyVisible", true));
			config.setPortalsInitiallyVisible(getBoolean(configNode, "portalsInitiallyVisible", true));
			config.setViewsInitiallyVisible(getBoolean(configNode, "viewsInitiallyVisible", true));
			
			config.setCustomStyle(getString(configNode, "customStyle", null));
			config.setCustomScript(getString(configNode, "customScript", null));
			config.setUseCdn(getString(configNode, "useCdn", ""));
		}
		
		// Parse rasteriser config
		Element rasteriserNode = getChild(root, "rasteriser");
		if (rasteriserNode != null)
		{
			RasteriserType rasteriser = parseRasteriserType(getString(rasteriserNode, "type"));
			config.setRasteriserType(rasteriser);

			config.setColourDepth(parseColourDepth(getString(rasteriserNode, "colourDepth")));
			config.setAlphaBits(parseAlphaBits(getString(rasteriserNode, "alphaBits")));
			config.setNumSamples(parseNumSamples(getString(rasteriserNode, "numSamples")));
			config.setTileSize(parseTileSize(getString(rasteriserNode, "tileSize")));
			config.setUseEGL(getBoolean(rasteriserNode, "useEGL", false));
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

			map.setOrigin(parseOrigin(getString(mapElement, "origin")));
			
			Element subsetNode = getChild(mapElement, "subset");
			map.setWorldSubset(parseWorldSubset(subsetNode));
			
			final boolean useBiomeColours = getBoolean(mapElement, "useBiomeColours", false);
			map.setUseBiomeColours(useBiomeColours);

			map.setSmoothLit(getBoolean(mapElement, "useSmoothLighting", true));
			
			map.setNorthDirection( parseNorthDirection( getString(mapElement, "north")));
			map.setCustomCompassRose( getString(mapElement, "compassRose"));
			
			// Sign filter
			Element signsNode = getChild(mapElement, "signs");
			if (signsNode != null) {
				map.setSignFilter(parseSignFilter(getString(signsNode, "filter")));
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

				final int width = parseWidth( getString(viewsNode, "width") );
				viewConfig.setWidth(width);

				final int height = parseHeight( getString(viewsNode, "height") );
				viewConfig.setHeight(height);
			}
			
			// Player filter
			Element playersNode = getChild(mapElement, "players");
			if (playersNode != null)
			{
				PlayerFilterType playerFilterType = parsePlayerFilterType( getString(playersNode, "filter") );
				Path playerFilterFile = parsePlayerFilterFile( getString(playersNode, "playerFilterFile") );
				boolean showBeds = getBoolean(playersNode, "showBeds", true);
				boolean showRespawnAnchors = getBoolean(playersNode, "showRespawnAnchors", true);
				
				PlayerFilter playerFilter = new PlayerFilter(playerFilterType, playerFilterFile, map.getWorldDir().toPath(), showBeds, showRespawnAnchors);
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
				List<File> modJars = new ArrayList<>();
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
		
		Element tweaksNode = getChild(root, "tweeks"); //Backwards compatibility with misspelled tag
		if (tweaksNode == null) {
			tweaksNode = getChild(root, "tweaks");
		}
		if (tweaksNode != null)
		{
			config.setForceLoadAwt(getBoolean(tweaksNode, "forceLoadAwt", false));
		}
		
		Element debugNode = getChild(root, "debug");
		if (debugNode != null) {
			config.setMaxTiles(parseMaxTiles(getString(debugNode, "maxTiles")));
			config.setVerbose(getBoolean(debugNode, "verbose", false));
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
			return (Element)nodeList.item(0);
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
			if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name))
			{
				return (Element)n;
			}
		}
		
		return null;	
	}
	
	public static Element[] getChildren(Element parent, String name)
	{
		List<Element> result = new ArrayList<>();
		
		NodeList list = parent.getChildNodes();
		for (int i=0; i<list.getLength(); i++)
		{
			Node n = list.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name))
			{
				result.add( (Element)n );
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

	public static String getString(Element element, String attributeName, String defaultValue) {
		if (element == null || attributeName == null) {
			return defaultValue;
		}

		String value = element.getAttribute(attributeName);
		if (value.equals("")) {
			value = defaultValue;
		}

		return value;
	}

	public static boolean getBoolean(Element element, String attributeName, boolean defaultValue) {
		if (element == null || attributeName == null)
			return defaultValue;

		String elementString = element.getAttribute(attributeName);

		if (elementString.isEmpty())
			return defaultValue;

		return elementString.equalsIgnoreCase("true");
	}

	private static WorldSubset parseWorldSubset(Element subsetNode) {
		if (subsetNode != null) {
			Element circularNode = getChild(subsetNode, "CircularSubset");
			if (circularNode != null) {
				long radius = 0;
				try {
					radius = Long.parseLong(circularNode.getAttribute("radius"));
				} catch (NumberFormatException e) {
					log.error("Failed to parse subset radius", e);
				}

				Vector3l origin = null;
				if (circularNode.hasAttribute("origin")) {
					origin = parseOrigin(circularNode.getAttribute("origin"));
				}

				if (radius > 0) {
					return new CircularWorldSubset(origin, radius);
				}
			}
		}

		return new FullWorldSubset();
	}
}
