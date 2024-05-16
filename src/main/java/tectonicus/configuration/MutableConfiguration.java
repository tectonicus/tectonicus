/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import ch.qos.logback.classic.Level;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Data
@Command(name = "java -jar Tectonicus.jar",
		header = {
				"@|cyan  ______               __                                                 |@",
				"@|cyan /\\__  _\\             /\\ \\__                 __                           |@",
				"@|cyan \\/_/\\ \\/    __    ___\\ \\ ,_\\   ___     ___ /\\_\\    ___   __  __    ____  |@",
				"@|cyan    \\ \\ \\  /'__`\\ /'___\\ \\ \\/  / __`\\ /' _ `\\/\\ \\  /'___\\/\\ \\/\\ \\  /',__\\ |@",
				"@|cyan     \\ \\ \\/\\  __//\\ \\__/\\ \\ \\_/\\ \\L\\ \\/\\ \\/\\ \\ \\ \\/\\ \\__/\\ \\ \\_\\ \\/\\__, `\\|@",
				"@|cyan      \\ \\_\\ \\____\\ \\____\\\\ \\__\\ \\____/\\ \\_\\ \\_\\ \\_\\ \\____\\\\ \\____/\\/\\____/|@",
				"@|cyan       \\/_/\\/____/\\/____/ \\/__/\\/___/  \\/_/\\/_/\\/_/\\/____/ \\/___/  \\/___/ |@",
				""}, //Font is 'Larry 3D' from http://www.patorjk.com/software/taag
		mixinStandardHelpOptions = true,
		description = "Tectonicus is a high detail Minecraft world mapper focused on creating zoomable maps that look very close to what you see in Minecraft.",
		resourceBundle = "commandLine", versionProvider = tectonicus.BuildInfo.PropertiesVersionProvider.class)
public class MutableConfiguration implements Configuration, Callable<MutableConfiguration> {
	@Option(names = {"-c", "--config", "config"}, paramLabel = "<String>")
	private Path configFile;

	@Option(names = {"-m", "--mode", "mode"}, paramLabel = "<string>")
	private Mode mode;

	@Option(names = {"-r", "--rasterizer", "rasterizer", "rasteriser"}, paramLabel = "<string>")
	private RasteriserType rasteriserType;

	@Option(names = {"--useEGL"}, paramLabel = "<boolean>")
	private boolean useEGL;

	@Option(names = {"-e", "--eraseOutputDir", "eraseOutputDir"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean eraseOutputDir;

	@Option(names = {"-o", "--outputDir", "outputDir"}, paramLabel = "<String>")
	private File outputDir;

	@Option(names = {"-w", "--worldDir", "worldDir"}, paramLabel = "<String>")
	private File worldDir;

	@Option(names = {"-d", "--dimension", "dimension"}, paramLabel = "<String>")
	private Dimension dimension;

	@Option(names = {"-C", "--useCache", "useCache"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean useCache;

	@Option(names = {"--cacheDir", "cacheDir"}, paramLabel = "<String>")
	private File cacheDir;

	@Option(names = {"-j", "--minecraftJar", "minecraftJar"}, paramLabel = "<String>")
	private File minecraftJar;

	@Option(names = {"--useSmoothLighting"}, paramLabel = "<boolean>")
	private boolean smoothLit;

	@Option(names = {"-t", "--texturePack", "texturePack"}, paramLabel = "<String>")
	private File texturePack;

	@Option(names = {"-p", "--useOldColorPalette", "useOldColorPalette"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean useOldColorPalette; //TODO: is this working?

	@Option(names = {"-a", "--useProgrammerArt"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean usingProgrammerArt;

	@Option(names = {"-f", "--outputHtmlName", "outputHtmlName"}, paramLabel = "<string>")
	private String outputHtmlName;

	@Option(names = {"--htmlTitle"}, paramLabel = "<string>")
	private String htmlTitle;

	@Option(names = {"--defaultSkin", "defaultSkin"}, paramLabel = "<string>")
	private String defaultSkin;

	@Option(names = {"--signsInitiallyVisible", "signsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean signsInitiallyVisible;
	@Option(names = {"--playersInitiallyVisible", "playersInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean playersInitiallyVisible;
	@Option(names = {"--portalsInitiallyVisible", "portalsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean portalsInitiallyVisible;
	@Option(names = {"--bedsInitiallyVisible", "bedsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean bedsInitiallyVisible;
	@Option(names = {"--respawnAnchorsInitiallyVisible", "respawnAnchorsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean respawnAnchorsInitiallyVisible;
	@Option(names = {"--viewsInitiallyVisible", "viewsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean viewsInitiallyVisible;
	@Option(names = {"--spawnInitiallyVisible", "spawnInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean spawnInitiallyVisible;

	@Option(names = {"--showSpawn", "showSpawn"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean showSpawn;

	@Option(names = {"-v", "--verbose", "verbose"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean verbose;

	@Option(names = {"--logLevel","logLevel"}, paramLabel = "<String>")
	private Level loggingLevel;

	@Option(names = {"-s", "--tileSize", "tileSize"}, paramLabel = "<integer>")
	private int tileSize;

	@Option(names = {"--maxTiles", "maxTiles"}, paramLabel = "<integer>")
	private int maxTiles;

	@Option(names = {"--colorDepth", "colorDepth", "colourDepth"}, paramLabel = "<integer>")
	private int colourDepth;
	@Option(names = {"--alphaBits", "alphaBits"}, paramLabel = "<integer>")
	private int alphaBits;
	@Option(names = {"--numSamples", "numSamples"}, paramLabel = "<integer>")
	private int numSamples;

	@Option(names = {"-z", "--numZoomLevels", "numZoomLevels"}, paramLabel = "<integer>")
	private int numZoomLevels;

	@Option(names = {"--numDownsampleThreads", "numDownsampleThreads"}, paramLabel = "<integer>")
	private int numDownsampleThreads;

	private boolean forceLoadAwt;

	private String singlePlayerName;
        
	@Option(names = {"-css", "--customStyle", "customStyle"}, paramLabel = "<String>")
	private String customStyle;

	@Option(names = {"-js", "--customScript", "customScript"}, paramLabel = "<String>")
	private String customScript;
	
	@Option(names = {"--useCdn", "useCdn"}, paramLabel = "<String")
	private String useCdn;

	private List<MutableMap> maps;

	public MutableConfiguration() {
		mode = Mode.CMD;
		rasteriserType = RasteriserType.LWJGL;
		showSpawn = true;
		tileSize = 512;
		maxTiles = -1;
		colourDepth = 32;
		alphaBits = 8;
		numSamples = 0;
		numZoomLevels = 8;
		signsInitiallyVisible = true;
		playersInitiallyVisible = true;
		portalsInitiallyVisible = true;
		bedsInitiallyVisible = true;
		spawnInitiallyVisible = true;
		viewsInitiallyVisible = true;
		loggingLevel = Level.DEBUG;
		outputHtmlName = "map.html";
		outputDir = new File(".");
		cacheDir = new File(outputDir, "Cache");
		defaultSkin = "steve";
		numDownsampleThreads = 1;
		singlePlayerName = "";
		maps = new ArrayList<>();
		smoothLit = false;
	}

	@Override
	public MutableConfiguration call() {
		return this;
	}

	public void printActive() {
		log.debug("Settings:");
		log.debug("\tloggingLevel:{}", getLoggingLevel());
		log.debug("\tmode:{}", getMode().getName());
		log.debug("\trasteriser:{}", getRasteriserType());
		log.debug("\tuseEGL: {}", isUseEGL());
		log.debug("\toutputDir:{}", outputDir.getAbsolutePath());
		log.debug("\tuseCache:{}", useCache());
		log.debug("\tcacheDir:{}", cacheDir.getAbsolutePath());
		log.debug("\ttexturePack:{}", texturePack != null ? texturePack.getAbsolutePath() : "none");
		log.debug("\tuseOldColorPalette:{}", useOldColorPalette());
		log.debug("\tcolourDepth:{}", this.getColourDepth());
		log.debug("\talphaBits:{}", this.getAlphaBits());
		log.debug("\tnumSamples:{}", this.getNumSamples());
		log.debug("\ttileSize:{}", this.getTileSize());
		log.debug("\tnumZoomLevels:{}", this.getNumZoomLevels());
		log.debug("\tportalsInitiallyVisible:{}", arePortalsInitiallyVisible());
		log.debug("\tshowSpawn:{}", showSpawn());
		log.debug("\tsignsInitiallyVisible:{}", areSignsInitiallyVisible());
		log.debug("\tplayersInitiallyVisible:{}", arePlayersInitiallyVisible());
		log.debug("\tbedsInitiallyVisible:{}", areBedsInitiallyVisible());
		log.debug("\trespawnAnchorsInitiallyVisible:{}", areRespawnAnchorsInitiallyVisible());
		log.debug("\tspawnInitiallyVisible:{}", isSpawnInitiallyVisible());
		log.debug("\tviewsInitiallyVisible:{}", areViewsInitiallyVisible());
		log.debug("\teraseOutputDir:{}", eraseOutputDir());
		log.debug("\tforceLoadAwt:{}", forceLoadAwt());
		log.debug("\toutputHtmlName:{}", getOutputHtmlName());
		log.debug("\thtmlTitle: {}", getHtmlTitle());
		log.debug("\tnumDownsampleThreads:{}", getNumDownsampleThreads());
		log.debug("\tsinglePlayerName:{}", getSinglePlayerName());
		log.debug("\tuseCdn: {}", getUseCdn());

		System.out.println();

		for (Map m : getMaps())
		{
			log.debug("'{}' map", m.getName());
			
			log.debug("\tworldDir: {}", m.getWorldDir().getAbsolutePath());
			log.debug("\tdimension: {}", m.getDimension());
			log.debug("\tcameraAngle: {}", m.getCameraAngleDeg());
			log.debug("\tcameraElevation: {}", m.getCameraElevationDeg());
			log.debug("\tclosestZoomSize: {}", m.getClosestZoomSize());
			log.debug("\tuseSmoothLighting:{}", m.isSmoothLit());
			log.debug("\tuseBiomeColours: {}", m.useBiomeColours());

			for (Layer l : m.getLayers())
			{
				log.debug("\t'{}' layer", l.getName());
				
				log.debug("\t\trenderStyle: {}", l.getRenderStyle());
				log.debug("\t\tlightStyle: {}", l.getLightStyle());
				log.debug("\t\timageFormat: {}", l.getImageFormat());

				if (l.getImageFormat() == ImageFormat.JPG)
					log.debug("\t\timageCompressionLevel: {}", l.getImageCompressionLevel());

				if (l.getCustomBlockConfig() != null)
					log.debug("\t\tcustomBlockConfig: {}", l.getCustomBlockConfig());
			}
			if (m.numLayers() == 0)
			{
				log.warn("\tNo layers found!");
			}
		}
		if (numMaps() == 0)
		{
			log.warn("No maps found!");
		}

		System.out.println();
	}

	public void setEraseOutputDir(final boolean eraseOutputDir)
	{
		this.eraseOutputDir = eraseOutputDir;
	}
	public boolean eraseOutputDir() { return eraseOutputDir; }

	public void setUseCache(final boolean useCache)
	{
		this.useCache = useCache;
	}
	public boolean useCache() { return useCache; }

	public void setUseOldColorPalette(boolean useOldColorPalette)
	{
		this.useOldColorPalette = useOldColorPalette;
	}
	public boolean useOldColorPalette() { return useOldColorPalette; }

	public void setShowPlayerSpawn(final boolean showSpawn)
	{
		this.showSpawn = showSpawn;
	}
	public boolean showSpawn() { return showSpawn; }

	public void setForceLoadAwt(final boolean forceLoadAwt)
	{
		this.forceLoadAwt = forceLoadAwt;
	}
	public boolean forceLoadAwt() { return forceLoadAwt; }

	public void setSignsInitiallyVisible(final boolean visible)
	{
		this.signsInitiallyVisible = visible;
	}
	public boolean areSignsInitiallyVisible() { return signsInitiallyVisible; }

	public void setPlayersInitiallyVisible(final boolean visible)
	{
		this.playersInitiallyVisible = visible;
	}
	public boolean arePlayersInitiallyVisible() { return playersInitiallyVisible; }

	public void setPortalsInitiallyVisible(final boolean visible)
	{
		this.portalsInitiallyVisible = visible;
	}
	public boolean arePortalsInitiallyVisible() { return portalsInitiallyVisible; }

	public void setBedsInitiallyVisible(final boolean visible)
	{
		this.bedsInitiallyVisible = visible;
	}
	public boolean areBedsInitiallyVisible() { return bedsInitiallyVisible; }

	public void setRespawnAnchorsInitiallyVisible(final boolean visible)
	{
		this.respawnAnchorsInitiallyVisible = visible;
	}
	public boolean areRespawnAnchorsInitiallyVisible() { return respawnAnchorsInitiallyVisible; }

	public void setSpawnInitiallyVisible(final boolean visible)
	{
		this.spawnInitiallyVisible = visible;
	}
	public boolean isSpawnInitiallyVisible() { return spawnInitiallyVisible; }

	public void setViewsInitiallyVisible(final boolean visible)
	{
		this.viewsInitiallyVisible = visible;
	}
	public boolean areViewsInitiallyVisible() { return viewsInitiallyVisible; }
        
	public int numMaps()
	{
		return maps.size();
	}

	public MutableMap getMap(final int index)
	{
		if (index < 0 || index >= maps.size())
			return null;

		return maps.get(index);
	}

	@Override
	public List<Map> getMaps()
	{
		return new ArrayList<>(maps);
	}

	public void addMap(MutableMap newMap)
	{
		if (maps.contains(newMap))
			throw new RuntimeException("Map already present in configuration!");

		maps.add(newMap);
	}
}

