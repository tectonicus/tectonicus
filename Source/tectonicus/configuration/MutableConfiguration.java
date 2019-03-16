/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
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
import java.util.concurrent.Callable;

import lombok.Data;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tectonicus.Log;

@Data
@Command(name = "java -jar Tectonicus.jar", mixinStandardHelpOptions = true,
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

	@Option(names = {"-x", "--extractNatives", "extractNatives"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean extractLwjglNatives;

	@Option(names = {"-d", "--eraseOutputDir", "eraseOutputDir"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean eraseOutputDir;

	@Option(names = {"-l", "--logFile", "logFile"}, paramLabel = "<String>")
	private File logFile;

	@Option(names = {"-o", "--outputDir", "outputDir"}, paramLabel = "<String>")
	private File outputDir;

	@Option(names = {"-C", "--useCache", "useCache"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean useCache;

	@Option(names = {"--cacheDir", "cacheDir"}, paramLabel = "<String>")
	private File cacheDir;

	@Option(names = {"-j", "--minecraftJar", "minecraftJar"}, paramLabel = "<String>")
	private File minecraftJar;

	@Option(names = {"-t", "--texturePack", "texturePack"}, paramLabel = "<String>")
	private File texturePack;

	@Option(names = {"-p", "--useOldColorPalette", "useOldColorPalette"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean useOldColorPalette;

	@Option(names = {"-f", "--outputHtmlName", "outputHtmlName"}, paramLabel = "<string>")
	private String outputHtmlName;

	@Option(names = {"-s", "--defaultSkin", "defaultSkin"}, paramLabel = "<string>")
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

	@Option(names = {"-w", "--tileSize", "tileSize"}, paramLabel = "<integer>")
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
	private boolean force32BitNatives;
	private boolean force64BitNatives;

	private String singlePlayerName;

	@Option(names = {"-u", "--updateToLeaflet"}, paramLabel = "<String>")
	private Path updateToLeaflet;
	
	private List<MutableMap> maps;
	
	public MutableConfiguration()
	{
		mode = Mode.CMD;
		rasteriserType = RasteriserType.LWJGL;
		extractLwjglNatives = true;
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
		outputHtmlName = "map.html";
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
		System.out.println("\tmode:"+getMode().getName());
		System.out.println("\trasteriser:"+getRasteriserType());
		System.out.println("\toutputDir:"+outputDir.getAbsolutePath());
		System.out.println("\tuseCache:"+useCache());
		System.out.println("\tcacheDir:"+cacheDir.getAbsolutePath());
		System.out.println("\tminecraftJar:"+minecraftJar.getAbsolutePath());
		System.out.println("\ttexturePack:"+(texturePack != null ? texturePack.getAbsolutePath() : "none"));
		System.out.println("\tuseOldColorPalette:"+useOldColorPalette());
		System.out.println("\tcolourDepth:"+colourDepth());
		System.out.println("\talphaBits:"+alphaBits());
		System.out.println("\tnumSamples:"+numSamples());
		System.out.println("\ttileSize:"+tileSize());
		System.out.println("\tnumZoomLevels:"+numZoomLevels());
		System.out.println("\tportalsInitiallyVisible:"+arePortalsInitiallyVisible());
		System.out.println("\tshowSpawn:"+showSpawn());
		System.out.println("\tsignsInitiallyVisible:"+areSignsInitiallyVisible());
		System.out.println("\tplayersInitiallyVisible:"+arePlayersInitiallyVisible());
		System.out.println("\tbedsInitiallyVisible:"+areBedsInitiallyVisible());
		System.out.println("\tspawnInitiallyVisible:"+isSpawnInitiallyVisible());
		System.out.println("\tviewsInitiallyVisible:"+areViewsInitiallyVisible());
		System.out.println("\teraseOutputDir:"+eraseOutputDir());
		System.out.println("\textractLwjglNatives:"+extractLwjglNatives());
		System.out.println("\tisVerbose:"+isVerbose());
		System.out.println("\tforceLoadAwt:"+forceLoadAwt());
		System.out.println("\tforce32BitNatives:"+force32BitNatives());
		System.out.println("\tforce64BitNatives:"+force64BitNatives());
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
				System.out.println("\tno layers!");
			}
		}
		if (numMaps() == 0)
		{
			System.out.println("no maps!");
		}
		
		System.out.println();
	}

	public void setExtractLwjglNatives(final boolean extractLwjglNatives)
	{
		this.extractLwjglNatives = extractLwjglNatives;
	}
	public boolean extractLwjglNatives() { return extractLwjglNatives; }
	
	public void setEraseOutputDir(final boolean eraseOutputDir)
	{
		this.eraseOutputDir = eraseOutputDir;
	}
	public boolean eraseOutputDir() { return eraseOutputDir; }
	
	public void setOutputDir(File dir)
	{
		this.outputDir = dir;
	}
	public File outputDir() { return outputDir; }
	
	public void setCacheDir(File dir)
	{
		this.cacheDir = dir;
	}
	public File cacheDir() { return cacheDir; }
	
	public void setUseCache(final boolean useCache)
	{
		this.useCache = useCache;
	}
	public boolean useCache() { return useCache; }
	
	public void setMinecraftJar(File jar)
	{
		this.minecraftJar = jar;
	}
	public File minecraftJar() { return minecraftJar; }
	
	public void setTexturePack(File texturePack)
	{
		this.texturePack = texturePack;
	}
	public File texturePack() { return texturePack; }
	
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
	
	public void setColourDepth(final int colourDepth)
	{
		this.colourDepth = colourDepth;
	}
	public int colourDepth() { return colourDepth; }
	
	public void setAlphaBits(final int alphaBits)
	{
		this.alphaBits = alphaBits;
	}
	public int alphaBits() { return alphaBits; }
	
	public void setNumSamples(final int numSamples)
	{
		this.numSamples = numSamples;
	}
	public int numSamples() { return numSamples; }
	
	public void setNumZoomLevels(final int numZoomLevels)
	{
		this.numZoomLevels = numZoomLevels;
	}
	public int numZoomLevels() { return numZoomLevels; }
	
	public void setMaxTiles(final int maxTiles)
	{
		this.maxTiles = maxTiles;
	}
	public int maxTiles() { return maxTiles; }
	
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
	
	public void setForce32BitNatives(final boolean force32BitNatives)
	{
		this.force32BitNatives = force32BitNatives;
	}
	public boolean force32BitNatives() { return force32BitNatives; }
	
	public void setForce64BitNatives(final boolean force64BitNatives)
	{
		this.force64BitNatives = force64BitNatives;
	}
	public boolean force64BitNatives() { return force64BitNatives; }
	
	public void setTileSize(final int tileSize)
	{
		this.tileSize = tileSize;
	}
	public int tileSize() { return tileSize; }
	
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


	public void setLogFile(File file)
	{
		this.logFile = file;
	}
	public File getLogFile() { return logFile; }
	
	public void setOutputHtmlName(String name)
	{
		this.outputHtmlName = name;
	}
	public String getOutputHtmlName() { return outputHtmlName; }
	
	public void setDefaultSkin(String skin) { this.defaultSkin = skin; }
	public String getDefaultSkin()	{ return defaultSkin; }
	
	public void setNumDownsampleThreads(final int num)
	{
		this.numDownsampleThreads = num;
	}
	public int getNumDownsampleThreads() { return numDownsampleThreads; }
	
	public void setSinglePlayerName(String name)
	{
		this.singlePlayerName = name;
	}
	public String getSinglePlayerName() { return singlePlayerName; }
	
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
		return new ArrayList<Map>(maps);
	}
	
	public void addMap(MutableMap newMap)
	{
		if (maps.contains(newMap))
			throw new RuntimeException("Map already present in configuration!");
		
		maps.add(newMap);
	}
}
