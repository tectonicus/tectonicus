/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.Layer;
import tectonicus.configuration.MutableConfiguration;
import tectonicus.configuration.MutableLayer;
import tectonicus.configuration.MutableMap;
import tectonicus.configuration.XmlConfigurationParser;
import tectonicus.gui.Gui;
import tectonicus.raw.Player;
import tectonicus.util.FileUtils;
import tectonicus.util.OutputResourcesUtil;
import tectonicus.util.Util;
import tectonicus.world.World;

import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TectonicusApp
{
	private final Configuration config;
	private static org.slf4j.Logger log;

	private TectonicusApp(Configuration config)
	{
		this.config = config;

		if (config.isVerbose()) {
			config.setLoggingLevel(Level.TRACE);
		}
		
		Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.setLevel(config.getLoggingLevel());
		
		BuildInfo.print();
		
		log.info("Started on {}", new Date());

		log.debug("System:\n\tOS Name: {}\n\tOS Architecture: {}\n\tOS Version: {}",
				getProperty("os.name"),
				getProperty("os.arch"),
				getProperty("os.version"));

		log.debug("\tJava vendor: {}\n\tJava version: {}\n\tAwt toolkit: {}",
				getProperty("java.vendor"),
				getProperty("java.version"),
				getProperty("awt.toolkit"));
	}
	
	private static String getProperty(String key)
	{
		String result = "";
		try
		{
			result = System.getProperty(key);
		}
		catch (Exception e) {}
		return result;
	}

	public Integer run()
	{
		InteractiveRenderer interactiveRenderer = null;
		TileRenderer tileRenderer = null;
		try
		{
			MessageDigest hashAlgorithm = MessageDigest.getInstance("sha1");
			if (config.getMode() == Mode.INTERACTIVE)
			{
				interactiveRenderer = new InteractiveRenderer(config, 512, 512);

				tectonicus.configuration.Map map = config.getMap(0);;
				Layer layer = map.getLayer(0);
				
				BiomeCache biomeCache = CacheUtil.createBiomeCache(config, map, hashAlgorithm);
				PlayerSkinCache skinCache = new PlayerSkinCache(config, hashAlgorithm);
				
				World world = new World(interactiveRenderer.getRasteriser(), map, biomeCache, skinCache, config);
				TileRenderer.setupWorldForLayer(layer, world);
				
				interactiveRenderer.display(world);
				
				interactiveRenderer.destroy();
			}
			else if (config.getMode() == Mode.CMD)
			{
				// Do this first before we attempt to load any caches
				if (config.eraseOutputDir())
				{
					log.info("Deleting output dir: {}", config.getOutputDir().getAbsolutePath());
					
					FileUtils.deleteDirectory(config.getOutputDir());
				}
				
				tileRenderer = new TileRenderer(config, new CommandLineOutput(), hashAlgorithm);
				
				tileRenderer.output();
			}
			else if (config.getMode() == Mode.VIEWS)
			{
				tileRenderer = new TileRenderer(config, new CommandLineOutput(), hashAlgorithm);
				
				tileRenderer.renderViews();
			}
			else if (config.getMode() == Mode.PLAYERS)
			{
				final Date startTime = new Date();
				
				PlayerSkinCache skinCache = new PlayerSkinCache(config, hashAlgorithm);
				PlayerIconAssembler iconAssembler = new PlayerIconAssembler(skinCache);
				
				for (tectonicus.configuration.Map map : config.getMaps())
				{
					List<Player> players = World.loadPlayers(map.getWorldDir(), skinCache);
									
					File mapDir = new File(config.getOutputDir(), map.getId());
					File playerDir = new File(mapDir, "players.js");
					
					File imagesDir = new File(config.getOutputDir(), "Images");

					OutputResourcesUtil.outputPlayers(playerDir, imagesDir, map, players, iconAssembler);
				}
				
				skinCache.destroy();
				
				final Date endTime = new Date();
				String time = Util.getElapsedTime(startTime, endTime);
				log.debug("Player export took {}", time);
			}
			else if (config.getMode() == Mode.GUI)
			{
				Gui gui = new Gui(hashAlgorithm);
				gui.display();
			}
			else if (config.getMode() == Mode.PROFILE)
			{
				/*
				BiomeCache biomeCache = new NullBiomeCache();
				BlockFilter blockFilter = new NullBlockFilter();
				
				RegionIterator it = new AllRegionsIterator(args.worldDir());
				File regionFile = it.next();
				Region region = new Region(regionFile);
				ChunkCoord chunkCoord = region.getContainedChunks()[0];
				
				Chunk c = region.loadChunk(chunkCoord, biomeCache, blockFilter);
				
				// createGeometry profiling
				Rasteriser rasteriser = RasteriserFactory.createRasteriser(RasteriserType.LWJGL, DisplayType.Offscreen, 512, 312, 24, 8, 16, 4);
				NullBlockMaskFactory mask = new NullBlockMaskFactory();
				TexturePack texturePack = new TexturePack(rasteriser, args.minecraftJar(), args.texturePack());
				World world = new World(rasteriser, args.worldDir(), args.getDimensionDir(), args.minecraftJar(), args.texturePack(), biomeCache, hashAlgorithm, "Player", new FullWorldSubsetFactory());
				
				// Warm it up
				for (int i=0; i<100000; i++)
				{
					c.createGeometry(rasteriser, world, world.getBlockTypeRegistry(), mask, texturePack);
				}
				
				final long start = System.currentTimeMillis();
				
				for (long i=0; i<10000000000l; i++)
				{
					c.createGeometry(rasteriser, world, world.getBlockTypeRegistry(), mask, texturePack);
				}
				
				final long end = System.currentTimeMillis();
				
				final long time = end - start;
				
				System.out.println("Total time: "+time+"ms");
				
				// Original:			5120ms
				// Two-loop version:	5117ms
				*/
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		catch (OutOfMemoryError e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (tileRenderer != null)
				tileRenderer.destroy();
			if (interactiveRenderer != null)
				interactiveRenderer.destroy();
		}
		
		log.info("Finished");

		return 0;
	}

	public static void main(String[] argArray) throws Exception {
		// Init the logging framework
		String logDirProperty = "tectonicus.logDir";
		String logAppendProperty = "tectonicus.logAppend";
		System.setProperty(logDirProperty, System.getProperty(logDirProperty, "log"));
		System.setProperty(logAppendProperty, System.getProperty(logAppendProperty, "false"));
		log = org.slf4j.LoggerFactory.getLogger(TectonicusApp.class);
		
		//Parse command line to get config file
		MutableConfiguration m = new MutableConfiguration();
		CommandLine cmd = new CommandLine(m);
		cmd.registerConverter(Level.class, Level::toLevel);
		cmd.setCaseInsensitiveEnumValuesAllowed(true);
		CommandLine.ParseResult parseResult = cmd.parseArgs(argArray);
		
		Path configFile = m.getConfigFile();
		//If no arguments are specified search for default config filenames
		if (parseResult.originalArgs().isEmpty()) {
			boolean defaultConfigFound = false;
			List<String> configNames = Arrays.asList("tectonicus.xml", "tectonicusConfig.xml");
			for (String name : configNames) {
				Path defaultConfigPath = Paths.get(name);
				if (Files.exists(defaultConfigPath)) {
					configFile = defaultConfigPath;
					defaultConfigFound = true;
					break;
				}
			}
			
			//If no arguments and no default config file found print usage help
			if (!defaultConfigFound) {
				cmd.usage(cmd.getOut());
				System.exit(cmd.getCommandSpec().exitCodeOnUsageHelp());
			}
		}
		
		MutableConfiguration config = new MutableConfiguration();
		if (configFile != null) {
			// Load config from xml first
			config = XmlConfigurationParser.parseConfiguration(configFile.toFile());
		}

		// Load config from command line second.  Command line options will override config file options
		CommandLine commandLine = new CommandLine(config);
		commandLine.registerConverter(Level.class, Level::toLevel);
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		commandLine.setExecutionStrategy(new CommandLine.RunFirst());
		commandLine.execute(argArray);

		if (commandLine.isUsageHelpRequested() || commandLine.isVersionHelpRequested()) {
			System.exit(commandLine.getCommandSpec().exitCodeOnUsageHelp());
		}

		if (config.numMaps() == 0) { //If no maps were specified in xml config add a single map and layer
			MutableMap map = new MutableMap("Map0");
			MutableLayer layer = new MutableLayer("LayerA", map.getId());
			map.addLayer(layer);
			map.setWorldDir(config.getWorldDir());
			//TODO: add additional map config options from command line
			config.addMap(map);
		}

		if (config.getDimension() != null) {
			config.getMap(0).setDimension(config.getDimension());
		}

		TectonicusApp app = new TectonicusApp(config);

		config.printActive();

		// Workaround for sun bug 6539705 ( http://bugs.sun.com/view_bug.do?bug_id=6539705 )
		// Trigger the load of the awt libraries before we load lwjgl
		Toolkit.getDefaultToolkit();

		if (config.forceLoadAwt()) {
			System.loadLibrary("awt");
		}

		app.run();
	}
}
