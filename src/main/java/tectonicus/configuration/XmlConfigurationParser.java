/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import ch.qos.logback.classic.Level;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tectonicus.Minecraft;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.configuration.filter.BeaconFilter;
import tectonicus.configuration.filter.BeaconFilterType;
import tectonicus.configuration.filter.ChestFilter;
import tectonicus.configuration.filter.ChestFilterType;
import tectonicus.configuration.filter.PlayerFilter;
import tectonicus.configuration.filter.PlayerFilterType;
import tectonicus.configuration.filter.PortalFilter;
import tectonicus.configuration.filter.PortalFilterType;
import tectonicus.configuration.filter.ViewFilter;
import tectonicus.configuration.filter.ViewFilterType;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.CircularWorldSubset;
import tectonicus.world.subset.FullWorldSubset;
import tectonicus.world.subset.RectangularWorldSubset;
import tectonicus.world.subset.WorldSubset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static tectonicus.configuration.ParseUtil.parseAlphaBits;
import static tectonicus.configuration.ParseUtil.parseBackgroundColor;
import static tectonicus.configuration.ParseUtil.parseBeaconFilter;
import static tectonicus.configuration.ParseUtil.parseBiomeBlendRadius;
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
import static tectonicus.configuration.ParseUtil.parseRenderStyle;
import static tectonicus.configuration.ParseUtil.parseSignFilter;
import static tectonicus.configuration.ParseUtil.parseSinglePlayerName;
import static tectonicus.configuration.ParseUtil.parseTileSize;
import static tectonicus.configuration.ParseUtil.parseUseDefaultBlockConfig;
import static tectonicus.configuration.ParseUtil.parseViewFilter;
import static tectonicus.configuration.ParseUtil.parseWidth;

@Slf4j
@UtilityClass
public class XmlConfigurationParser
{
	private static final int VERSION = 2;
	
	public static Configuration parseConfiguration(File file) throws Exception
	{
		Configuration config = new Configuration();
		
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
			
			String logLevel = getString(configNode, "loggingLevel");
			if (StringUtils.isEmpty(logLevel)) {
				logLevel = getString(configNode, "logLevel");
			}
			config.setLoggingLevel(Level.toLevel(logLevel));

			config.setSpawnInitiallyVisible(getBoolean(configNode, "spawnInitiallyVisible", true));
			config.setPlayersInitiallyVisible(getBoolean(configNode, "playersInitiallyVisible", true));
			config.setShowPlayerInventories(getBoolean(configNode, "showPlayerInventories", false));
			config.setBedsInitiallyVisible(getBoolean(configNode, "bedsInitiallyVisible", true));
			config.setRespawnAnchorsInitiallyVisible(getBoolean(configNode, "respawnAnchorsInitiallyVisible", true));
			config.setSignsInitiallyVisible(getBoolean(configNode, "signsInitiallyVisible", true));
			config.setPortalsInitiallyVisible(getBoolean(configNode, "portalsInitiallyVisible", true));
			config.setViewsInitiallyVisible(getBoolean(configNode, "viewsInitiallyVisible", true));
			config.setChestsInitiallyVisible(getBoolean(configNode, "chestsInitiallyVisible", false));
			config.setBeaconsInitiallyVisible(getBoolean(configNode, "beaconsInitiallyVisible", true));
			
			config.setCustomStyle(getString(configNode, "customStyle", null));
			config.setCustomScript(getString(configNode, "customScript", null));
			config.setUseCdn(getString(configNode, "useCdn", ""));
		}
		
		// Parse rasteriser config
		Element rasteriserNode = getChild(root, "rasteriser");
		if (rasteriserNode != null) {
			config.setRasteriserType(RasteriserType.LWJGL);
			
			config.setColourDepth(parseColourDepth(getString(rasteriserNode, "colourDepth")));
			config.setAlphaBits(parseAlphaBits(getString(rasteriserNode, "alphaBits")));
			config.setNumSamples(parseNumSamples(getString(rasteriserNode, "numSamples")));
			config.setTileSize(parseTileSize(getString(rasteriserNode, "tileSize")));
			config.setUseEGL(getBoolean(rasteriserNode, "useEGL", false));
		}
		
		int legacyLayerNumber = 0;
		List<DimensionEntry> dimensions = normalizeDimensions(root, configNode, config);
		for (DimensionEntry entry : dimensions) {
			Element mapElement = entry.element();
			DimensionConfig map = new DimensionConfig(entry.id());
			map.setNumZoomLevels(parseNumZoomLevels(getString(mapElement, "numZoomLevels")));
			map.setMarkerVisibility(parseMarkerVisibility(mapElement));
			map.setShowPlayerInventories(getBoolean(mapElement, "showPlayerInventories", false));
			
			map.setWorldDir( new File(getString(mapElement, "worldDir")) );
			
			map.setClosestZoomSize( parseClosestZoomSize(getString(mapElement, "closestZoomSize")));
			
			DimensionInfo dimensionInfo = parseDimension(getString(mapElement, "id", getString(mapElement, "dimension")));
			map.setDimension(dimensionInfo.dimension());
			map.setDimensionInfo(dimensionInfo);
			String name = getString(mapElement, "name");
			map.setName(name.isBlank() ? dimensionInfo.dimension().getName() : name);
			
			final int cameraAngle = parseCameraAngle( getString(mapElement, "cameraAngle") );
			map.setCameraAngleDeg(cameraAngle);
			
			final int elevationAngle = parseElevationAngle( getString(mapElement, "cameraElevation") );
			map.setCameraElevationDeg(elevationAngle);

			map.setOrigin(parseOrigin(getString(mapElement, "origin")));
			
			Element subsetNode = getChild(mapElement, "subset");
			map.setWorldSubset(parseWorldSubset(subsetNode));
			
			final boolean useBiomeColours = getBoolean(mapElement, "useBiomeColours", false);
			map.setUseBiomeColours(useBiomeColours);

			parseRenderOptions(mapElement, map);
			
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
				
				ViewConfig viewConfig = map.getViewConfig();
				
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
			if (chestsNode != null) {
				ChestFilterType chestFilterType = parseChestFilter(getString(chestsNode, "filter"));
				map.setChestFilter(new ChestFilter(chestFilterType));
			}
			
			// Beacon filter
			Element beaconsNode = getChild(mapElement, "beacons");
			if (beaconsNode != null) {
				BeaconFilterType beaconFilterType = parseBeaconFilter(getString(beaconsNode, "filter"));
				map.setBeaconFilter(new BeaconFilter(beaconFilterType));
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
			for (Element ownLayer : layers)
			{
				Element layerElement = inherit(mapElement, ownLayer);
				String layerId = entry.legacy() ? "Layer" + layerSuffix(legacyLayerNumber++)
						: entry.world().getId() + ".Layer" + (entry.world().getDimensions().stream().mapToInt(DimensionConfig::numLayers).sum() + map.numLayers());
				LayerConfig layer = new LayerConfig(layerId, map.getId());
				parseRenderOptions(layerElement, layer);
				
				
				LightStyle style = parseLightStyle(getString(layerElement, "lighting") );
				if (style == null)
					style = LightStyle.Day;
				layer.setLightStyle(style);
				String layerName = getString(layerElement, "name");
				layer.setName(layerName.isBlank()
						? layerId.substring(layerId.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT) : layerName);
				
				layer.setRenderStyle( parseRenderStyle(getString(layerElement, "renderStyle")));
				
				layer.setImageFormat( parseImageFormat(getString(layerElement, "imageFormat")));
				layer.setImageCompressionLevel( parseImageCompression(getString(layerElement, "imageCompressionLevel")));
				layer.setBackgroundColor( parseBackgroundColor( getString(layerElement, "backgroundColor"), dimensionInfo.dimension() ) );
				
				layer.setUseDefaultBlockConfig( parseUseDefaultBlockConfig( getString(layerElement, "useDefaultBlocks")));
				
				layer.setCustomBlockConfig( parseCustomBlockConfig(getString(layerElement, "customBlocks")) );
				map.addLayer(layer);
			}
			
			if (entry.world() == null)
				config.addDimension(map);
			else
				entry.world().addDimension(map);
		}
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
	

	private record DimensionEntry(Element element, WorldConfig world, String id, boolean legacy) {}

	private static final List<String> RENDER_ATTRIBUTES = List.of("useSmoothLighting", "useOldGrayscaleLighting",
			"sortTranslucentFaces", "legacyWater", "biomeBlendRadius");

	private static List<DimensionEntry> normalizeDimensions(Element root, Element defaults, Configuration config) {
		List<DimensionEntry> result = new ArrayList<>();
		int legacyNumber = 0;
		for (Element element : getChildren(root, null)) {
			if (element.getTagName().equals("world")) {
				String id = "World" + config.getWorlds().size();
				Element effective = inherit(defaults, element);
				String worldDir = getString(effective, "worldDir");
				if (worldDir.isBlank())
					throw new IllegalArgumentException("World " + id + " requires worldDir");
				WorldConfig world = new WorldConfig(id, new File(worldDir));
				String worldName = getString(element, "name");
				world.setName(worldName.isBlank() ? WorldConfig.defaultName(world.getWorldDir()) : worldName);
				String jar = getString(effective, "minecraftJar");
				world.setMinecraftJar(jar.isBlank() ? null : new File(jar));
				String pack = getString(effective, "texturePack");
				world.setTexturePack(pack.isBlank() ? null : new File(pack));
				world.setUsingProgrammerArt(getBoolean(effective, "useProgrammerArt", false));
				world.setMarkerVisibility(parseMarkerVisibility(effective));
				world.setShowPlayerInventories(getBoolean(effective, "showPlayerInventories", false));
				parseRenderOptions(effective, world);
				Element mods = getChild(effective, "mods");
				if (mods != null)
					world.setModJars(java.util.Arrays.stream(getChildren(mods, "mod"))
							.map(mod -> new File(getString(mod, "path"))).toList());
				config.addWorld(world);
				int dimensionNumber = 0;
				for (Element dimension : getChildren(element, null)) {
					if (!isDimension(dimension))
						continue;
					Element resolved = inherit(effective, dimension);
					// The save directory belongs to the world, never to a nested dimension.
					resolved.setAttribute("worldDir", worldDir);
					result.add(new DimensionEntry(resolved, world, id + ".Dimension" + dimensionNumber++, false));
				}
			} else if (isDimension(element)) {
				result.add(new DimensionEntry(inherit(defaults, element), null, "Map" + legacyNumber++, true));
			}
		}
		return result;
	}

	private static boolean isDimension(Element element) {
		return element.getTagName().equals("dimension") || element.getTagName().equals("map");
	}

	private static String layerSuffix(int index) {
		StringBuilder result = new StringBuilder();
		do {
			result.insert(0, (char) ('A' + index % 26));
			index = index / 26 - 1;
		} while (index >= 0);
		return result.toString();
	}

	/** Merge only settings; hierarchy children always belong to their original scope. */
	private static Element inherit(Element defaults, Element overrides) {
		Element result = (Element) overrides.cloneNode(false);
		if (defaults != null) {
			for (int i = 0; i < defaults.getAttributes().getLength(); i++) {
				Node attribute = defaults.getAttributes().item(i);
				if (attribute.getNodeName().equals("name"))
					continue;
				result.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
			}
			for (Element child : getChildren(defaults, null)) {
				if (!List.of("world", "dimension", "map", "layer").contains(child.getTagName()))
					result.appendChild(child.cloneNode(true));
			}
		}
		for (int i = 0; i < overrides.getAttributes().getLength(); i++) {
			Node attribute = overrides.getAttributes().item(i);
			result.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
		}
		for (Element child : getChildren(overrides, null)) {
			Element previous = getChild(result, child.getTagName());
			if (previous != null && !List.of("layer", "map", "dimension", "world").contains(child.getTagName())) {
				Element merged = List.of("mods", "subset").contains(child.getTagName())
						? (Element) child.cloneNode(true) : inherit(previous, child);
				result.replaceChild(merged, previous);
			} else {
				result.appendChild(child.cloneNode(true));
			}
		}
		if (!List.of("config", "world", "dimension", "map", "layer").contains(overrides.getTagName()))
			return result;
		Element render = getChild(result, "render");
		if (render == null) {
			render = result.getOwnerDocument().createElement("render");
			result.appendChild(render);
		}
		Element ownRender = getChild(overrides, "render");
		for (String attribute : RENDER_ATTRIBUTES) {
			if (defaults != null && defaults.hasAttribute(attribute) && !render.hasAttribute(attribute))
				render.setAttribute(attribute, defaults.getAttribute(attribute));
			if (overrides.hasAttribute(attribute))
				render.setAttribute(attribute, overrides.getAttribute(attribute));
			if (ownRender != null && ownRender.hasAttribute(attribute))
				render.setAttribute(attribute, ownRender.getAttribute(attribute));
		}
		return result;
	}

	private static void parseRenderOptions(Element element, RenderOptions options) {
		Element render = getChild(element, "render");
		options.setSmoothLit(getBoolean(render, "useSmoothLighting", getBoolean(element, "useSmoothLighting", true)));
		options.setOldGrayscaleLighting(getBoolean(render, "useOldGrayscaleLighting", false));
		options.setSortTranslucentFaces(getBoolean(render, "sortTranslucentFaces", true));
		options.setLegacyWater(getBoolean(render, "legacyWater", false));
		String radius = getString(render, "biomeBlendRadius");
		if (StringUtils.isNotBlank(radius))
			options.setBiomeBlendRadius(parseBiomeBlendRadius(radius));
	}

	private static MarkerVisibility parseMarkerVisibility(Element element) {
		MarkerVisibility visibility = new MarkerVisibility();
		visibility.setSpawn(getBoolean(element, "spawnInitiallyVisible", true));
		visibility.setPlayers(getBoolean(element, "playersInitiallyVisible", true));
		visibility.setPortals(getBoolean(element, "portalsInitiallyVisible", true));
		visibility.setSigns(getBoolean(element, "signsInitiallyVisible", true));
		visibility.setBeds(getBoolean(element, "bedsInitiallyVisible", true));
		visibility.setRespawnAnchors(getBoolean(element, "respawnAnchorsInitiallyVisible", true));
		visibility.setViews(getBoolean(element, "viewsInitiallyVisible", true));
		visibility.setChests(getBoolean(element, "chestsInitiallyVisible", false));
		visibility.setBeacons(getBoolean(element, "beaconsInitiallyVisible", true));
		return visibility;
	}

	private static Element loadXml(File file, String rootName) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			Document doc = docBuilder.parse(file);
			NodeList nodeList = doc.getElementsByTagName(rootName);
			return (Element)nodeList.item(0);
		} catch (FileNotFoundException e) {
			log.error("Could not find file: {}", file.getAbsolutePath());
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
		
		return null;
	}
	
	public static Element getChild(Element parent, String name)
	{
		if (parent == null)
			return null;
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
			if (n.getNodeType() == Node.ELEMENT_NODE && (name == null || n.getNodeName().equals(name)))
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
			
			Element rectangularNode = getChild(subsetNode, "RectangularSubset");
			if (rectangularNode != null) {
				long width = 0;
				long depth = 0;
				
				try {
					width = Long.parseLong(rectangularNode.getAttribute("width"));
					depth = Long.parseLong(rectangularNode.getAttribute("depth"));
				} catch (NumberFormatException e) {
					log.error("Failed to parse rectangular subset attributes", e);
				}

				Vector3l origin = null;
				if (rectangularNode.hasAttribute("origin")) {
					origin = parseOrigin(rectangularNode.getAttribute("origin"));
				}
				
				if (width > 0 && depth > 0) {
					return new RectangularWorldSubset(origin, width, depth);
				}
			}
		}

		return new FullWorldSubset();
	}
}
