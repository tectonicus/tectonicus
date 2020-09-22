/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tectonicus.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Log4j2
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
		description = "Tectonicus is a high detail Minecraft world mapper focused on creating zoomable maps that look as close to what you see in Minecraft as possible.",
		resourceBundle = "commandLine", versionProvider = tectonicus.BuildInfo.PropertiesVersionProvider.class)
public class MutableConfiguration implements Configuration, Callable<MutableConfiguration>
{
	@Option(names = {"-c", "--config", "config"}, paramLabel = "<String>")
	private Path configFile;

	@Option(names = {"-m", "--mode", "mode"}, paramLabel = "<string>")
	private Mode mode;

	@Option(names = {"-r", "--rasterizer", "rasterizer", "rasteriser"}, paramLabel = "<string>")
	private RasteriserType rasteriserType;

	@Option(names = {"-d", "--eraseOutputDir", "eraseOutputDir"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean eraseOutputDir;

	@Option(names = {"-l", "--logFile", "logFile"}, paramLabel = "<String>")
	private File logFile;

	@Option(names = {"-o", "--outputDir", "outputDir"}, paramLabel = "<String>")
	private File outputDir;

	@Option(names = {"-w", "--worldDir", "worldDir"}, paramLabel = "<String>")
	private MutableMap worldDir;

	@Option(names = {"-C", "--useCache", "useCache"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean useCache;

	@Option(names = {"--cacheDir", "cacheDir"}, paramLabel = "<String>")
	private File cacheDir;

	@Option(names = {"-j", "--minecraftJar", "minecraftJar"}, paramLabel = "<String>")
	private File minecraftJar;

	@Option(names = {"-t", "--texturePack", "texturePack"}, paramLabel = "<String>")
	private File texturePack;

	@Option(names = {"-p", "--useOldColorPalette", "useOldColorPalette"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean useOldColorPalette; //TODO: is this working?

	@Option(names = {"-a", "--useProgrammerArt", "useProgrammerArt"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean usingProgrammerArt;

	@Option(names = {"-f", "--outputHtmlName", "outputHtmlName"}, paramLabel = "<string>")
	private String outputHtmlName;

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
	@Option(names = {"--viewsInitiallyVisible", "viewsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean viewsInitiallyVisible;
	@Option(names = {"--spawnInitiallyVisible", "spawnInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean spawnInitiallyVisible;

	@Option(names = {"--showSpawn", "showSpawn"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean showSpawn;

	@Option(names = {"-v", "--verbose", "verbose"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean isVerbose;

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

	@Option(names = {"-u", "--updateToLeaflet"}, paramLabel = "<String>")
	private Path updateToLeaflet;

	private List<MutableMap> maps;

	public MutableConfiguration()
	{
		mode = Mode.CMD;
		rasteriserType = RasteriserType.LWJGL;
		showSpawn = true;
		tileSize = 512;
		maxTiles = -1;
		colourDepth = 16;
		alphaBits = 8;
		numSamples = 0;
		numZoomLevels = 8;
		signsInitiallyVisible = true;
		playersInitiallyVisible = true;
		portalsInitiallyVisible = true;
		bedsInitiallyVisible = true;
		spawnInitiallyVisible = true;
		viewsInitiallyVisible = true;
		logFile = new File("./TectonicusLog.txt");
		loggingLevel = Level.DEBUG;
		outputHtmlName = "map.html";
		outputDir = new File(".");
		cacheDir = new File(outputDir, "Cache");
		defaultSkin = "steve";
		numDownsampleThreads = 1;
		singlePlayerName = "Player";
		maps = new ArrayList<>();
	}

	@Override
	public MutableConfiguration call() {
		return this;
	}

	public void printActive()
	{
		System.out.println("Settings:");
		System.out.println("\tloggingLevel:"+getLoggingLevel());
		System.out.println("\tmode:"+getMode().getName());
		System.out.println("\trasteriser:"+getRasteriserType());
		System.out.println("\toutputDir:"+outputDir.getAbsolutePath());
		System.out.println("\tuseCache:"+useCache());
		System.out.println("\tcacheDir:"+cacheDir.getAbsolutePath());
		System.out.println("\tminecraftJar:"+minecraftJar.getAbsolutePath());
		System.out.println("\ttexturePack:"+(texturePack != null ? texturePack.getAbsolutePath() : "none"));
		System.out.println("\tuseOldColorPalette:"+useOldColorPalette());
		System.out.println("\tcolourDepth:"+ this.getColourDepth());
		System.out.println("\talphaBits:"+ this.getAlphaBits());
		System.out.println("\tnumSamples:"+ this.getNumSamples());
		System.out.println("\ttileSize:"+ this.getTileSize());
		System.out.println("\tnumZoomLevels:"+ this.getNumZoomLevels());
		System.out.println("\tportalsInitiallyVisible:"+arePortalsInitiallyVisible());
		System.out.println("\tshowSpawn:"+showSpawn());
		System.out.println("\tsignsInitiallyVisible:"+areSignsInitiallyVisible());
		System.out.println("\tplayersInitiallyVisible:"+arePlayersInitiallyVisible());
		System.out.println("\tbedsInitiallyVisible:"+areBedsInitiallyVisible());
		System.out.println("\tspawnInitiallyVisible:"+isSpawnInitiallyVisible());
		System.out.println("\tviewsInitiallyVisible:"+areViewsInitiallyVisible());
		System.out.println("\teraseOutputDir:"+eraseOutputDir());
		System.out.println("\tisVerbose:"+isVerbose());
		System.out.println("\tforceLoadAwt:"+forceLoadAwt());
		System.out.println("\tlogFile:"+getLogFile().getAbsolutePath());
		System.out.println("\toutputHtmlName:"+getOutputHtmlName());
		System.out.println("\tnumDownsampleThreads:"+getNumDownsampleThreads());
		System.out.println("\tsinglePlayerName:"+getSinglePlayerName());

		System.out.println();

		for (Map m : getMaps())
		{
			System.out.println("'"+m.getName()+"' map");

			System.out.println("\tworldDir: "+m.getWorldDir().getAbsolutePath());
			System.out.println("\tdimension: "+m.getDimension());
			System.out.println("\tcameraAngle: "+m.getCameraAngleDeg());
			System.out.println("\tcameraElevation: "+m.getCameraElevationDeg());
			System.out.println("\tclosestZoomSize: "+m.getClosestZoomSize());
			System.out.println("\tworldSubset: "+m.getWorldSubsetFactory().getDescription());
			System.out.println("\tuseBiomeColours: "+m.useBiomeColours());

			for (Layer l : m.getLayers())
			{
				System.out.println("\t'"+l.getName()+"' layer");

				System.out.println("\t\trenderStyle: "+l.getRenderStyle());
				System.out.println("\t\tlightStyle: "+l.getLightStyle());
				System.out.println("\t\timageFormat: "+l.getImageFormat());

				if (l.getImageFormat() == ImageFormat.Jpg)
					System.out.println("\t\timageCompressionLevel: "+l.getImageCompressionLevel());

				if (l.getCustomBlockConfig() != null)
					System.out.println("\t\tcustomBlockConfig: "+l.getCustomBlockConfig());
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

	public MutableMap getWorldDir() { return worldDir; }

	public void setUseCache(final boolean useCache)
	{
		this.useCache = useCache;
	}
	public boolean useCache() { return useCache; }

	public File minecraftJar() { return minecraftJar; }

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

	public void setIsVerbose(final boolean isVerbose)
	{
		this.isVerbose = isVerbose;
		if (isVerbose)
			Log.setLogLevel(Log.DEBUG);
	}
	public boolean isVerbose() { return isVerbose; }

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

