/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TectonicusApp
{
	private final Configuration args;
	private static org.apache.logging.log4j.Logger log;

	private TectonicusApp(Configuration args)
	{
		this.args = args;

		if (args.isVerbose()) {
			args.setLoggingLevel(Level.TRACE);
		}

		Configurator.setRootLevel(args.getLoggingLevel());
		
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
			if (args.getMode() == Mode.INTERACTIVE)
			{
				interactiveRenderer = new InteractiveRenderer(args, 512, 512);

				tectonicus.configuration.Map map;
				Layer layer;
				if (args.getWorldDir() != null) {
					map = args.getWorldDir();
					layer = new MutableLayer("LayerA", map.getId());
				} else {
					map = args.getMap(0);
					layer = map.getLayer(0);
				}
				
				BiomeCache biomeCache = CacheUtil.createBiomeCache(args.minecraftJar(), args.getCacheDir(), map, hashAlgorithm);
				PlayerSkinCache skinCache = new PlayerSkinCache(args, hashAlgorithm);
				
				World world = new World(interactiveRenderer.getRasteriser(), map.getWorldDir(), map.getDimension(),
						args.minecraftJar(), args.getTexturePack(), map.getModJars(), biomeCache, hashAlgorithm, args.getSinglePlayerName(), map.getWorldSubset(), skinCache, map.getSignFilter(), args);
				TileRenderer.setupWorldForLayer(layer, world);
				
				interactiveRenderer.display(world);
				
				interactiveRenderer.destroy();
			}
			else if (args.getMode() == Mode.CMD)
			{
				// Do this first before we attempt to load any caches
				if (args.eraseOutputDir())
				{
					log.info("Deleting output dir: {}", args.getOutputDir().getAbsolutePath());
					
					FileUtils.deleteDirectory(args.getOutputDir());
				}
				
				tileRenderer = new TileRenderer(args, new CommandLineOutput(), hashAlgorithm);
				
				tileRenderer.output();
			}
			else if (args.getMode() == Mode.VIEWS)
			{
				tileRenderer = new TileRenderer(args, new CommandLineOutput(), hashAlgorithm);
				
				tileRenderer.renderViews();
			}
			else if (args.getMode() == Mode.PLAYERS)
			{
				final Date startTime = new Date();
				
				PlayerSkinCache skinCache = new PlayerSkinCache(args, hashAlgorithm);
				PlayerIconAssembler iconAssembler = new PlayerIconAssembler(skinCache);
				
				for (tectonicus.configuration.Map map : args.getMaps())
				{
					List<Player> players = World.loadPlayers(map.getWorldDir(), skinCache);
									
					File mapDir = new File(args.getOutputDir(), map.getId());
					File playerDir = new File(mapDir, "players.js");
					
					File imagesDir = new File(args.getOutputDir(), "Images");

					OutputResourcesUtil.outputPlayers(playerDir, imagesDir, map, players, iconAssembler);
				}
				
				skinCache.destroy();
				
				final Date endTime = new Date();
				String time = Util.getElapsedTime(startTime, endTime);
				log.debug("Player export took {}", time);
			}
			else if (args.getMode() == Mode.GUI)
			{
				Gui gui = new Gui(hashAlgorithm);
				gui.display();
			}
			else if (args.getMode() == Mode.PROFILE)
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

	public static void main(String[] argArray) throws Exception
	{
		// Init the logging framework
		System.setProperty("logFilename", "log/tectonicus.log");
		log = org.apache.logging.log4j.LogManager.getLogger(TectonicusApp.class);

		//Parse command line to get config file
		MutableConfiguration m = new MutableConfiguration();
		CommandLine cmd = new CommandLine(m);
		cmd.registerConverter(MutableMap.class, s -> new MutableMap("Throwaway"));
		cmd.registerConverter(Level.class, Level::toLevel);
		cmd.setCaseInsensitiveEnumValuesAllowed(true);
		CommandLine.ParseResult parseResult = cmd.parseArgs(argArray);
		if (parseResult.originalArgs().isEmpty()) {
			cmd.usage(cmd.getOut());
			System.exit(cmd.getCommandSpec().exitCodeOnUsageHelp());
		}
		Path configFile = m.getConfigFile();
		
		MutableConfiguration args = new MutableConfiguration();
		if (configFile != null)
		{
			// Load config from xml first
			args = XmlConfigurationParser.parseConfiguration(configFile.toFile());
		}

		// Load config from command line second.  Command line options will override config file options
		CommandLine commandLine = new CommandLine(args);
		commandLine.registerConverter(MutableMap.class, s -> {
			MutableMap map = new MutableMap("Map0");
			MutableLayer layer = new MutableLayer("LayerA", map.getId());
			map.addLayer(layer);
			map.setWorldDir(new File(s));
			return map;
		});
		commandLine.registerConverter(Level.class, Level::toLevel);
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		commandLine.setExecutionStrategy(new CommandLine.RunFirst());
		commandLine.execute(argArray);

		if (commandLine.isUsageHelpRequested() || commandLine.isVersionHelpRequested()) {
			System.exit(commandLine.getCommandSpec().exitCodeOnUsageHelp());
		}

		// Reset log file name
		System.setProperty("logFilename", args.getLogFile());
		Configurator.reconfigure();

		if (args.getMinecraftJar() == null) {
			log.info("No Minecraft jar specified.");
			args.setMinecraftJar(Minecraft.findMinecraftJar());
		}

		if (args.getUpdateToLeaflet() != null) {
			updateToLeaflet(args.getUpdateToLeaflet());
			System.exit(0);
		}

		if (args.getDimension() != null) {
			args.getWorldDir().setDimension(args.getDimension());
		}

		TectonicusApp app = new TectonicusApp(args);

		args.printActive();

		// Workaround for sun bug 6539705 ( http://bugs.sun.com/view_bug.do?bug_id=6539705 )
		// Trigger the load of the awt libraries before we load lwjgl
		Toolkit.getDefaultToolkit();

		if (args.forceLoadAwt())
		{
			System.loadLibrary("awt");
		}

		app.run();
	}

	//TODO: this needs to be updated
	private static void updateToLeaflet(Path renderDir) {
		if (renderDir.resolve("Scripts").toFile().exists()) {
			OutputResourcesUtil.extractMapResources(renderDir.toFile());
			writeUpdatedHtmlFile(renderDir.toFile());
			System.out.println("Finished updating map " + renderDir + " to use Leaflet.");
		} else {
			System.err.println(renderDir + " is not a Tectonicus map render directory.");
		}
		System.exit(1);
	}

	private static void writeUpdatedHtmlFile(final File exportDir) {

		Path htmlFile = null;
		List<String> mapLines = new ArrayList<>();
		try (Stream<Path> htmlFiles = Files.list(exportDir.toPath()).filter(path -> path.toString().endsWith(".html"))) {
			 htmlFile = htmlFiles.findFirst().orElse(Paths.get(""));
			 mapLines = Files.readAllLines(htmlFile).stream().filter(str -> str.contains("Map") && str.contains(".js")
					 && !str.contains("Scripts")).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		File outputHtmlFile = new File(exportDir, htmlFile.getFileName().toString());
		log.info("\twriting html to {}", outputHtmlFile.getAbsolutePath());

		URL url = TectonicusApp.class.getClassLoader().getResource("mapWithSigns.html");
		try (Scanner scanner = new Scanner(url.openStream());
			 PrintWriter writer = new PrintWriter(new FileOutputStream(outputHtmlFile)))
		{

			while (scanner.hasNext())
			{
				String line = scanner.nextLine();
				StringBuilder outLine = new StringBuilder();

				List<Util.Token> tokens = Util.split(line);

				while (!tokens.isEmpty())
				{
					Util.Token first = tokens.remove(0);
					if (first.isReplaceable)
					{
						if (first.value.equals("includes"))
						{
							for (String l : mapLines) {
								outLine.append(l).append("\n");
							}
						}
					}
					else
					{
						outLine.append(first.value);
					}
				}

				writer.write(outLine.append("\n").toString());
			}

			writer.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
