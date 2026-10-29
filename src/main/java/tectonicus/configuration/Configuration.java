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
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.ParseResult;
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
public class Configuration implements Callable<Configuration> {
	@Getter
	@RequiredArgsConstructor
	public enum Mode {
		CMD("Command Line"),
		GUI("Gui"),
		INTERACTIVE("Interactive"),
		PLAYERS("Export Players"),
		VIEWS("Render Views"),
		PROFILE("Profile");

		private final String name;
	}

	public enum RasteriserType {
		LWJGL
	}

	public enum RenderStyle {
		NORMAL,
		CAVE,
		EXPLORED_CAVES,
		NETHER
	}


	@Option(names = {"-c", "--config", "config"}, paramLabel = "<String>")
	private Path configFile;

	@Option(names = {"-m", "--mode", "mode"}, paramLabel = "<string>")
	private Mode mode;

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
	private boolean showPlayerInventories;
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
	@Option(names = {"--chestsInitiallyVisible", "chestsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean chestsInitiallyVisible;
	@Option(names = {"--beaconsInitiallyVisible", "beaconsInitiallyVisible"}, arity = "0..1", paramLabel = "<boolean>")
	private boolean beaconsInitiallyVisible;

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

	private final List<WorldConfig> worlds = new ArrayList<>();

	public Configuration() {
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
		respawnAnchorsInitiallyVisible = true;
		beaconsInitiallyVisible = true;
		loggingLevel = Level.DEBUG;
		outputHtmlName = "map.html";
		outputDir = new File(".");
		cacheDir = new File(outputDir, "Cache");
		defaultSkin = "steve";
		numDownsampleThreads = 1;
		singlePlayerName = "";

		smoothLit = false;
	}

	@Override
	public Configuration call() {
		return this;
	}

	public void applyCommandLineOverrides(ParseResult options) {
		for (WorldConfig world : worlds) {
			if (options.hasMatchedOption("--minecraftJar"))
				world.setMinecraftJar(minecraftJar);
			if (options.hasMatchedOption("--texturePack"))
				world.setTexturePack(texturePack);
			if (options.hasMatchedOption("--useProgrammerArt"))
				world.setUsingProgrammerArt(usingProgrammerArt);
		}
		for (DimensionConfig dimension : getDimensions()) {
			if (options.hasMatchedOption("--numZoomLevels"))
				dimension.setNumZoomLevels(numZoomLevels);
			var visibility = dimension.getMarkerVisibility();
			if (options.hasMatchedOption("--spawnInitiallyVisible"))
				visibility.setSpawn(spawnInitiallyVisible);
			if (options.hasMatchedOption("--signsInitiallyVisible"))
				visibility.setSigns(signsInitiallyVisible);
			if (options.hasMatchedOption("--playersInitiallyVisible"))
				visibility.setPlayers(playersInitiallyVisible);
			if (options.hasMatchedOption("--portalsInitiallyVisible"))
				visibility.setPortals(portalsInitiallyVisible);
			if (options.hasMatchedOption("--bedsInitiallyVisible"))
				visibility.setBeds(bedsInitiallyVisible);
			if (options.hasMatchedOption("--respawnAnchorsInitiallyVisible"))
				visibility.setRespawnAnchors(respawnAnchorsInitiallyVisible);
			if (options.hasMatchedOption("--viewsInitiallyVisible"))
				visibility.setViews(viewsInitiallyVisible);
			if (options.hasMatchedOption("--chestsInitiallyVisible"))
				visibility.setChests(chestsInitiallyVisible);
			if (options.hasMatchedOption("--beaconsInitiallyVisible"))
				visibility.setBeacons(beaconsInitiallyVisible);
			if (options.hasMatchedOption("--useSmoothLighting")) {
				dimension.setSmoothLit(smoothLit);
				for (LayerConfig layer : dimension.getLayers())
					layer.setSmoothLit(smoothLit);
			}
		}
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
		log.debug("\tchestsInitiallyVisible:{}", isChestsInitiallyVisible());
		log.debug("\tbeaconsInitiallyVisible:{}", isBeaconsInitiallyVisible());
		log.debug("\teraseOutputDir:{}", eraseOutputDir());
		log.debug("\tforceLoadAwt:{}", forceLoadAwt());
		log.debug("\toutputHtmlName:{}", getOutputHtmlName());
		log.debug("\thtmlTitle: {}", getHtmlTitle());
		log.debug("\tnumDownsampleThreads:{}", getNumDownsampleThreads());
		log.debug("\tsinglePlayerName:{}", getSinglePlayerName());
		log.debug("\tuseCdn: {}", getUseCdn());

		System.out.println();

		for (DimensionConfig m : getDimensions())
		{
			log.debug("'{}' map", m.getName());

			log.debug("\tworldDir: {}", m.getWorldDir().getAbsolutePath());
			log.debug("\tdimension: {}", m.getDimension());
			log.debug("\tcameraAngle: {}", m.getCameraAngleDeg());
			log.debug("\tcameraElevation: {}", m.getCameraElevationDeg());
			log.debug("\tclosestZoomSize: {}", m.getClosestZoomSize());
			log.debug("\tuseSmoothLighting:{}", m.isSmoothLit());
			log.debug("\tsortTranslucentFaces:{}", m.isSortTranslucentFaces());
			log.debug("\tlegacyWater:{}", m.isLegacyWater());
			log.debug("\tbiomeBlendRadius: {}", m.isBiomeBlendRadiusConfigured() ? m.getBiomeBlendRadius() : "automatic");
			log.debug("\tuseBiomeColours: {}", m.useBiomeColours());

			for (LayerConfig l : m.getLayers())
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
		if (numDimensions() == 0)
		{
			log.warn("No maps found!");
		}

		System.out.println();
	}

	public boolean eraseOutputDir() { return eraseOutputDir; }

	public boolean useCache() { return useCache; }

	public boolean useOldColorPalette() { return useOldColorPalette; }

	public void setShowPlayerSpawn(final boolean showSpawn)
	{
		this.showSpawn = showSpawn;
	}
	public boolean showSpawn() { return showSpawn; }

	public boolean forceLoadAwt() { return forceLoadAwt; }

	public boolean areSignsInitiallyVisible() { return signsInitiallyVisible; }

	public boolean arePlayersInitiallyVisible() { return playersInitiallyVisible; }

	public boolean arePortalsInitiallyVisible() { return portalsInitiallyVisible; }

	public boolean areBedsInitiallyVisible() { return bedsInitiallyVisible; }

	public boolean areRespawnAnchorsInitiallyVisible() { return respawnAnchorsInitiallyVisible; }

	public boolean isSpawnInitiallyVisible() { return spawnInitiallyVisible; }

	public boolean areViewsInitiallyVisible() { return viewsInitiallyVisible; }

	public void setMinecraftJar(File minecraftJar) {
		for (WorldConfig world : worlds)
			if (java.util.Objects.equals(world.getMinecraftJar(), this.minecraftJar))
				world.setMinecraftJar(minecraftJar);
		this.minecraftJar = minecraftJar;
	}

	public void setTexturePack(File texturePack) {
		for (WorldConfig world : worlds)
			if (java.util.Objects.equals(world.getTexturePack(), this.texturePack))
				world.setTexturePack(texturePack);
		this.texturePack = texturePack;
	}

	public void setUsingProgrammerArt(boolean usingProgrammerArt) {
		for (WorldConfig world : worlds)
			if (world.isUsingProgrammerArt() == this.usingProgrammerArt)
				world.setUsingProgrammerArt(usingProgrammerArt);
		this.usingProgrammerArt = usingProgrammerArt;
	}

	public int numDimensions() {
		return getDimensions().size();
	}

	public DimensionConfig getDimensionConfig(int index) {
		List<DimensionConfig> dimensions = getDimensions();
		return index >= 0 && index < dimensions.size() ? dimensions.get(index) : null;
	}

	public List<DimensionConfig> getDimensions() {
		return worlds.stream().flatMap(world -> world.getDimensions().stream()).toList();
	}

	public List<WorldConfig> getWorlds() {
		return List.copyOf(worlds);
	}

	public void addWorld(WorldConfig world) {
		if (worlds.stream().anyMatch(existing -> existing.getId().equalsIgnoreCase(world.getId())))
			throw new IllegalArgumentException("Duplicate world ID: " + world.getId());
		worlds.add(world);
	}

	/** Normalize command-line and legacy dimensions by save directory. */
	public void addDimension(DimensionConfig dimension) {
		if (getDimensions().stream().anyMatch(existing -> existing.getId().equalsIgnoreCase(dimension.getId())))
			throw new IllegalArgumentException("Duplicate dimension ID: " + dimension.getId());
		Path directory = dimension.getWorldDir().toPath().toAbsolutePath().normalize();
		WorldConfig world = worlds.stream()
				.filter(existing -> existing.getWorldDir().toPath().toAbsolutePath().normalize().equals(directory))
				.findFirst().orElse(null);
		if (world == null) {
			world = new WorldConfig("World" + worlds.size(), dimension.getWorldDir());
			world.setName(WorldConfig.defaultName(dimension.getWorldDir()));
			world.setMinecraftJar(minecraftJar);
			world.setTexturePack(texturePack);
			world.setUsingProgrammerArt(usingProgrammerArt);
			world.setModJars(dimension.getModJars());
			addWorld(world);
		}
		world.addDimension(dimension);
	}
}
