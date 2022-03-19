/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.joml.Vector3f;
import tectonicus.PlayerIconAssembler.WriteIconTask;
import tectonicus.blockTypes.BlockRegistry;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.FileTileCache;
import tectonicus.cache.FileViewCache;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.cache.RegionHashStore;
import tectonicus.cache.TileCache;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.ChestFilter;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Configuration.RenderStyle;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.configuration.PlayerFilter;
import tectonicus.configuration.PortalFilter;
import tectonicus.configuration.SignFilter;
import tectonicus.configuration.ViewFilter;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.raw.BlockEntity;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.LevelDat;
import tectonicus.raw.Player;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.FileUtils;
import tectonicus.util.JsObjectWriter;
import tectonicus.util.TempArea;
import tectonicus.util.Vector2f;
import tectonicus.util.Vector3d;
import tectonicus.util.Vector3l;
import tectonicus.view.ViewRenderer;
import tectonicus.world.Sign;
import tectonicus.world.World;
import tectonicus.world.filter.ExploredCaveFilter;
import tectonicus.world.filter.ExploredCaveFilter113;
import tectonicus.world.filter.NetherBlockFilter;
import tectonicus.world.filter.NetherBlockFilter113;
import tectonicus.world.subset.CircularWorldSubset;
import tectonicus.world.subset.RegionIterator;
import tectonicus.world.subset.WorldSubset;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tectonicus.Version.VERSION_12;
import static tectonicus.Version.VERSION_13;
import static tectonicus.Version.VERSION_16;

@Log4j2
public class TileRenderer
{
	@RequiredArgsConstructor
	public enum Task
	{
		LOADING_WORLD("Loading World"),
		CALCULATING_CHUNK_HASHES("Calculating Chunk Hashes"),
		STARTING_RENDERER("Starting Renderer"),
		FIND_VISIBLE_TILES("Find Visible Tiles"),
		FIND_CHANGED_TILES("Find Changed Tiles"),
		RENDER_BASE_TILES("Render Base Tiles"),
		DOWNSAMPLING("Downsampling"),
		OUTPUT_HTML("Output Html"),
		OUTPUT_CHANGED_LIST("Output Changed List"),
		FINISHED("Finished");

		private final String description;

		@Override
		public String toString() {
			return description;
		}
	}
	
	public static final Color clearColour = new Color(229, 227, 223);
	
	private final Configuration args;
	
	private final MessageDigest hashAlgorithm;
	
	private final int tileWidth;
	private final int tileHeight;
	
	private final int numZoomLevels;
	
	private final File exportDir;
	
	private final PlayerSkinCache playerSkinCache;
	
	private RegionHashStore regionHashStore;
	
	private HddTileListFactory hddTileListFactory;
	
	private OrthoCamera camera;
	
	private Rasteriser rasteriser;
	
	private ChangeFile changedFileList;
	
	private PlayerIconAssembler playerIconAssembler;
	
	private final ProgressListener progressListener;
	
	private MemoryMonitor memoryMonitor;
	
	private boolean abort;
	
	public TileRenderer(Configuration args, ProgressListener listener, MessageDigest hashAlgorithm) throws Exception
	{
		this.args = args;
		this.hashAlgorithm = hashAlgorithm;
		
		this.progressListener = listener;
		
		this.tileWidth = args.getTileSize();
		this.tileHeight = args.getTileSize();
		
		this.numZoomLevels = args.getNumZoomLevels();
		
		this.exportDir = args.getOutputDir();
		
		playerSkinCache = new PlayerSkinCache(args, hashAlgorithm);
		
		hddTileListFactory = new HddTileListFactory( new File(args.getCacheDir(), "tileLists") );
		
		log.debug("Creating player icon assembler");
		playerIconAssembler = new PlayerIconAssembler(playerSkinCache);
		
		memoryMonitor = new MemoryMonitor();

		log.info("Initialising display...");

		DisplayType type = DisplayType.OFFSCREEN;
		if (args.isUseEGL()) {
			type = DisplayType.OFFSCREEN_EGL;
		}

		rasteriser = RasteriserFactory.createRasteriser(args.getRasteriserType(), type, 2048, 2048, args.getColourDepth(), args.getAlphaBits(), 24, args.getNumSamples());

		if (rasteriser != null)
		{
			log.debug("Using rasteriser: {}", rasteriser);
			rasteriser.printInfo();
		}
		else
		{
			throw new RuntimeException("Could not create drawing surface");
		}
		
		log.info("Creating camera");
		camera = new OrthoCamera(rasteriser, tileWidth, tileHeight);
		
		log.info("TileRenderer init complete");
	}
	
	public void destroy()
	{
		log.info("Cleaning up...");
		
		rasteriser.destroy();
		
		playerSkinCache.destroy();
	}
	
	public void abort()
	{
		this.abort = true;
	}
	
	public Rasteriser getRasteriser()
	{
		return rasteriser;
	}
	
	public Result output()
	{
		progressListener.onTaskStarted(Task.STARTING_RENDERER.toString());
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(args.getCacheDir());
		
		TempArea tempArea = new TempArea( new File(args.getCacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(args.getOutputDir(), "changed.txt"));
		
		for (tectonicus.configuration.Map map : args.getMaps())
		{
			// Clear shared state?
			// ..
			
			// Clear region cache?
			// ..
			
			File mapDir = new File(exportDir, map.getId());
			FileUtils.ensureExists(mapDir);
			
			BiomeCache biomeCache = CacheUtil.createBiomeCache(args.minecraftJar(), args.getCacheDir(), map, hashAlgorithm);

			// Create the world for this map
			World world = new World(rasteriser, map.getWorldDir(), map.getDimension(), args.minecraftJar(), args.getTexturePack(), map.getModJars(),
									biomeCache, hashAlgorithm, args.getSinglePlayerName(), map.getWorldSubset(), playerSkinCache, map.getSignFilter(), args);
			
			// Setup camera
			setupInitialCamera(map);
			
			WorldVectors worldVectors = calcWorldVectors();
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");
			
			WorldStats worldStats = preProcess(world, map.getDimension(), map.getSignFilter(), map.getPortalFilter(), map.getViewFilter(), map.getChestFilter(), portalsFile, signsFile, viewsFile);
			
			// Find visible tiles
			HddTileList visibleTiles = findVisibleTiles(world, camera, worldStats.numChunks());
			
			
			// Figure out which tiles we need to render
			progressListener.onTaskStarted(Task.FIND_CHANGED_TILES.toString());

			// Output entity javascript for creating map markers
			//TODO: move all the code from these methods to a separate class
			outputSigns(new File(mapDir, "signs.js"), signsFile, map);
			outputPlayers(new File(mapDir, "players.js"), new File(exportDir, "Images/PlayerIcons/"), map, world.players(map.getDimension()), playerIconAssembler);
			outputBeds(mapDir, map, world.players(null));
			outputRespawnAnchors(mapDir, map, world.players(null));
			worldStats.setNumPortals((outputPortals(new File(mapDir, "portals.js"), portalsFile, map)));
			outputViews(new File(mapDir, "views.js"), viewsFile, map);
			outputChests(new File(mapDir, "chests.js"), map, world.getChests());

			// Render views
			FileViewCache viewCache = CacheUtil.createViewCache(args.getCacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, args.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
			
			TileCoordBounds bounds = null;
			
			if (map.getLayers().isEmpty())
				log.warn("No layers found!!!");
			
			for (Layer layer : map.getLayers())
			{
				// Setup per-layer config
				setupWorldForLayer(layer, world);
				
				// Set new tile cache for this layer
				String optionString = FileTileCache.calcOptionsString(args);
				TileCache tileCache = CacheUtil.createTileCache(args.useCache(), optionString, layer.getImageFormat(), args.getCacheDir(), map, layer, hashAlgorithm);
			
				File baseTilesDir = DirUtils.getZoomDir(exportDir, layer, numZoomLevels);
				FileUtils.ensureExists(baseTilesDir);

				// Find changed tiles
				HddTileList changedTiles = tileCache.findChangedTiles(hddTileListFactory, visibleTiles, regionHashStore, world, map, camera, map.getClosestZoomSize(), tileWidth, tileHeight, baseTilesDir);
				
				// Trim changed tiles to size
				changedTiles = trimTileList(changedTiles, args.getMaxTiles());
				
				// Render base tiles
				renderBaseTiles(world, map, layer, baseTilesDir, changedTiles, tileCache);
				
				// Create downsampled layers
				bounds = downsample(changedTiles, exportDir, layer, baseTilesDir, tileCache);
			}
			
			outputIcons(map, world);
			
			// Output world stats
			worldStats.outputBlockStats(new File(mapDir, "blockStats.js"), map.getId(), world.getBlockTypeRegistry());
			worldStats.outputWorldStats(new File(mapDir, "worldStats.js"), map.getId());
			
			// Output world vectors for this camera config
			outputWorldVectors( new File(mapDir, "worldVectors.js"), map.getId(), worldVectors, bounds, world.getLevelDat(), worldStats.numChunks(), world.numPlayers(), map);
		}
		
		// Output html resources
		// TODO: Should only load texture pack once and share between this and world loading
		outputHtmlResources( new TexturePack(rasteriser, args.minecraftJar(), args.getTexturePack(), args.getMap(0).getModJars(), args), playerIconAssembler, args.getDefaultSkin());
		
		outputContents(new File(new File(exportDir, "Scripts"), "contents.js"), args);
		
		
		// Output html
		File outputHtmlFile = null;
		try {
			outputHtmlFile = outputHtml();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ----
		
		Date endTime = new Date();
		String time = Util.getElapsedTime(startTime, endTime);
		
		outputRenderStats(time);
		
		outputChangedFile();
		
		log.info("Render complete - total time "+time);
		
		return new Result(abort, outputHtmlFile);
	}
	
	// Just renders views
	public Result outputViews()
	{
		progressListener.onTaskStarted(Task.STARTING_RENDERER.toString());
		log.info("Starting view renderer");
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(args.getCacheDir());
		
		TempArea tempArea = new TempArea( new File(args.getCacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(args.getOutputDir(), "changed.txt"));
		
		for (tectonicus.configuration.Map map : args.getMaps())
		{
			File mapDir = new File(exportDir, map.getId());
			FileUtils.ensureExists(mapDir);
			
			BiomeCache biomeCache = CacheUtil.createBiomeCache(args.minecraftJar(), args.getCacheDir(), map, hashAlgorithm);

			// Create the world for this map
			World world = new World(rasteriser, map.getWorldDir(), map.getDimension(), args.minecraftJar(), args.getTexturePack(), map.getModJars(),
									biomeCache, hashAlgorithm, args.getSinglePlayerName(), map.getWorldSubset(), playerSkinCache, map.getSignFilter(), args);
			
			// TODO: Load custom blocks here
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");
			
			preProcess(world, map.getDimension(), map.getSignFilter(), map.getPortalFilter(), map.getViewFilter(), map.getChestFilter(), portalsFile, signsFile, viewsFile);
			
			// Output views
			outputViews(new File(mapDir, "views.js"), viewsFile, map);
			
			// Render views
			FileViewCache viewCache = CacheUtil.createViewCache(args.getCacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, args.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
		}
		
		Date endTime = new Date();
		String time = Util.getElapsedTime(startTime, endTime);
		
		outputChangedFile();
		
		log.info("View render complete - total time "+time);
		
		return new Result(abort, null);
	}
	
	public static void setupWorldForLayer(Layer layer, World world)
	{
		log.info("Creating block registry");
		world.loadBlockRegistry(layer.getCustomBlockConfig(), layer.useDefaultBlockConfig());
		
		world.setLightStyle(layer.getLightStyle());
		world.setDefaultBlockId(BlockIds.AIR);
		world.setDefaultBlockName(Block.AIR.getName());

		if (layer.getRenderStyle() == RenderStyle.CAVE) {
			if (world.getTexturePack().getVersion().getNumVersion() < VERSION_13.getNumVersion()) {
				world.setDefaultBlockId(BlockIds.STONE);
				world.setBlockMaskFactory(new CaveMaskFactory());
			} else {
				world.setDefaultBlockName(Block.STONE.getName());
				world.setBlockMaskFactory(new CaveMaskFactory113());
			}
		} else if (layer.getRenderStyle() == RenderStyle.EXPLORED_CAVES) {
			if (world.getTexturePack().getVersion().getNumVersion() < VERSION_13.getNumVersion()) {
				world.setBlockFilter(new ExploredCaveFilter());
				world.setBlockMaskFactory(new CaveMaskFactory());
			} else {
				world.setBlockFilter(new ExploredCaveFilter113());
				world.setBlockMaskFactory(new CaveMaskFactory113());
			}
		} else if (layer.getRenderStyle() == RenderStyle.NETHER) {
			if (world.getTexturePack().getVersion().getNumVersion() < VERSION_13.getNumVersion()) {
				world.setBlockFilter(new NetherBlockFilter());
			} else {
				world.setBlockFilter(new NetherBlockFilter113());
			}
		}	
	}

	private WorldStats preProcess(World world, Dimension dimension, SignFilter signFilter, PortalFilter portalFilter, ViewFilter viewFilter, ChestFilter chestFilter, File portalsFile, File signsFile, File viewsFile)
	{
		WorldStats stats = null;
		
		HddObjectListWriter<Portal> portals = null;
		HddObjectListWriter<Sign> signs = null;
		HddObjectListWriter<Sign> views = null;
		
		try
		{
			portals = new HddObjectListWriter<>(portalsFile, true);
			signs = new HddObjectListWriter<>(signsFile, true);
			views = new HddObjectListWriter<>(viewsFile, true);
			
			stats = preProcess(world, signFilter, portalFilter, viewFilter, chestFilter, portals, signs, views);
			
			log.debug("Found "+views.size()+" views");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (portals != null)
				portals.close();
			if (signs != null)
				signs.close();
			if (views != null)
				views.close();
		}
		
		final int numPlayers = world.players(dimension).size();
		stats.setNumPlayers(numPlayers);
		
		return stats;
	}
	
	private WorldStats preProcess(World world, SignFilter signFilter, PortalFilter portalFilter, ViewFilter viewFilter, ChestFilter chestFilter, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs, HddObjectListWriter<Sign> views)
	{
		// Pre-render pass - calc chunk hashes and project signs
		if (progressListener != null)
			progressListener.onTaskStarted(Task.CALCULATING_CHUNK_HASHES.toString());
		
		WorldStats worldStats = new WorldStats();
		
		regionHashStore = new RegionHashStore(args.getCacheDir());
		
		log.info("Discovering chunks...");
		//	Iterate over regions, then over chunks
		//		hash each chunk and store in region hashes file
		//		gather world stats and signs for each chunk
		
		RegionIterator it = world.createRegionIterator();
		
		log.debug("Looking for chunks in "+it.getBaseDir().getAbsolutePath());
		final Date beginTime = new Date();
		while (it.hasNext())
		{
			File regionFile = it.next();
			if (regionFile != null)
			{
				Region region = null;
				try
				{
					region = new Region(regionFile);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				if (region != null)
				{
					// For every region...
					
					regionHashStore.startRegion(region.getRegionCoord());
					
					ChunkCoord[] chunkCoords = region.getContainedChunks();
					for (ChunkCoord coord : chunkCoords)
					{
						// For every chunk coord...
						
						Chunk c = region.loadChunk(coord, world.getBiomeCache(), world.getBlockFilter(), worldStats, world.getWorldInfo());
						if (c != null)
						{
							c.calculateHash(hashAlgorithm);
							regionHashStore.addHash(c.getCoord(), c.getHash());
							
							worldStats.incNumChunks();
							
							findSigns(c.getRawChunk(), signs, signFilter);

							if (Minecraft.getWorldVersion() < 13) {
								findPortalsOld(c.getRawChunk(), portals, portalFilter, worldStats);
							} else {
								findPortals(c.getRawChunk(), portals, portalFilter, worldStats);
							}
							
							findViews(c.getRawChunk(), views, viewFilter);
							
							findChests(c.getRawChunk(), chestFilter, world.getChests());
							
							if (worldStats.numChunks() % 100 == 0) {
								System.out.print("\tfound " + worldStats.numChunks() + " chunks so far\r"); //prints a carriage return after line
								log.trace("found {} chunks so far", worldStats.numChunks());
							}
						}
					}
					
					regionHashStore.endRegion();
				}
			}
		}
		
		final Date endTime = new Date();
		final String searchTime = Util.getElapsedTime(beginTime, endTime);
		
		log.debug("\nFound "+worldStats.numChunks()+" chunks in total");
		log.debug("Chunk search took: " + searchTime);
		
		if (worldStats.numChunks() == 0)
		{
			// Uh oh, didn't find any chunks!
			// Print some debugging info to help people figure out what they're doing wrong
			
			log.error("Failed to find any chunks!");
			log.error("Contents of "+it.getBaseDir().getAbsolutePath());
			File[] contents = it.getBaseDir().listFiles();
			if (contents != null)
			{
				for (File f : contents)
				{
					log.error("\t"+f.getName());
				}
			}
		}
		
		return worldStats;
	}
	
	private static void findSigns(RawChunk chunk, HddObjectListWriter<Sign> signs, SignFilter filter)
	{
		try
		{
			for (SignEntity s : chunk.getSigns().values())
			{
				if (passesFilter(s, filter))
				{
					Sign sign = new Sign(s);
					signs.add(sign);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void findPortalsOld(RawChunk chunk, HddObjectListWriter<Portal> portals, PortalFilter filter, WorldStats stats)
	{
		try
		{			
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int y=1; y<Minecraft.getChunkHeight()-1; y++)
				{
					for (int z=0; z<RawChunk.DEPTH; z++)
					{
						final int id = chunk.getBlockId(x, y, z);
						final int above = chunk.getBlockId(x, y+1, z);
						int below = chunk.getBlockId(x, y-1, z);
						
						if (id == BlockIds.PORTAL && above != BlockIds.PORTAL) //Find vertical center portal blocks
						{
							ChunkCoord coord = chunk.getChunkCoord();

							int tempY = y;
							while (below == BlockIds.PORTAL)
							{
								tempY -= 1;
								below = chunk.getBlockId(x, tempY, z);
							}
							
							Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + x,
														y-Math.round((y-(tempY+1))/2),
														coord.z * RawChunk.DEPTH + z);
							
							if (filter.passesFilter())
							{
								portals.add( new Portal(pos.x, pos.y, pos.z) );
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void findPortals(RawChunk chunk, HddObjectListWriter<Portal> portals, PortalFilter filter, WorldStats stats)
	{
		try
		{
			String netherPortalName = Block.NETHER_PORTAL.getName();

			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int y=1; y<Minecraft.getChunkHeight()-1; y++)
				{
					for (int z=0; z<RawChunk.DEPTH; z++)
					{
						String id = chunk.getBlockName(x, y, z);
						String above = chunk.getBlockName(x, y+1, z);
						String below = chunk.getBlockName(x, y-1, z);

						if (id != null && id.equals(netherPortalName) && !above.equals(netherPortalName)) //Find vertical center portal blocks
						{
							ChunkCoord coord = chunk.getChunkCoord();

							int tempY = y;
							while (below.equals(netherPortalName))
							{
								tempY -= 1;
								below = chunk.getBlockName(x, tempY, z);
							}

							// For 1.18 and higher we need to subtract 64 from the y value to get the actual y value
							long finalY;
							if (Minecraft.getChunkHeight() > 256) {
								finalY = (y-(y-(tempY+1))/2) - 64L;
							} else {
								finalY = y-(y-(tempY+1))/2L;
							}

							Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + x,
									finalY,
									coord.z * RawChunk.DEPTH + z);

							if (filter.passesFilter())
							{
								portals.add( new Portal(pos.x, pos.y, pos.z) );
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void findViews(RawChunk chunk, HddObjectListWriter<Sign> views, ViewFilter filter)
	{
		try
		{
			for (SignEntity s : chunk.getSigns().values())
			{
				if (filter.passesFilter(s))
				{
					Sign sign = new Sign(s);
					views.add(sign);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void findChests(RawChunk chunk, ChestFilter filter, List<ContainerEntity> chests)
	{
		try
		{
			for (ContainerEntity entity : chunk.getChests())
			{
				if (filter.passesFilter(entity.isUnopenedContainer()))
				{
					chests.add(entity);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void renderBaseTiles(World world, tectonicus.configuration.Map map, Layer layer, File layerDir, HddTileList tiles, TileCache tileCache)
	{
		if (abort)
			return;
		
		progressListener.onTaskStarted(Task.RENDER_BASE_TILES.toString());
		
		final int zoom = map.getClosestZoomSize();
		final ImageFormat imageFormat = layer.getImageFormat();
		
		log.debug("Base render is at zoom "+zoom+" with "+tileWidth+"x"+tileHeight+" tiles");
		
		setupInitialCamera(map);
		
		int done = 0;

		ImageWriteQueue imageWriteQueue = new ImageWriteQueue(args.getNumDownsampleThreads());

		for (TileCoord t : tiles) {
			System.out.print("Rendering tile @ " + t.x + "," + t.y + " (tile " + (done + 1) + " of " + tiles.size() + ")\r"); //prints a carriage return after line
			log.trace("Rendering tile @ {},{} (tile {} of {})", t.x, t.y, done+1, tiles.size());
			progressListener.onTaskUpdate(done, tiles.size());

			setupCameraForTile(camera, t, tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), zoom);

			rasteriser.resetState();
			rasteriser.clear(layer.getBackgroundColorRGB());

			world.draw(camera, false, true);

			File outputFile = getImageFile(layerDir, t.x, t.y, imageFormat);
			BufferedImage tileImage = rasteriser.takeScreenshot(0, 0, tileWidth, tileHeight, imageFormat);
			if (tileImage != null) {
				imageWriteQueue.write(outputFile, tileImage, imageFormat, layer.getImageCompressionLevel());
			} else {
				log.error("Error: Rasteriser.takeScreenshot gave us a null image (width:" + tileWidth + " height:" + tileHeight + " format:" + imageFormat + ")");
			}

			tileCache.writeImageCache(t);

			changedFileList.writeLine(outputFile.getAbsolutePath());

			done++;

			if (abort)
				break;
		}

		imageWriteQueue.waitUntilFinished();
		tileCache.closeTileCache();
		
		log.info("\nBase tile render complete");
	}

	public static void setupCameraForTile(OrthoCamera camera, TileCoord tile, final int tileWidth, final int tileHeight, final float cameraAngleRads, final float cameraElevationRads, final int zoom)
	{
		Point lookAt = tileToScreen(tile, tileWidth, tileHeight);
		
		// Adjust so that we're looking at the center of the tile
		lookAt.x += tileWidth / 2;
		lookAt.y += tileHeight / 2;
		
		// Use up/right to slide camera to correct world pos
		
		Vector3f up = camera.getUp();
		Vector3f right = camera.getRight();
		
		Vector3f cameraPos = new Vector3f(0, 0, 0);
		cameraPos.x += right.x * camera.getVisibleWorldWidth() * tile.x;
		cameraPos.y += right.y * camera.getVisibleWorldWidth() * tile.x;
		cameraPos.z += right.z * camera.getVisibleWorldWidth() * tile.x;
		
		cameraPos.x -= up.x * camera.getVisibleWorldHeight() * tile.y;
		cameraPos.y -= up.y * camera.getVisibleWorldHeight() * tile.y;
		cameraPos.z -= up.z * camera.getVisibleWorldHeight() * tile.y;
		
		camera.lookAt(cameraPos.x, cameraPos.y, cameraPos.z, zoom, cameraAngleRads, cameraElevationRads);

		// Correct the height so the terrain lies between the near and far planes
		{
			Vector3f forward = camera.getForward(); 
			
			final float inc = 400;
			
			// March the camera backwards until it's above the ground
			while (cameraPos.y < 128)
			{
				cameraPos.x -= forward.x * inc;
				cameraPos.y -= forward.y * inc;
				cameraPos.z -= forward.z * inc;
			}
			
			// Move the camera forwards so it's close-ish to the ground
			while (cameraPos.y > 512)
			{
				cameraPos.x += forward.x * inc;
				cameraPos.y += forward.y * inc;
				cameraPos.z += forward.z * inc;
			}
			
			camera.lookAt(cameraPos.x, cameraPos.y, cameraPos.z, zoom, cameraAngleRads, cameraElevationRads);
		}
		
		camera.apply();
	}
	
	private void setupInitialCamera(tectonicus.configuration.Map map)
	{
		final int zoom = map.getClosestZoomSize();
		
		camera.lookAt(0, 0, 0, zoom, map.getCameraAngleRad(), map.getCameraElevationRad());
		camera.apply();
		setupCameraForTile(camera, new TileCoord(0, 0), tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), map.getClosestZoomSize());
	}
	
	private HddTileList findVisibleTiles(World world, OrthoCamera camera, final int numChunks)
	{
		HddTileList visible = hddTileListFactory.createList();
		
		if (abort)
			return visible;

		progressListener.onTaskStarted(Task.FIND_VISIBLE_TILES.toString());
		
		// Method:
		//	for each chunk:
		//		- find corner vertices in world space
		//		- project into screen space
		//		- keep screen min / max in both axies, convert to screen aligned rectangle containing chunk
		//		- loop over screen rect and generate all tiles which it intersects
		
		int count = 0;
		
		RegionIterator it = world.createRegionIterator();
		while (it.hasNext())
		{
			File regionFile = it.next();
			if (regionFile == null)
				continue;
			
			Region region = null;
			try
			{
				region = new Region(regionFile);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			if (region != null)
			{
				ChunkCoord[] chunkCoords = region.getContainedChunks();
				for (ChunkCoord coord : chunkCoords)
				{
					if (coord != null && world.contains(coord))
					{
						BoundingBox bounds = new BoundingBox(new Vector3f(coord.x * RawChunk.WIDTH, 0, coord.z * RawChunk.DEPTH), RawChunk.WIDTH, Minecraft.getChunkHeight(), RawChunk.DEPTH);
						ArrayList<Vector3f> cornerPoints = bounds.getCornerPoints();
						
						int minX = Integer.MAX_VALUE;
						int maxX = Integer.MIN_VALUE;
						int minY = Integer.MAX_VALUE;
						int maxY = Integer.MIN_VALUE;
						
						// Project corners to find screen rect
						for (Vector3f corner : cornerPoints)
						{
							Point screenPos = camera.project(corner);
							
							minX = Math.min(screenPos.x, minX);
							maxX = Math.max(screenPos.x, maxX);
							
							minY = Math.min(screenPos.y, minY);
							maxY = Math.max(screenPos.y, maxY);
						}
						
						// Find tiles that screen rect overlaps
						for (int x=minX; x<=maxX+tileWidth; x+=tileWidth)
						{
							for (int y=minY; y<=maxY+tileHeight; y+=tileHeight)
							{
								TileCoord tile = screenToTile( new Point(x, y) );
								visible.add(tile);						
							}
						}
						
						count++;
						if (count % 100 == 0)
						{
							final int percentage = (int)Math.floor((count / (float)numChunks) * 100);
							System.out.print(percentage+"%\r"); //prints a carriage return after line
						}
						progressListener.onTaskUpdate(count, numChunks);
					}
				}
			}
			
			if (abort)
				break;
		}
		System.out.println("100%");
		
		log.info("found {} total tiles to output", visible.size());
		
		return visible;
	}
	
	private HddTileList trimTileList(HddTileList inTiles, final int maxTiles)
	{
		if (maxTiles > 0)
		{
			HddTileList trimmedTiles = hddTileListFactory.createList();
			int count = 0;
			for (TileCoord c : inTiles)
			{
				trimmedTiles.add(c);
				count++;
				if (count > maxTiles)
					break;
			}
			return trimmedTiles;
		}
		else
		{
			return inTiles;
		}
	}
	
	private TileCoordBounds downsample(HddTileList baseTiles, File exportDir, Layer layer, File baseDir, TileCache tileCache)
	{
		final Date downsampleStart = new Date();
		
		int zoomLevel = args.getNumZoomLevels() - 1;
		
		File prevDir = baseDir;
		HddTileList prevTiles = baseTiles;
		while (zoomLevel >= 0)
		{
			if (abort)
				break;

			progressListener.onTaskStarted(Task.DOWNSAMPLING + " zoom level " + zoomLevel);
			
			HddTileList nextTiles = findNextZoomTiles(prevTiles, hddTileListFactory);
			File nextDir = DirUtils.getZoomDir(exportDir, layer, zoomLevel);
			if (!tileCache.isUsingExistingCache())
			{
				FileUtils.deleteDirectory(nextDir);
			}
			if (!nextDir.exists())
			{
				final boolean mkOk = nextDir.mkdirs();
				if (!mkOk)
					throw new RuntimeException("Couldn't create dir:"+nextDir.getAbsolutePath());
			}
			
			log.debug("\tDownsampling {} tiles into {} tiles", prevTiles.size(), nextTiles.size());
			
			Downsampler downsampler = new Downsampler(args.getNumDownsampleThreads(), changedFileList);
			downsampler.downsample(prevDir, nextDir, nextTiles, layer, tileWidth, tileHeight, progressListener);
			
			zoomLevel--;
			prevDir = nextDir;
			prevTiles = nextTiles;
		}
		
		final Date downsampleEnd = new Date();
		final String downsampleTime = Util.getElapsedTime(downsampleStart, downsampleEnd);
		log.debug("Downsampling took "+downsampleTime);
		
		return new TileCoordBounds(prevTiles.getAbsoluteMinCoord(), prevTiles.getAbsoluteMaxCoord());
	}

	private static class TileCoordBounds
	{
		public TileCoord min;
		public TileCoord max;
		
		public TileCoordBounds(TileCoord min, TileCoord max)
		{
			this.min = min;
			this.max = max;
		}
	}
	
	private static HddTileList findNextZoomTiles(HddTileList baseTiles, HddTileListFactory factory) {
		HddTileList result = factory.createList();
		
		for (TileCoord c : baseTiles)
		{
			final int x = (int)Math.floor(c.x / 2.0f);
			final int y = (int)Math.floor(c.y / 2.0f);
			result.add( new TileCoord(x, y) );
		}
		
		return result;
	}

	
	
	
	private void outputRenderStats(final String timeTaken)
	{
		File statsFile = new File(new File(exportDir, "Scripts"), "stats.js");
		try {
			Files.deleteIfExists(statsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting stats to {}", statsFile.getAbsolutePath());
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm z");
		final String renderedDateStr = dateFormat.format( new Date() );
		final String renderedTimeStr = timeFormat.format( new Date() );
		
		JsObjectWriter jsWriter = null;
		try
		{
			jsWriter = new JsObjectWriter(statsFile);
			
			Map<String, Object> stats = new HashMap<>();
			
			stats.put("tectonicusVersion", BuildInfo.getVersion());
			
			stats.put("renderTime", timeTaken);
			stats.put("renderedOnDate", renderedDateStr);
			stats.put("renderedOnTime", renderedTimeStr);
			stats.put("peakMemoryBytes", memoryMonitor.getPeakMemory());
			
			jsWriter.write("stats", stats);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jsWriter != null)
				jsWriter.close();
		}
	}
	
	private File outputHtml() throws IOException {
		progressListener.onTaskStarted(Task.OUTPUT_HTML.toString());
		
		File outputHtmlFile = new File(exportDir, args.getOutputHtmlName());
		log.info("Writing html to {}", outputHtmlFile.getAbsolutePath());
		
		URL url = getClass().getClassLoader().getResource("mapWithSigns.html");
		if (url == null)
			throw new IOException("resource not found");
		try (Scanner scanner = new Scanner(url.openStream());
			 PrintWriter writer = new PrintWriter(new FileOutputStream(outputHtmlFile)))
		{
			while (scanner.hasNext())
			{
				String line = scanner.nextLine();
				StringBuilder outLine = new StringBuilder();
				
				ArrayList<Util.Token> tokens = Util.split(line);
				
				while (!tokens.isEmpty())
				{
					Util.Token first = tokens.remove(0);
					if (first.isReplaceable)
					{
						if (first.value.equals("includes"))
						{
							String templateStart = "		<script src=\"";
							String templateEnd = "\"></script>\n";
							
							for (tectonicus.configuration.Map map : args.getMaps())
							{
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/players.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/beds.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/respawnAnchors.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/portals.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/signs.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/views.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/chests.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/worldVectors.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/blockStats.js");
								outLine.append(templateEnd);
								
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/worldStats.js");
								outLine.append(templateEnd);
								
								// Any per layer includes?
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
		
		return outputHtmlFile;
	}
	
	private void outputMergedJs(File outFile, ArrayList<String> inputResources)
	{
		InputStream in = null;
		final int scale = (int)Math.pow(2, numZoomLevels);
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(outFile)))
		{
			for (String res : inputResources)
			{
				in = TileRenderer.class.getClassLoader().getResourceAsStream(res);
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				
				String line;
				while ((line = reader.readLine()) != null)
				{
					StringBuilder outLine = new StringBuilder();
					
					ArrayList<Util.Token> tokens = Util.split(line);
					
					while (!tokens.isEmpty())
					{
						Util.Token first = tokens.remove(0);
						if (first.isReplaceable)
						{
							if (first.value.equals("tileWidth"))
							{
								outLine.append(tileWidth);
							}
							else if (first.value.equals("tileHeight"))
							{
								outLine.append(tileHeight);
							}
							else if (first.value.equals("maxZoom"))
							{
								outLine.append(numZoomLevels);
							}
							else if (first.value.equals("mapCoordScaleFactor"))
							{
								outLine.append(scale);
								outLine.append(".0"); // Append .0 so that it's treated as float in the javascript
							}
							else if (first.value.equals("showSpawn"))
							{
								outLine.append(args.showSpawn());
							}
							else if (first.value.equals("signsInitiallyVisible"))
							{
								outLine.append(args.areSignsInitiallyVisible());
							}
							else if (first.value.equals("playersInitiallyVisible"))
							{
								outLine.append(args.arePlayersInitiallyVisible());
							}
							else if (first.value.equals("portalsInitiallyVisible"))
							{
								outLine.append(args.arePortalsInitiallyVisible());
							}
							else if (first.value.equals("bedsInitiallyVisible"))
							{
								outLine.append(args.areBedsInitiallyVisible());
							}
							else if (first.value.equals("respawnAnchorsInitiallyVisible"))
							{
								outLine.append(args.areRespawnAnchorsInitiallyVisible());
							}
							else if (first.value.equals("spawnInitiallyVisible"))
							{
								outLine.append(args.isSpawnInitiallyVisible());
							}
							else if (first.value.equals("viewsInitiallyVisible"))
							{
								outLine.append(args.areViewsInitiallyVisible());
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (in != null) {
					in.close();
				}
			}
			catch (Exception e) {}
		}
	}
	
	private void outputWorldVectors(File vectorsFile, String varNamePrefix, WorldVectors worldVectors, TileCoordBounds bounds, LevelDat levelDat, final int numChunks, final int numPlayers, tectonicus.configuration.Map map)
	{
		try {
			Files.deleteIfExists(vectorsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting world vectors to {}", vectorsFile.getAbsolutePath());
		
		final int scale = (int)Math.pow(2, numZoomLevels);
		
		final int surfaceAreaM = numChunks * RawChunk.WIDTH * RawChunk.DEPTH;
		final DecimalFormat formatter = new DecimalFormat("####.#");
		final String surfaceArea = formatter.format(surfaceAreaM / 1000.0f / 1000.0f);
		
		
		JsonWriter json = null;
		try
		{
			json = new JsonWriter(vectorsFile);
			
			json.startObject(varNamePrefix+"_worldVectors");
			
			// World name and size 
			json.writeVariable("worldName", levelDat.getWorldName());
			json.writeVariable("worldSizeInBytes", levelDat.getSizeOnDisk());
			json.writeVariable("numChunks", numChunks);
			json.writeVariable("surfaceArea", surfaceArea);
			json.writeVariable("numPlayers", numPlayers);

			Vector3l spawnPos = levelDat.getSpawnPosition();
			if (map.getWorldSubset().containsBlock(spawnPos.x, spawnPos.z)) {
				// Spawn point
				json.writeWorldCoord("spawnPosition", levelDat.getSpawnPosition());
			}
			
			// Start view
			
			Vector3l startView = new Vector3l();
			
			if (map.getWorldSubset() instanceof CircularWorldSubset) {
				CircularWorldSubset subset = (CircularWorldSubset) map.getWorldSubset();

				startView.x = subset.getOrigin().x;
				startView.y = 64;  //sea level
				startView.z = subset.getOrigin().z;
			} else {
				startView=levelDat.getSpawnPosition();
			}
			
			json.writeWorldCoord("startView", startView);
						
			Vector2f origin = new Vector2f();
			origin.x = (worldVectors.origin.x / scale);
			origin.y = (worldVectors.origin.y / scale);
			json.writeMapsPoint("origin", origin);
			
			// Axes
			Vector2f xAxis = new Vector2f(worldVectors.xAxis.x / scale, worldVectors.xAxis.y / scale);
			json.writeMapsPoint("xAxis", xAxis);
			
			Vector2f yAxis = new Vector2f(worldVectors.yAxis.x / scale, worldVectors.yAxis.y / scale);
			json.writeMapsPoint("yAxis", yAxis);
			
			Vector2f zAxis = new Vector2f(worldVectors.zAxis.x / scale, worldVectors.zAxis.y / scale);
			json.writeMapsPoint("zAxis", zAxis);
			
			// Units
			Vector2f mapXUnit = new Vector2f(worldVectors.mapXUnit.x * scale, worldVectors.mapXUnit.y * scale);
			json.writeMapsPoint("mapXUnit", mapXUnit);
			
			Vector2f mapYUnit = new Vector2f(worldVectors.mapYUnit.x * scale, worldVectors.mapYUnit.y * scale);
			json.writeMapsPoint("mapYUnit", mapYUnit);
			
			// Min and max bounds
			final long mapXMin = bounds.min.x * tileWidth;
			final long mapYMin = bounds.min.y * tileHeight;
			
			final long mapXMax = bounds.max.x * tileWidth + tileWidth;
			final long mapYMax = bounds.max.y * tileHeight + tileHeight;
			
			final long mapWidth = (mapXMax - mapXMin);
			final long mapHeight = (mapYMax - mapYMin);
			
			json.writeMapsPoint("mapMin", mapXMin, mapYMin);
			
			json.writeMapsPoint("mapSize", +mapWidth, mapHeight);

			json.writeVariable("yOffset", worldVectors.yOffset);

			json.endObject();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (json != null)
				json.close();
		}
	}
	
	private void outputContents(File outputFile, Configuration config)
	{
		try {
			Files.deleteIfExists(outputFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Writing master contents to {}", outputFile.getAbsolutePath());

		try (PrintWriter writer = new PrintWriter(outputFile))
		{
			writer.println("tileSize = "+config.getTileSize()+";");
			writer.println("maxZoom = "+config.getNumZoomLevels()+";");
			writer.println();
			
			writer.println("var contents = ");
			writer.println("[");
			
			List<tectonicus.configuration.Map> maps = config.getMaps();
			for (int i=0; i<maps.size(); i++)
			{
				tectonicus.configuration.Map m = maps.get(i);
				
				writer.println("\t{");
				
				writer.println("\t\tid: \""+m.getId()+"\",");
				writer.println("\t\tname: \""+m.getName()+"\",");
				writer.println("\t\tplayers: "+m.getId()+"_playerData,");
				writer.println("\t\tbeds: "+m.getId()+"_bedData,");
				writer.println("\t\trespawnAnchors: "+m.getId()+"_respawnAnchorData,");
				writer.println("\t\tsigns: "+m.getId()+"_signData,");
				writer.println("\t\tportals: "+m.getId()+"_portalData,");
				writer.println("\t\tviews: "+m.getId()+"_viewData,");
				writer.println("\t\tchests: "+m.getId()+"_chestData,");
				writer.println("\t\tblockStats: "+m.getId()+"_blockStats,");
				writer.println("\t\tworldStats: "+m.getId()+"_worldStats,");
				writer.println("\t\tworldVectors: "+m.getId()+"_worldVectors,");
				
				writer.println("\t\tlayers:");
				writer.println("\t\t[");
				for (int j=0; j<m.numLayers(); j++)
				{
					Layer l = m.getLayer(j);
					
					writer.println("\t\t\t{");
					
					writer.println("\t\t\t\tid: \""+l.getId()+"\",");
					writer.println("\t\t\t\tname: \""+l.getName()+"\",");
					
					writer.println("\t\t\t\tbackgroundColor: \""+l.getBackgroundColor()+"\",");
					writer.println("\t\t\t\timageFormat: \""+l.getImageFormat().getExtension()+"\",");
					writer.println("\t\t\t\tisPng: \""+l.getImageFormat().isPng()+"\"");
					
					if (j < m.numLayers()-1)
						writer.println("\t\t\t},");
					else
						writer.println("\t\t\t}");
				}
				writer.println("\t\t]");
				
				if (i < maps.size()-1)
					writer.println("\t},");
				else
					writer.println("\t}");
			}
			
			writer.println("]");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void outputPlayers(File playersFile, File imagesDir, tectonicus.configuration.Map map, List<Player> players, PlayerIconAssembler playerIconAssembler)
	{
		try {
			Files.deleteIfExists(playersFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileUtils.ensureExists(imagesDir);
		
		log.info("Exporting players to {}", playersFile.getAbsolutePath());
		
		int numOutput = 0;
		ExecutorService executor = Executors.newCachedThreadPool();
		try (JsArrayWriter jsWriter = new JsArrayWriter(playersFile, map.getId() + "_playerData")) {

			PlayerFilter playerFilter = map.getPlayerFilter();
			WorldSubset worldSubset = map.getWorldSubset();
			for (Player player : players) {
				if (playerFilter.passesFilter(player)) {
					Vector3d position = player.getPosition();
					if (worldSubset.containsBlock(position.x, position.z)) {
						log.debug("\texporting {}", player.getName());

						HashMap<String, String> args = new HashMap<>();

						Vector3d pos = player.getPosition();
						args.put("name", "\"" + player.getName() + "\"");

						String posStr = "new WorldCoord(" + pos.x + ", " + pos.y + ", " + pos.z + ")";
						args.put("worldPos", posStr);

						args.put("health", "" + player.getHealth());
						args.put("food", "" + player.getFood());
						args.put("air", "" + player.getAir());

						args.put("xpLevel", "" + player.getXpLevel());
						args.put("xpTotal", "" + player.getXpTotal());

						jsWriter.write(args);

						File iconFile = new File(imagesDir, player.getName() + ".png");
						WriteIconTask task = playerIconAssembler.new WriteIconTask(player, iconFile);
						executor.submit(task);

						numOutput++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executor.shutdown();
		}
		log.debug("Exported {} players", numOutput);
	}
	
	public void outputBeds(File exportDir, tectonicus.configuration.Map map, List<Player> players)
	{
		File bedsFile = new File(exportDir, "beds.js");
		try {
			Files.deleteIfExists(bedsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting beds to {}", bedsFile.getAbsolutePath());
		
		int numOutput = 0;

		try (JsArrayWriter jsWriter = new JsArrayWriter(bedsFile, map.getId() + "_bedData")) {

			if (map.getDimension() == Dimension.OVERWORLD) // Beds only exist in the overworld dimension
			{
				WorldSubset worldSubset = map.getWorldSubset();
				PlayerFilter filter = map.getPlayerFilter();
				for (Player player : players) {
					if (filter.isShowBeds() && filter.passesFilter(player) && player.getSpawnDimension() == Dimension.OVERWORLD && player.getSpawnPosition() != null) {
						HashMap<String, String> bedArgs = new HashMap<>();

						Vector3l spawn = player.getSpawnPosition();

						if (worldSubset.containsBlock(spawn.x, spawn.z)) {
							log.debug("\texporting {}'s bed", player.getName());

							bedArgs.put("playerName", "\"" + player.getName() + "\"");

							String posStr = "new WorldCoord(" + spawn.x + ", " + spawn.y + ", " + spawn.z + ")";
							bedArgs.put("worldPos", posStr);


							jsWriter.write(bedArgs);
							numOutput++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.debug("Exported {} beds", numOutput);
	}

	public void outputRespawnAnchors(File exportDir, tectonicus.configuration.Map map, List<Player> players)
	{
		File anchorsFile = new File(exportDir, "respawnAnchors.js");
		try {
			Files.deleteIfExists(anchorsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting respawn anchors to {}", anchorsFile.getAbsolutePath());

		int numOutput = 0;

		try (JsArrayWriter jsWriter = new JsArrayWriter(anchorsFile, map.getId() + "_respawnAnchorData")) {

			if (map.getDimension() == Dimension.NETHER) // Respawn anchors only work in the nether dimension
			{
				WorldSubset worldSubset = map.getWorldSubset();
				PlayerFilter filter = map.getPlayerFilter();
				for (Player player : players) {
					if (filter.isShowRespawnAnchors() && filter.passesFilter(player) && player.getSpawnDimension() == Dimension.NETHER && player.getSpawnPosition() != null) {
						HashMap<String, String> anchorArgs = new HashMap<>();

						Vector3l spawn = player.getSpawnPosition();

						if (worldSubset.containsBlock(spawn.x, spawn.z)) {
							log.debug("\texporting {}'s respawn anchor", player.getName());

							anchorArgs.put("playerName", "\"" + player.getName() + "\"");

							String posStr = "new WorldCoord(" + spawn.x + ", " + spawn.y + ", " + spawn.z + ")";
							anchorArgs.put("worldPos", posStr);


							jsWriter.write(anchorArgs);
							numOutput++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.debug("Exported {} respawn anchors", numOutput);
	}
	
	private void outputHtmlResources(TexturePack texturePack, PlayerIconAssembler playerIconAssembler, String defaultSkin)
	{
		log.info("Writing javascript and image resources...");

		File imagesDir = new File(exportDir, "Images");
		imagesDir.mkdirs();
		
		FileUtils.extractResource("Images/Spawn.png", new File(imagesDir, "Spawn.png"));
		FileUtils.extractResource("Images/Logo.png", new File(imagesDir, "Logo.png"));
		
		FileUtils.extractResource("Images/Spacer.png", new File(imagesDir, "Spacer.png"));

		Version texturePackVersion = texturePack.getVersion();
		switch (texturePackVersion) {
			case VERSION_4:
				writeImage(texturePack.getItem(10, 2), 32, 32, new File(imagesDir, "Sign.png"));
				writeImage(texturePack.getItem(10, 1), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem(7, 1), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem(7, 2), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem(7, 3), 32, 32, new File(imagesDir, "DiamondIcon.png"));
				writeImage(texturePack.getItem(13, 2), 32, 32, new File(imagesDir, "Bed.png"));
				if (defaultSkin.equals("steve"))
					defaultSkin = "mob/char.png";
				break;

			case VERSION_5:
				writeImage(texturePack.getItem("textures/items/sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				writeImage(texturePack.getItem("textures/items/painting.png"), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem("textures/items/ingotIron.png"), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem("textures/items/ingotGold.png"), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem("textures/items/diamond.png"), 32, 32, new File(imagesDir, "DiamondIcon.png"));
				writeImage(texturePack.getItem("textures/items/bed.png"), 32, 32, new File(imagesDir, "Bed.png"));
				if (defaultSkin.equals("steve"))
					defaultSkin = "mob/char.png";
				break;

			case VERSIONS_6_TO_8:
			case VERSION_RV:
			case VERSIONS_9_TO_11:
			case VERSION_12:
				writeImage(texturePack.getItem("assets/minecraft/textures/items/sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/items/painting.png"), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/items/iron_ingot.png"), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/items/gold_ingot.png"), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/items/diamond.png"), 32, 32, new File(imagesDir, "DiamondIcon.png"));
				if (texturePackVersion != VERSION_12) {
					writeImage(texturePack.getItem("assets/minecraft/textures/items/bed.png"), 32, 32, new File(imagesDir, "Bed.png"));
				}

				if (defaultSkin.equals("steve"))
					defaultSkin = "assets/minecraft/textures/entity/steve.png";
				break;

			default: //assume version is 1.13+
				if (texturePackVersion == VERSION_13) {
					writeImage(texturePack.getItem("assets/minecraft/textures/item/sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				} else {
					writeImage(texturePack.getItem("assets/minecraft/textures/item/oak_sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				}
				writeImage(texturePack.getItem("assets/minecraft/textures/item/painting.png"), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/item/iron_ingot.png"), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/item/gold_ingot.png"), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem("assets/minecraft/textures/item/diamond.png"), 32, 32, new File(imagesDir, "DiamondIcon.png"));
				if (defaultSkin.equals("steve"))
					defaultSkin = "assets/minecraft/textures/entity/steve.png";
		}
		
		// Hearts need composing so they get the outline
		{
			BufferedImage emptyHeart = texturePack.getIcon(16, 0, 9, 9);
			BufferedImage halfHeart = texturePack.getIcon(61, 0, 9, 9);
			BufferedImage fullHeart = texturePack.getIcon(52, 0, 9, 9);
			
			BufferedImage composedHalf = TexturePack.copy(emptyHeart);
			composedHalf.getGraphics().drawImage(halfHeart, 0, 0, halfHeart.getWidth(), halfHeart.getHeight(), null);
	
			BufferedImage composedFull = TexturePack.copy(emptyHeart);
			composedFull.getGraphics().drawImage(fullHeart, 0, 0, fullHeart.getWidth(), fullHeart.getHeight(), null);
			
			writeImage(emptyHeart, 18, 18, new File(imagesDir, "EmptyHeart.png"));
			writeImage(composedHalf, 18, 18, new File(imagesDir, "HalfHeart.png"));
			writeImage(composedFull, 18, 18, new File(imagesDir, "FullHeart.png"));
		}
		
		// Food needs composing like hearts
		{
			BufferedImage emptyFood = texturePack.getIcon(16, 27, 9, 9);
			BufferedImage halfFood = texturePack.getIcon(61, 27, 9, 9);
			BufferedImage fullFood = texturePack.getIcon(52, 27, 9, 9);
			
			BufferedImage composedHalfFood = TexturePack.copy(emptyFood);
			composedHalfFood.getGraphics().drawImage(halfFood, 0, 0, halfFood.getWidth(), halfFood.getHeight(), null);
			
			BufferedImage composedFullFood = TexturePack.copy(emptyFood);
			composedFullFood.getGraphics().drawImage(fullFood, 0, 0, fullFood.getWidth(), fullFood.getHeight(), null);
			
			writeImage(emptyFood, 18, 18, new File(imagesDir, "EmptyFood.png"));
			writeImage(composedHalfFood, 18, 18, new File(imagesDir, "HalfFood.png"));
			writeImage(composedFullFood, 18, 18, new File(imagesDir, "FullFood.png"));
		}
		
		// Air just comes out direct
		writeImage(texturePack.getIcon(16, 18, 9, 9), 18, 18, new File(imagesDir, "FullAir.png"));
		writeImage(texturePack.getIcon(25, 18, 9, 9), 18, 18, new File(imagesDir, "EmptyAir.png"));
		
		writeImage(texturePack.getChestImage(), 176, 77, new File(imagesDir, "SmallChest.png"));
		
		// Write default player icon
		playerIconAssembler.writeDefaultIcon(texturePack.getItem(defaultSkin), new File(imagesDir, "PlayerIcons/Tectonicus_Default_Player_Icon.png"));
		
		//Extract Leaflet resources
		extractMapResources(exportDir);

		ArrayList<String> scriptResources = new ArrayList<>();
		scriptResources.add("marker.js");
		scriptResources.add("controls.js");
		scriptResources.add("minecraftProjection.js");
		scriptResources.add("main.js");
		outputMergedJs(new File(exportDir, "Scripts/tectonicus.js"), scriptResources);
	}

	public static void extractMapResources(File exportDir) {
		File scriptsDir = new File(exportDir, "Scripts");
		scriptsDir.mkdirs();
		File scriptImagesDir = new File(scriptsDir, "images");
		scriptImagesDir.mkdirs();

		FileUtils.extractResource("styles.css", new File(scriptsDir, "styles.css"));

		FileUtils.extractResource("math.js", new File(scriptsDir, "math.js"));
		FileUtils.extractResource("leaflet.js", new File(scriptsDir, "leaflet.js"));
		FileUtils.extractResource("leaflet.css", new File(scriptsDir, "leaflet.css"));
		FileUtils.extractResource("leafletStyles.css", new File(scriptsDir, "leafletStyles.css"));
		FileUtils.extractResource("Images/layers.png", new File(scriptImagesDir, "layers.png"));
		FileUtils.extractResource("Images/layers-2x.png", new File(scriptImagesDir, "layers-2x.png"));
		FileUtils.extractResource("Images/marker-icon.png", new File(scriptImagesDir, "marker-icon.png"));
		FileUtils.extractResource("Images/marker-icon-2x.png", new File(scriptImagesDir, "marker-icon-2x.png"));
		FileUtils.extractResource("Images/marker-shadow.png", new File(scriptImagesDir, "marker-shadow.png"));
		FileUtils.extractResource("popper.min.js", new File(scriptsDir, "popper.min.js"));
		FileUtils.extractResource("tippy-bundle.umd.min.js", new File(scriptsDir, "tippy-bundle.umd.min.js"));
		FileUtils.extractResource("tippy-light-theme.css", new File(scriptsDir, "tippy-light-theme.css"));
	}

	public static void writeImage(BufferedImage img, final int width, final int height, File file)
	{
		try
		{
			BufferedImage toWrite;
			if (img.getWidth() != width || img.getHeight() != height)
			{
				toWrite = new BufferedImage(width, height, img.getType());
				toWrite.getGraphics().drawImage(img, 0, 0, width, height, null);
			}
			else
			{
				toWrite = img;
			}
			ImageIO.write(toWrite, "png", file);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private WorldVectors calcWorldVectors()
	{
		// Calculate origin and axes needed for the js to convert from world to map coords
		
		WorldVectors worldVectors = new WorldVectors();
		
		// Vectors for world->map projection
		Vector2f originScreenPos = camera.projectf( new Vector3f(0, 0, 0) );
		Vector2f p100 = camera.projectf( new Vector3f(1, 0, 0) );
		Vector2f p010 = camera.projectf( new Vector3f(0, 1, 0) );
		Vector2f p001 = camera.projectf( new Vector3f(0, 0, 1) );
		
		worldVectors.origin = new Vector2f(originScreenPos.x, originScreenPos.y);
		worldVectors.xAxis = new Vector2f(p100.x - originScreenPos.x, p100.y - originScreenPos.y);
		worldVectors.yAxis = new Vector2f(p010.x - originScreenPos.x, p010.y - originScreenPos.y);
		worldVectors.zAxis = new Vector2f(p001.x - originScreenPos.x, p001.y - originScreenPos.y);
		
		// Vectors for map->world projection
		Vector3f base = camera.unproject(new Vector2f(0, 0));
		Vector3f mapXUnit = camera.unproject(new Vector2f(1, 0));
		Vector3f mapYUnit = camera.unproject(new Vector2f(0, 1));
		
		worldVectors.mapXUnit = new Vector2f(mapXUnit.x - base.x, mapXUnit.z - base.z);
		worldVectors.mapYUnit = new Vector2f(mapYUnit.x - base.x, mapYUnit.z - base.z);

		// For 1.18 and higher we need to offset the map markers
		if (Minecraft.getChunkHeight() > 256) {
			worldVectors.yOffset = 64;
		}

		return worldVectors;
	}
	
	private void outputSigns(File outputFile, File signListFile, tectonicus.configuration.Map map)
	{
		HddObjectListReader<Sign> signsIn = null;
		try
		{
			signsIn = new HddObjectListReader<>(signListFile);
			outputSigns(outputFile, signsIn, map);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (signsIn != null)
				signsIn.close();
		}
	}
	
	private void outputSigns(File signFile, HddObjectListReader<Sign> signs, tectonicus.configuration.Map map) throws IOException {
		log.info("Exporting signs to {}", signFile.getAbsolutePath());

		Files.deleteIfExists(signFile.toPath());
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(signFile, map.getId()+"_signData");

			WorldSubset worldSubset = map.getWorldSubset();
			Sign sign = new Sign();
			while (signs.hasNext())
			{				
				signs.read(sign);
				String message = "\"" +sign.getText(0) + "\\n" + sign.getText(1) + "\\n" + sign.getText(2) + "\\n" + sign.getText(3) + "\"";
				if (map.getSignFilter() == SignFilter.Obey)
					message = "\"\\nOBEY\\n\\n\"";
				
				HashMap<String, String> signArgs = new HashMap<>();
				
				final float worldX = sign.getX() + 0.5f;
				final float worldY = sign.getY();
				final float worldZ = sign.getZ() + 0.5f;				
				
				String posStr = "new WorldCoord("+worldX+", "+worldY+", "+worldZ+")";
				signArgs.put("worldPos", posStr);
				signArgs.put("message", message);
				if (map.getSignFilter() == SignFilter.Obey)
				{
					signArgs.put("text1", "\"\"");
					signArgs.put("text2", "\"OBEY\"");
					signArgs.put("text3", "\"\"");
					signArgs.put("text4", "\"\"");
				}
				else
				{
					signArgs.put("text1", "\"" + sign.getText(0) + "\"");
					signArgs.put("text2", "\"" + sign.getText(1) + "\"");
					signArgs.put("text3", "\"" + sign.getText(2) + "\"");
					signArgs.put("text4", "\"" + sign.getText(3) + "\"");
				}
				
				if (worldSubset.containsBlock(sign.getX(), sign.getZ()))
				{
					jsWriter.write(signArgs);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jsWriter != null)
				jsWriter.close();
		}
	}
	
	private int outputPortals(File outFile, File portalListFile, tectonicus.configuration.Map map)
	{
		int numPortals = 0;
		
		try
		{
			HddObjectListReader<Portal> portalsIn = new HddObjectListReader<>(portalListFile);
			numPortals = outputPortals(outFile, portalsIn, map);
			portalsIn.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return numPortals;
	}
	
	private int outputPortals(File portalFile, HddObjectListReader<Portal> portalPositions, tectonicus.configuration.Map map) throws IOException {
		log.info("Writing portals...");

		Files.deleteIfExists(portalFile.toPath());
		
		int numPortals = 0;
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(portalFile, map.getId()+"_portalData");

			ArrayList<Portal> portals = new ArrayList<>();
			
			if (portalPositions.hasNext())
			{
				long prevX;
				long prevY;
				long prevZ;
				long firstX;
				long firstZ;

				Portal portal = new Portal();
				portalPositions.read(portal);
				firstX = portal.getX();
				firstZ = portal.getZ();
				prevX = portal.getX();
				prevY = portal.getY();
				prevZ = portal.getZ();
				
				while (portalPositions.hasNext())
				{				
					portalPositions.read(portal);
					
					//Find the horizontal center portal block location
					if((portal.getX() == prevX && portal.getZ() == prevZ+1) || (portal.getX() == prevX+1 && portal.getZ() == prevZ))
					{
						prevX = portal.getX();
						prevY = portal.getY();
						prevZ = portal.getZ();
					}
					else
					{
						portals.add(new Portal(prevX+(firstX-prevX)/2, prevY, prevZ+(firstZ-prevZ)/2));
						numPortals++;
						prevX = portal.getX();
						prevY = portal.getY();
						prevZ = portal.getZ();
						firstX = portal.getX();
						firstZ = portal.getZ();
					}
				}
				portals.add(new Portal(portal.getX()+((firstX-prevX)/2), portal.getY(), portal.getZ()+(firstZ-prevZ)/2));
				numPortals++;
			
				WorldSubset worldSubset = map.getWorldSubset();
				for (Portal p : portals)
				{
					final float worldX = p.getX();
					final float worldY = p.getY();
					final float worldZ = p.getZ();
					
					HashMap<String, String> portalArgs = new HashMap<>();
					String posStr = "new WorldCoord("+worldX+", "+worldY+", "+worldZ+")";
					portalArgs.put("worldPos", posStr);
					
					if (worldSubset.containsBlock(p.getX(), p.getZ()))
					{
						jsWriter.write(portalArgs);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jsWriter != null)
				jsWriter.close();
		}
		
		log.debug("Wrote {} portals", numPortals);
		return numPortals;
	}
	
	private void outputViews(File outputFile, File viewsListFile, tectonicus.configuration.Map map)
	{
		HddObjectListReader<Sign> viewsIn = null;
		try
		{
			viewsIn = new HddObjectListReader<>(viewsListFile);
			outputViews(outputFile, viewsIn, map);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (viewsIn != null)
				viewsIn.close();
		}
	}
	
	private void outputViews(File viewsFile, HddObjectListReader<Sign> views, tectonicus.configuration.Map map) throws IOException {
		log.info("Writing views...");
		
		Files.deleteIfExists(viewsFile.toPath());
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(viewsFile, map.getId()+"_viewData");

			WorldSubset worldSubset = map.getWorldSubset();
			ImageFormat imageFormat = map.getViewConfig().getImageFormat();
			Sign sign = new Sign();
			while (views.hasNext())
			{				
				views.read(sign);
				
				HashMap<String, String> viewArgs = new HashMap<>();
				
				final float worldX = sign.getX() + 0.5f;
				final float worldY = sign.getY();
				final float worldZ = sign.getZ() + 0.5f;				
				
				String posStr = "new WorldCoord("+worldX+", "+worldY+", "+worldZ+")";
				viewArgs.put("worldPos", posStr);
				
				StringBuilder text = new StringBuilder();
				for(int i=0; i<4; i++)
				{
					if (!sign.getText(i).startsWith("#"))
					{
						text.append(sign.getText(i)).append(" ");
					}
				}

				viewArgs.put("text", "\"" + text.toString().trim() + "\"");
				
				String filename = map.getId()+"/Views/View_"+sign.getX()+"_"+sign.getY()+"_"+sign.getZ()+"."+imageFormat.getExtension();
				viewArgs.put("imageFile", "\"" + filename + "\"");

				if (worldSubset.containsBlock(sign.getX(), sign.getZ())) {
					jsWriter.write(viewArgs);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jsWriter != null)
				jsWriter.close();
		}
	}
	
	private void outputChests(File chestFile, tectonicus.configuration.Map map, List<ContainerEntity> chestList)
	{
		log.info("Writing chests to {}", chestFile.getAbsolutePath());

		try {
			Files.deleteIfExists(chestFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (JsArrayWriter jsWriter = new JsArrayWriter(chestFile, map.getId() + "_chestData")) {

			ArrayList<BlockEntity> removeList = new ArrayList<>();
			for (BlockEntity entity : chestList) {
				final int x = entity.getX();
				final int y = entity.getY();
				final int z = entity.getZ();

				for (BlockEntity newEntity : chestList) {
					final int newX = newEntity.getX();
					final int newY = newEntity.getY();
					final int newZ = newEntity.getZ();

					if (newX == x + 1 && newY == y && newZ == z) //north south chest
					{
						entity.setX(newX);
						if (!removeList.contains(entity))
							removeList.add(newEntity);
					} else if (newZ == z + 1 && newY == y && newX == x) //east west chest
					{
						entity.setZ(z);
						if (!removeList.contains(entity))
							removeList.add(newEntity);
					}
				}
			}

			chestList.removeAll(removeList);

			WorldSubset worldSubset = map.getWorldSubset();
			for (BlockEntity entity : chestList) {
				float worldX = entity.getX() + 0.5f;
				float worldY = entity.getY();
				float worldZ = entity.getZ() + 0.5f;
				HashMap<String, String> chestArgs = new HashMap<>();

				String posStr = "new WorldCoord(" + worldX + ", " + worldY + ", " + worldZ + ")";
				chestArgs.put("worldPos", posStr);

				if (worldSubset.containsBlock(entity.getX(), entity.getZ())) {
					jsWriter.write(chestArgs);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void outputIcons(tectonicus.configuration.Map map, World world)
	{
		BlockTypeRegistry registryOld = world.getBlockTypeRegistry();
		BlockRegistry registry = world.getModelRegistry();
		TexturePack texturePack = world.getTexturePack();
		Version version = world.getWorldInfo().getVersion();

		try {
			ItemRenderer itemRenderer = new ItemRenderer(rasteriser);
			if (texturePack.getVersion().getNumVersion() < VERSION_13.getNumVersion()) {
				itemRenderer.renderBlockOld(new File(exportDir, "Images/Chest.png"), registryOld, texturePack, BlockIds.CHEST, 5);
			} else {
				Map<String, String> properties = new HashMap<>();
				properties.put("facing", "south");
				itemRenderer.renderBlock(new File(exportDir, "Images/Chest.png"), registryOld, texturePack, Block.CHEST, new BlockProperties(properties));
			}
			itemRenderer.renderBed(new File(exportDir, "Images/Bed.png"), registryOld, texturePack);
			itemRenderer.renderCompass(map, new File(exportDir, map.getId()+"/Compass.png"));
			itemRenderer.renderPortal(new File(args.getOutputDir(), "Images/Portal.png"), registryOld, texturePack);
			if (version.getNumVersion() >= VERSION_16.getNumVersion()) {
				itemRenderer.renderBlockModel(new File(args.getOutputDir(), "Images/RespawnAnchor.png"), registry, texturePack, Block.RESPAWN_ANCHOR, "_4");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void outputChangedFile() {
		log.info("Writing changed file list...");
		progressListener.onTaskStarted(Task.OUTPUT_CHANGED_LIST.toString());
		
		changedFileList.close();
		changedFileList = null;
	}
	
	private TileCoord screenToTile(Point screenPos)
	{
		final float tileX = (float)screenPos.x / (float)tileWidth;
		final float tileY = (float)screenPos.y / (float)tileHeight;
		
		final int wholeTileX = (int)Math.floor(tileX);
		final int wholeTileY = (int)Math.floor(tileY);
		
		return new TileCoord(wholeTileX, wholeTileY);
	}
	
	private static Point tileToScreen(TileCoord tilePos, final int tileWidth, final int tileHeight)
	{
		return new Point(tilePos.x * tileWidth, tilePos.y * tileHeight);
	}
	
	public static File getImageFile(File dir, final int x, final int y, ImageFormat imageFormat)
	{
		final int xBin = x % 16;
		final int yBin = y % 16;
		File dir1 = new File(dir, ""+xBin);
		File dir2 = new File(dir1, ""+yBin);
		return new File(dir2, "tile_"+x+"_"+y+"."+imageFormat.getExtension());
	}
	
	public static Point calcLatLong(TileCoord tile, Point screenPos, final int tileWidth, final int tileHeight)
	{
		final int x = tile.x * tileWidth + screenPos.x;
		final int y = tile.y * tileHeight + screenPos.y;
		return new Point(x, y);
	}
	
	public static String jsEscape(String text)
	{
		text = text.replace("\\", "\\\\");	// Replace \ with \\
		text = text.replace(" ", "&nbsp;");	// Replace spaces with &nbsp;
		text = text.replace("\"", "\\\"");	// Replace " with \"
		text = text.replace("\'", "\\\'");	// Replace ' with \'
		
		return text;
	}
	
	public static class Result
	{
		public final boolean aborted;
		
		public final File htmlFile;
		
		public Result(final boolean aborted, final File htmlFile)
		{
			this.aborted = aborted;
			this.htmlFile = htmlFile;
		}
	}
	
	private static boolean passesFilter(SignEntity s, SignFilter filter)
	{
		final String text1 = s.getText1();
		final String text2 = s.getText2();
		final String text3 = s.getText3();
		final String text4 = s.getText4();
		
		// Empty signs (those with no text) are used for asthetic reasons, like building chairs
		// Always skip these
		if (text1.trim().isEmpty() && text2.trim().isEmpty() && text3.trim().isEmpty() && text4.trim().isEmpty())
			return false;
		
		// Always skip view signs
		if (text1.startsWith("#view") || text2.startsWith("#view") || text3.startsWith("#view") || text4.startsWith("#view"))
		{
			return false;
		}
		
		if (filter == SignFilter.None)
		{
			return false;
		}
		else if (filter == SignFilter.All || filter == SignFilter.Obey)
		{
			return true;
		}
		else if (filter == SignFilter.Special)
		{
			String line = "" + text1 + text2 + text3 + text4;
			line = line.trim();
			if (line.length() > 0)
			{
				final char first = line.charAt(0);
				final char last = line.charAt(line.length()-1);
				
				final char[] special = { '-', '=', '~', '!' };
				return containedIn(special, first) && containedIn(special, last);
			}
			else
				return false;
		}
		else
		{
			throw new RuntimeException("Unknown player filter:"+filter);
		}
	}
	
	private static boolean containedIn(final char[] possible, final char actual)
	{
		for (char ch : possible)
		{
			if (ch == actual)
				return true;
		}
		return false;
	}
	
	private static class WorldVectors
	{
		Vector2f origin;
		Vector2f xAxis;
		Vector2f yAxis;
		Vector2f zAxis;
		Vector2f mapXUnit;
		Vector2f mapYUnit;
		int yOffset;

		public WorldVectors()
		{
			origin = new Vector2f();
			
			xAxis = new Vector2f();
			yAxis = new Vector2f();
			zAxis = new Vector2f();
			
			mapXUnit = new Vector2f();
			mapYUnit = new Vector2f();
		}
	}
}
