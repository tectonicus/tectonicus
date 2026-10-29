/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.FileTileCache;
import tectonicus.cache.FileViewCache;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.cache.RegionHashStore;
import tectonicus.cache.TileCache;
import tectonicus.cache.TileHashCache;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Configuration.RenderStyle;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.DimensionConfig;
import tectonicus.itemmodeldefinitionregistry.ItemModelDefinitionRegistry;
import tectonicus.itemregistry.ItemRegistry;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.raw.BedEntity;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.OrthoCamera;
import tectonicus.util.DirUtils;
import tectonicus.util.FileUtils;
import tectonicus.util.ImageUtils;
import tectonicus.util.TileOutput;
import tectonicus.util.TempArea;
import tectonicus.util.Util;
import tectonicus.view.ViewRenderer;
import tectonicus.world.Sign;
import tectonicus.world.World;
import tectonicus.world.WorldVectors;
import tectonicus.world.filter.ExploredCaveFilter;
import tectonicus.world.filter.ExploredCaveFilter113;
import tectonicus.world.filter.NetherBlockFilter;
import tectonicus.world.filter.NetherBlockFilter113;
import tectonicus.world.subset.RegionIterator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static tectonicus.Version.VERSION_13;
import static tectonicus.util.OutputResourcesUtil.outputBeacons;
import static tectonicus.util.FindEntityUtil.findBeacons;
import static tectonicus.util.OutputResourcesUtil.outputBeds;
import static tectonicus.util.OutputResourcesUtil.outputChests;
import static tectonicus.util.OutputResourcesUtil.outputContents;
import static tectonicus.util.OutputResourcesUtil.outputCompass;
import static tectonicus.util.OutputResourcesUtil.outputHtml;
import static tectonicus.util.OutputResourcesUtil.outputHtmlResources;
import static tectonicus.util.OutputResourcesUtil.outputIcons;
import static tectonicus.util.OutputResourcesUtil.outputInventoryItemIcons;
import static tectonicus.util.OutputResourcesUtil.outputPlayers;
import static tectonicus.util.OutputResourcesUtil.outputPortals;
import static tectonicus.util.OutputResourcesUtil.outputRenderStats;
import static tectonicus.util.OutputResourcesUtil.outputRespawnAnchors;
import static tectonicus.util.OutputResourcesUtil.outputSigns;
import static tectonicus.util.OutputResourcesUtil.outputViews;
import static tectonicus.util.OutputResourcesUtil.outputWorldResources;

@Slf4j
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
	
	private final Configuration config;
	
	private final MessageDigest hashAlgorithm;
	
	private final int tileWidth;
	private final int tileHeight;
	
	private int numZoomLevels;
	
	private final File exportDir;
	
	private final PlayerSkinCache playerSkinCache;
	
	private RegionHashStore regionHashStore;
	
	private final HddTileListFactory hddTileListFactory;
	
	private final OrthoCamera camera;
	
	private final Rasteriser rasteriser;
	
	private ChangeFile changedFileList;
	
	private final PlayerIconAssembler playerIconAssembler;
	
	private final ProgressListener progressListener;
	
	private final MemoryMonitor memoryMonitor;
	
	private boolean abort;
	private HddTileList discoveredTiles;
	private boolean useFullHeightBounds;
	
	public TileRenderer(Configuration config, ProgressListener listener, MessageDigest hashAlgorithm) throws Exception
	{
		this.config = config;
		this.hashAlgorithm = hashAlgorithm;
		
		this.progressListener = listener;
		
		this.tileWidth = config.getTileSize();
		this.tileHeight = config.getTileSize();
		
		this.numZoomLevels = config.getNumZoomLevels();
		
		this.exportDir = config.getOutputDir();
		
		playerSkinCache = new PlayerSkinCache(config, hashAlgorithm);
		
		hddTileListFactory = new HddTileListFactory( new File(config.getCacheDir(), "tileLists") );
		
		log.debug("Creating player icon assembler");
		playerIconAssembler = new PlayerIconAssembler(playerSkinCache);
		
		memoryMonitor = new MemoryMonitor();

		log.info("Initialising display...");

		DisplayType type = DisplayType.OFFSCREEN;
		if (config.isUseEGL()) {
			type = DisplayType.OFFSCREEN_EGL;
		}

		rasteriser = RasteriserFactory.createRasteriser(config.getRasteriserType(), type, 2048, 2048, config.getColourDepth(), config.getAlphaBits(), 24, config.getNumSamples());

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
	
	public Result output() throws IOException
	{
		progressListener.onTaskStarted(Task.STARTING_RENDERER.toString());
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(config.getCacheDir());
		
		TempArea tempArea = new TempArea( new File(config.getCacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(config.getOutputDir(), "changed.txt"));
		
                World world = null;
		Set<String> exportedWorlds = new HashSet<>();
                
		for (tectonicus.configuration.DimensionConfig map : config.getDimensions())
		{
			// Clear shared state?
			// ..
			
			// Clear region cache?
			// ..
			
			numZoomLevels = map.getNumZoomLevels();
			File mapDir = new File(exportDir, "Scripts/" + map.getId());
			FileUtils.ensureExists(mapDir);

			BiomeCache biomeCache = CacheUtil.createBiomeCache(config, map, hashAlgorithm);

			// Create the world for this map
			world = new World(rasteriser, map, biomeCache, playerSkinCache, config);
			
			// Setup camera
			setupInitialCamera(map);
			
			WorldVectors worldVectors = WorldVectors.calcWorldVectors(camera);
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");

			discoveredTiles = hddTileListFactory.createList();
			useFullHeightBounds = map.getLayers().stream().anyMatch(layer ->
					(layer.getCustomBlockConfig() != null && !layer.getCustomBlockConfig().isBlank()) || !layer.useDefaultBlockConfig());
			WorldStats worldStats = preProcess(world, map, portalsFile, signsFile, viewsFile);
			
			// Find visible tiles
			HddTileList visibleTiles = discoveredTiles;
			discoveredTiles = null;
			log.info("found {} total tiles to output using occupied chunk heights", visibleTiles.size());
			
			
			// Figure out which tiles we need to render
			progressListener.onTaskStarted(Task.FIND_CHANGED_TILES.toString());

			// Output entity javascript for creating map markers
			outputSigns(new File(mapDir, "signs.js"), signsFile, map);
			outputPlayers(new File(mapDir, "players.js"), new File(DirUtils.getWorldImagesDir(exportDir, map.getWorld()), "PlayerIcons"), map, world.getPlayers(map.getDimensionInfo()), playerIconAssembler);
			outputBeds(mapDir, map, world.getAllPlayers(), world.getBeds());
			outputRespawnAnchors(mapDir, map, world.getAllPlayers());
			List<Portal> portals = outputPortals(new File(mapDir, "portals.js"), portalsFile, map);
			worldStats.setNumPortals(portals.size());
			outputViews(new File(mapDir, "views.js"), viewsFile, map);
			outputChests(new File(mapDir, "chests.js"), map, world.getChests());
			outputBeacons(new File(mapDir, "beacons.js"), map, world.getBeacons());

			// Render views
			FileViewCache viewCache = CacheUtil.createViewCache(config.getCacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, config.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
			
			TileCoordBounds bounds = null;
			
			if (map.getLayers().isEmpty())
				log.warn("No layers found!!!");
			
			File tileHashFile = config.useCache() && map.numLayers() > 1
					? tempArea.generateTempFile("tileHashes", ".cache") : null;
			try (TileHashCache tileHashes = new TileHashCache(tileHashFile, world, map, camera, regionHashStore,
					tileWidth, tileHeight, hashAlgorithm)) {
				for (LayerConfig layer : map.getLayers()) {
					// Setup per-layer config
					setupWorldForLayer(layer, world);

					// Set new tile cache for this layer
					String optionString = FileTileCache.calcOptionsString(config, map);
					TileCache tileCache = CacheUtil.createTileCache(config.useCache(), optionString, layer.getImageFormat(), config.getCacheDir(), map, layer, hashAlgorithm);
					try {
						File baseTilesDir = DirUtils.getZoomDir(exportDir, layer, numZoomLevels);
						FileUtils.ensureExists(baseTilesDir);

						// Share current hashes, but check each layer's saved cache and output independently.
						HddTileList changedTiles = tileCache.findChangedTiles(hddTileListFactory, visibleTiles, tileHashes, baseTilesDir);

						// Trim changed tiles to size
						changedTiles = trimTileList(changedTiles, config.getMaxTiles());

						// Render base tiles
						renderBaseTiles(world, map, layer, baseTilesDir, changedTiles, tileCache);

						// Retire old candidates, including tiles vacated if terrain shrinks.
						HddTileList removed = hddTileListFactory.createList();
						TileOutput.removeObsolete(baseTilesDir.toPath(), visibleTiles, layer.getImageFormat(), removed);
						HddTileList downsampleChanges = changedTiles;
						if (removed.size() > 0) {
							downsampleChanges = hddTileListFactory.createList();
							for (TileCoord tile : changedTiles)
								downsampleChanges.add(tile);
							for (TileCoord tile : removed)
								downsampleChanges.add(tile);
						}

						// Create downsampled layers
						bounds = downsample(visibleTiles, downsampleChanges, exportDir, layer, baseTilesDir, tileCache);
					} finally {
						tileCache.closeTileCache();
					}
				}
			}

			outputCompass(exportDir, map, rasteriser);
			if (exportedWorlds.add(map.getWorld().getId())) {
				File imagesDir = DirUtils.getWorldImagesDir(exportDir, map.getWorld());
				outputIcons(imagesDir, world, rasteriser);
				ItemModelDefinitionRegistry itemModels = new ItemModelDefinitionRegistry(world.getTexturePack());
				ItemRegistry items = new ItemRegistry(world.getTexturePack());
				outputInventoryItemIcons(imagesDir, rasteriser, world.getTexturePack(), world.getBlockTypeRegistry(), world.getModelRegistry(), items, itemModels);
				outputWorldResources(world.getTexturePack(), playerIconAssembler, config, exportDir, map.getWorld());
			}
			
			// Output world stats
			worldStats.outputBlockStats(new File(mapDir, "blockStats.js"), map.getJavascriptId(), world.getBlockTypeRegistry());
			worldStats.outputWorldStats(new File(mapDir, "worldStats.js"), map.getJavascriptId());
			
			// Output world vectors for this camera config
			worldVectors.outputWorldVectors(new File(mapDir, "worldVectors.js"), map, bounds, world,
					worldStats.numChunks(), portals, numZoomLevels, tileWidth, tileHeight);
		}
                
                if (world == null) {
                        System.out.println("Unable to render. No map is defined in config.");
                } else {
                        outputHtmlResources(config, exportDir, config.getDimensions().stream().mapToInt(DimensionConfig::getNumZoomLevels).max().orElse(config.getNumZoomLevels()), tileWidth, tileHeight);
                }
		
		outputContents(new File(new File(exportDir, "Scripts"), "contents.js"), config);
		
		
		// Output html
		File outputHtmlFile = null;
		try {
			progressListener.onTaskStarted(Task.OUTPUT_HTML.toString());
			outputHtmlFile = outputHtml(exportDir, config);
		} catch (IOException e) {
			log.error("Exception: ", e);
		}

		// ----
		
		Date endTime = new Date();
		String time = Util.getElapsedTime(startTime, endTime);
		
		outputRenderStats(exportDir, memoryMonitor, time);
		
		outputChangedFile();
		
		log.info("Render complete - total time "+time);
		
		return new Result(abort, outputHtmlFile);
	}
	
	// Just renders views
	public Result renderViews()
	{
		progressListener.onTaskStarted(Task.STARTING_RENDERER.toString());
		log.info("Starting view renderer");
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(config.getCacheDir());
		
		TempArea tempArea = new TempArea( new File(config.getCacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(config.getOutputDir(), "changed.txt"));
		
		for (tectonicus.configuration.DimensionConfig map : config.getDimensions())
		{
			File mapDir = new File(exportDir, "Scripts/" + map.getId());
			FileUtils.ensureExists(mapDir);

			BiomeCache biomeCache = CacheUtil.createBiomeCache(config, map, hashAlgorithm);

			// Create the world for this map
			World world = new World(rasteriser, map, biomeCache, playerSkinCache, config);
			
			// TODO: Load custom blocks here
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");
			
			preProcess(world, map, portalsFile, signsFile, viewsFile);
			
			// Output views
			outputViews(new File(mapDir, "views.js"), viewsFile, map);
			
			// Render views
			FileViewCache viewCache = CacheUtil.createViewCache(config.getCacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, config.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
		}
		
		Date endTime = new Date();
		String time = Util.getElapsedTime(startTime, endTime);
		
		outputChangedFile();
		
		log.info("View render complete - total time "+time);
		
		return new Result(abort, null);
	}
	
	public static void setupWorldForLayer(LayerConfig layer, World world)
	{
		log.info("Creating block registry");
		world.applyRenderOptions(layer);
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

	private WorldStats preProcess(World world, tectonicus.configuration.DimensionConfig map, File portalsFile, File signsFile, File viewsFile)
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
			
			stats = preProcess(world, map, portals, signs, views);
			
			log.debug("Found "+views.size()+" views");
		}
		catch (Exception e)
		{
			log.error("Exception: ", e);
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
		
		stats.setNumPlayers(world.getPlayers(map.getDimensionInfo()).size());
		
		return stats;
	}
	
	private WorldStats preProcess(World world, tectonicus.configuration.DimensionConfig map, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs, HddObjectListWriter<Sign> views)
	{
		// Pre-render pass - calc chunk hashes and project signs
		if (progressListener != null)
			progressListener.onTaskStarted(Task.CALCULATING_CHUNK_HASHES.toString());
		
		WorldStats worldStats = new WorldStats();
		
		regionHashStore = new RegionHashStore(new File(config.getCacheDir(), map.getId()));
		
		log.info("Discovering chunks...");
		//	Iterate over regions, then over chunks
		//		hash each chunk and store in region hashes file
		//		gather world stats and signs for each chunk
		
		final Date beginTime = new Date();
		if (world.isAlphaWorld()) {
			ChunkCoord[] alphaChunkCoords = world.getAlphaChunkCoords();
			ChunkCoord[] sortedAlphaChunks = Arrays.copyOf(alphaChunkCoords, alphaChunkCoords.length);
			Arrays.sort(sortedAlphaChunks, (lhs, rhs) -> {
				RegionCoord lhsRegion = RegionCoord.fromChunkCoord(lhs);
				RegionCoord rhsRegion = RegionCoord.fromChunkCoord(rhs);
				
				int regionXComp = Long.compare(lhsRegion.x, rhsRegion.x);
				if (regionXComp != 0)
					return regionXComp;
				
				int regionZComp = Long.compare(lhsRegion.z, rhsRegion.z);
				if (regionZComp != 0)
					return regionZComp;
				
				int chunkXComp = Long.compare(lhs.x, rhs.x);
				if (chunkXComp != 0)
					return chunkXComp;
				
				return Long.compare(lhs.z, rhs.z);
			});
			
			log.debug("Looking for alpha chunks in {}", world.getWorldDir().getAbsolutePath());
			
			//Group alpha chunks by "region" to use the regionHashStore and regionLoadQueue
			RegionCoord currentRegion = null;
			RegionLoadQueue regionLoadQueue = null;
			for (ChunkCoord coord : sortedAlphaChunks) {
				if (coord == null || !world.contains(coord))
					continue;
				
				RegionCoord regionCoord = RegionCoord.fromChunkCoord(coord);
				if (currentRegion == null || !currentRegion.equals(regionCoord)) {
					if (regionLoadQueue != null) {
						endRegion(portals, signs, views, regionLoadQueue, worldStats);
					}
					
					currentRegion = regionCoord;
					regionHashStore.startRegion(regionCoord);
					regionLoadQueue = new RegionLoadQueue(config.getNumDownsampleThreads());
				}
				
				Chunk c = null;
				try {
					c = world.loadChunk(coord, worldStats);
				} catch (Exception e) {
					log.error(String.format("Chunk %1$d,%2$d is probably corrupted.", coord.x, coord.z), e);
				}
				
				chunkSearch(world, map, portals, signs, views, c, worldStats, regionLoadQueue);
			}
			
			if (regionLoadQueue != null) {
				endRegion(portals, signs, views, regionLoadQueue, worldStats);
			}
		} else {
			RegionIterator it = world.createRegionIterator();
			
			log.debug("Looking for chunks in " + it.getBaseDir().getAbsolutePath());
			while (it.hasNext()) {
				File regionFile = it.next();
				if (regionFile != null && regionFile.length() > 0) {
					Region region = null;
					try {
						region = new Region(regionFile);
					} catch (Exception e) {
						log.error("Exception: ", e);
					}
					if (region != null) {
						// For every region...
						regionHashStore.startRegion(region.getRegionCoord());
						
						RegionLoadQueue regionLoadQueue = new RegionLoadQueue(config.getNumDownsampleThreads());
						
						for (ChunkCoord coord : region.getContainedChunkCoords()) {
							// For every chunk coord...
							Chunk c = null;
							try {
								c = region.loadChunk(coord, world.getBiomeCache(), world.getBlockFilter(), worldStats, world.getWorldInfo());
							} catch (Exception e) {
								// Catch exception, log it and skip the chunk
								log.error(String.format("Chunk %1$d,%2$d in region %3$d,%4$d is probably corrupted.", coord.x, coord.z, region.getRegionCoord().x, region.getRegionCoord().z), e);
							}
							
							chunkSearch(world, map, portals, signs, views, c, worldStats, regionLoadQueue);
						}
						
						endRegion(portals, signs, views, regionLoadQueue, worldStats);
					}
				}
			}
		}
		
		final Date endTime = new Date();
		final String searchTime = Util.getElapsedTime(beginTime, endTime);
		
		log.debug("\nFound " + worldStats.numChunks() + " chunks in total");
		log.debug("Chunk search took: " + searchTime);
		
		if (worldStats.numChunks() == 0) {
			// Uh oh, didn't find any chunks!
			// Print some debugging info to help people figure out what they're doing wrong
			
			log.error("Failed to find any chunks!");
			File inspectDir = world.getWorldDir();
			if (!world.isAlphaWorld()) {
				RegionIterator it = world.createRegionIterator();
				inspectDir = it.getBaseDir();
			}
			log.error("Contents of " + inspectDir.getAbsolutePath());
			File[] contents = inspectDir.listFiles();
			if (contents != null) {
				for (File f : contents) {
					log.error("\t" + f.getName());
				}
			}
		}
		
		return worldStats;
	}
	
	private void chunkSearch(World world, DimensionConfig map, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs, HddObjectListWriter<Sign> views, Chunk c, WorldStats worldStats, RegionLoadQueue regionLoadQueue) {
		if (c != null && c.getRawChunk().isFullChunk()) {
			worldStats.incNumChunks();
			if (discoveredTiles != null && world.contains(c.getCoord())) {
				int[] height = useFullHeightBounds ? new int[] {0, Minecraft.getChunkHeight()}
						: c.getRawChunk().getRenderHeightBounds();
				if (height != null) {
					TileCoordBounds tileBounds = getTileBoundsForChunk(camera, c.getCoord(), tileWidth, tileHeight, height[0], height[1]);
					for (int x = tileBounds.min.x; x <= tileBounds.max.x; x++)
						for (int y = tileBounds.min.y; y <= tileBounds.max.y; y++)
							discoveredTiles.add(new TileCoord(x, y));
				}
			}
			
			ConcurrentLinkedQueue<ContainerEntity> chests = world.getChests();
			Queue<BedEntity> beds = world.getBeds();
			findBeacons(c.getRawChunk(), world.getBeacons(), map.getBeaconFilter(),
					beacon -> world.getBeaconLevels(c.getRawChunk(), beacon));
			
			try {
				// MessageDigest is not thread safe, so we need to create a new instance for each chunk processed in separate thread...
				regionLoadQueue.load(c, regionHashStore, (MessageDigest) hashAlgorithm.clone(), map, portals, signs, views, chests, beds);
			} catch (CloneNotSupportedException e) {
				log.error("Exception: ", e);
			}
		}
	}
	
	private void endRegion(HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs, HddObjectListWriter<Sign> views, RegionLoadQueue regionLoadQueue, WorldStats worldStats) {
		regionLoadQueue.waitUntilFinished();
		
		System.out.print("\tfound " + worldStats.numChunks() + " chunks so far\r"); //prints a carriage return after line
		log.trace("found {} chunks so far", worldStats.numChunks());
		
		try {
			portals.flush();
			signs.flush();
			views.flush();
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
		
		regionHashStore.endRegion();
	}
	
	private void renderBaseTiles(World world, tectonicus.configuration.DimensionConfig map, LayerConfig layer, File layerDir, HddTileList tiles, TileCache tileCache)
	{
		if (abort)
			return;
		
		progressListener.onTaskStarted(Task.RENDER_BASE_TILES.toString());
		
		final int zoom = map.getClosestZoomSize();
		final ImageFormat imageFormat = layer.getImageFormat();
		
		log.debug("Base render is at zoom "+zoom+" with "+tileWidth+"x"+tileHeight+" tiles");
		
		setupInitialCamera(map);
		
		int done = 0;

		ImageWriteQueue imageWriteQueue = new ImageWriteQueue(config.getNumDownsampleThreads());

		for (TileCoord t : tiles) {
			System.out.print("Rendering tile @ " + t.x + "," + t.y + " (tile " + (done + 1) + " of " + tiles.size() + ")          \r"); //prints a carriage return after line
			log.trace("Rendering tile @ {},{} (tile {} of {})", t.x, t.y, done+1, tiles.size());
			progressListener.onTaskUpdate(done, tiles.size());

			setupCameraForTile(camera, t, tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), zoom);

			rasteriser.resetState();
			rasteriser.clear(layer.getBackgroundColorRGB());

			world.draw(camera, false, true);

			File outputFile = getImageFile(layerDir, t.x, t.y, imageFormat);
			BufferedImage tileImage = rasteriser.takeScreenshot(0, 0, tileWidth, tileHeight, imageFormat);
			if (tileImage != null) {
				boolean empty = imageFormat.hasAlpha() && ImageUtils.isFullyTransparent(tileImage);
				if (empty) {
					try {
						Files.deleteIfExists(outputFile.toPath());
					} catch (IOException e) {
						throw new IllegalStateException("Could not remove empty tile " + outputFile, e);
					}
				} else {
					imageWriteQueue.write(outputFile, tileImage, imageFormat, layer.getImageCompressionLevel());
					changedFileList.writeLine(outputFile.getAbsolutePath());
				}
				tileCache.writeImageCache(t, empty);
			} else {
				log.error("Error: Rasteriser.takeScreenshot gave us a null image (width:" + tileWidth + " height:" + tileHeight + " format:" + imageFormat + ")");
			}

			done++;

			if (abort)
				break;
		}

		imageWriteQueue.waitUntilFinished();

		log.info("\nBase tile render complete");
	}

	public static void setupCameraForTile(OrthoCamera camera, TileCoord tile, final int tileWidth, final int tileHeight, final float cameraAngleRads, final float cameraElevationRads, final int zoom)
	{
		camera.lookAtTile(tile.x, tile.y, zoom, cameraAngleRads, cameraElevationRads);
		camera.apply();
	}
	
	private void setupInitialCamera(tectonicus.configuration.DimensionConfig map)
	{
		final int zoom = map.getClosestZoomSize();
		
		camera.lookAt(0, 0, 0, zoom, map.getCameraAngleRad(), map.getCameraElevationRad());
		camera.apply();
		setupCameraForTile(camera, new TileCoord(0, 0), tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), map.getClosestZoomSize());
	}
	
	static TileCoordBounds getTileBoundsForChunk(OrthoCamera camera, ChunkCoord coord, int tileWidth, int tileHeight) {
		return getTileBoundsForChunk(camera, coord, tileWidth, tileHeight, 0, Minecraft.getChunkHeight());
	}

	static TileCoordBounds getTileBoundsForChunk(OrthoCamera camera, ChunkCoord coord, int tileWidth, int tileHeight, int minHeight, int maxHeight) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		// Convert directly from double screen positions to tile indices. Pixel positions
		// can exceed the integer range near the world border at high zoom levels.
		for (int x = 0; x <= 1; x++) {
			for (int y = 0; y <= 1; y++) {
				for (int z = 0; z <= 1; z++) {
					Vector2d screen = camera.projectd(((double)coord.x + x) * RawChunk.WIDTH,
							y == 0 ? minHeight : maxHeight, ((double)coord.z + z) * RawChunk.DEPTH);
					int tileX = (int)Math.floor(screen.x / tileWidth);
					int tileY = (int)Math.floor(screen.y / tileHeight);
					minX = Math.min(minX, tileX);
					maxX = Math.max(maxX, tileX);
					minY = Math.min(minY, tileY);
					maxY = Math.max(maxY, tileY);
				}
			}
		}
		return new TileCoordBounds(new TileCoord(minX, minY), new TileCoord(maxX, maxY));
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
	
	private TileCoordBounds downsample(HddTileList baseTiles, HddTileList changedTiles, File exportDir, LayerConfig layer, File baseDir, TileCache tileCache)
	{
		final Date downsampleStart = new Date();
		
		int zoomLevel = numZoomLevels - 1;

		if (!tileCache.hasCreatedDownsampleCache()) {
			tileCache.calculateDownsampledTileCoordinates(baseTiles, zoomLevel);
			// Removed tiles may have parents beyond the new candidate bounds.
			tileCache.calculateDownsampledTileCoordinates(changedTiles, zoomLevel);
		}
                else {
                        tileCache.calculateDownsampledTileCoordinates(changedTiles, zoomLevel);
                }
		
		File prevDir = baseDir;
		HddTileList prevTiles = baseTiles;
		while (zoomLevel >= 0)
		{
			if (abort)
				break;

			progressListener.onTaskStarted(Task.DOWNSAMPLING + " zoom level " + zoomLevel);
			
			File nextDir = DirUtils.getZoomDir(exportDir, layer, zoomLevel);
			HddTileList nextTiles = tileCache.findTilesForDownsampling(hddTileListFactory, zoomLevel, nextDir, layer.getImageFormat());
                        if (nextTiles.size() == 0) {
				log.info("\tNo downsampling needed");
			} else {
				if (!tileCache.isUsingExistingCache()) {
					FileUtils.deleteDirectory(nextDir);
				}
				if (!nextDir.exists()) {
					final boolean mkOk = nextDir.mkdirs();
					if (!mkOk)
						throw new RuntimeException("Couldn't create dir:" + nextDir.getAbsolutePath());
				}

				log.debug("\tDownsampling {} tiles into {} tiles", prevTiles.size(), nextTiles.size());

				Downsampler downsampler = new Downsampler(config.getNumDownsampleThreads(), changedFileList);
				downsampler.downsample(prevDir, nextDir, nextTiles, layer, tileWidth, tileHeight, progressListener, tileCache, zoomLevel);
			}
			
			zoomLevel--;
			prevDir = nextDir;
			prevTiles = nextTiles;
		}
		
		final Date downsampleEnd = new Date();
		final String downsampleTime = Util.getElapsedTime(downsampleStart, downsampleEnd);
		log.debug("Downsampling took "+downsampleTime);
		
		TileCoord min = baseTiles.size() == 0 ? new TileCoord(0, 0) : baseTiles.getAbsoluteMinCoord();
		TileCoord max = baseTiles.size() == 0 ? new TileCoord(0, 0) : baseTiles.getAbsoluteMaxCoord();
		for (int zoom = 0; zoom < numZoomLevels; zoom++) {
			min = new TileCoord(Math.floorDiv(min.x, 2), Math.floorDiv(min.y, 2));
			max = new TileCoord(Math.floorDiv(max.x, 2), Math.floorDiv(max.y, 2));
		}
		return new TileCoordBounds(min, max);
	}

	public static class TileCoordBounds
	{
		public TileCoord min;
		public TileCoord max;
		
		public TileCoordBounds(TileCoord min, TileCoord max)
		{
			this.min = min;
			this.max = max;
		}
	}
	
	private void outputChangedFile() {
		log.info("Writing changed file list...");
		progressListener.onTaskStarted(Task.OUTPUT_CHANGED_LIST.toString());
		
		changedFileList.close();
		changedFileList = null;
	}
	
	public static File getImageFile(File dir, final int x, final int y, ImageFormat imageFormat)
	{
		final int xBin = x % 16;
		final int yBin = y % 16;
		File dir1 = new File(dir, ""+xBin);
		File dir2 = new File(dir1, ""+yBin);
		return new File(dir2, "tile_"+x+"_"+y+"."+imageFormat.getExtension());
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
}
