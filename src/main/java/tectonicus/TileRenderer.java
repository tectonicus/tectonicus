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
import tectonicus.cache.BiomeCache;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.FileTileCache;
import tectonicus.cache.FileViewCache;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.cache.RegionHashStore;
import tectonicus.cache.TileCache;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Configuration.RenderStyle;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.DirUtils;
import tectonicus.util.FileUtils;
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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static tectonicus.Version.VERSION_13;
import tectonicus.raw.ContainerEntity;
import static tectonicus.util.OutputResourcesUtil.outputBeds;
import static tectonicus.util.OutputResourcesUtil.outputChests;
import static tectonicus.util.OutputResourcesUtil.outputContents;
import static tectonicus.util.OutputResourcesUtil.outputHtml;
import static tectonicus.util.OutputResourcesUtil.outputHtmlResources;
import static tectonicus.util.OutputResourcesUtil.outputIcons;
import static tectonicus.util.OutputResourcesUtil.outputPlayers;
import static tectonicus.util.OutputResourcesUtil.outputPortals;
import static tectonicus.util.OutputResourcesUtil.outputRenderStats;
import static tectonicus.util.OutputResourcesUtil.outputRespawnAnchors;
import static tectonicus.util.OutputResourcesUtil.outputSigns;
import static tectonicus.util.OutputResourcesUtil.outputViews;

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
	
	private final Configuration config;
	
	private final MessageDigest hashAlgorithm;
	
	private final int tileWidth;
	private final int tileHeight;
	
	private final int numZoomLevels;
	
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
	
	public Rasteriser getRasteriser()
	{
		return rasteriser;
	}
	
	public Result output()
	{
		progressListener.onTaskStarted(Task.STARTING_RENDERER.toString());
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(config.getCacheDir());
		
		TempArea tempArea = new TempArea( new File(config.getCacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(config.getOutputDir(), "changed.txt"));
		
		for (tectonicus.configuration.Map map : config.getMaps())
		{
			// Clear shared state?
			// ..
			
			// Clear region cache?
			// ..
			
			File mapDir = new File(exportDir, map.getId());
			FileUtils.ensureExists(mapDir);

			BiomeCache biomeCache = CacheUtil.createBiomeCache(config, map, hashAlgorithm);

			// Create the world for this map
			World world = new World(rasteriser, map, biomeCache, playerSkinCache, config);
			
			// Setup camera
			setupInitialCamera(map);
			
			WorldVectors worldVectors = WorldVectors.calcWorldVectors(camera);
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");

			WorldStats worldStats = preProcess(world, map, portalsFile, signsFile, viewsFile);
			
			// Find visible tiles
			HddTileList visibleTiles = findVisibleTiles(world, camera, worldStats.numChunks());
			
			
			// Figure out which tiles we need to render
			progressListener.onTaskStarted(Task.FIND_CHANGED_TILES.toString());

			// Output entity javascript for creating map markers
			outputSigns(new File(mapDir, "signs.js"), signsFile, map);
			outputPlayers(new File(mapDir, "players.js"), new File(exportDir, "Images/PlayerIcons/"), map, world.getPlayers(map.getDimension()), playerIconAssembler);
			outputBeds(mapDir, map, world.getAllPlayers());
			outputRespawnAnchors(mapDir, map, world.getAllPlayers());
			List<Portal> portals = outputPortals(new File(mapDir, "portals.js"), portalsFile, map);
			worldStats.setNumPortals(portals.size());
			outputViews(new File(mapDir, "views.js"), viewsFile, map);
			outputChests(new File(mapDir, "chests.js"), map, world.getChests());

			// Render views
			FileViewCache viewCache = CacheUtil.createViewCache(config.getCacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, config.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
			
			TileCoordBounds bounds = null;
			
			if (map.getLayers().isEmpty())
				log.warn("No layers found!!!");
			
			for (Layer layer : map.getLayers())
			{
				// Setup per-layer config
				setupWorldForLayer(layer, world);
				
				// Set new tile cache for this layer
				String optionString = FileTileCache.calcOptionsString(config);
				TileCache tileCache = CacheUtil.createTileCache(config.useCache(), optionString, layer.getImageFormat(), config.getCacheDir(), map, layer, hashAlgorithm);
			
				File baseTilesDir = DirUtils.getZoomDir(exportDir, layer, numZoomLevels);
				FileUtils.ensureExists(baseTilesDir);

				// Find changed tiles
				HddTileList changedTiles = tileCache.findChangedTiles(hddTileListFactory, visibleTiles, regionHashStore, world, map, camera, map.getClosestZoomSize(), tileWidth, tileHeight, baseTilesDir);
				
				// Trim changed tiles to size
				changedTiles = trimTileList(changedTiles, config.getMaxTiles());
				
				// Render base tiles
				renderBaseTiles(world, map, layer, baseTilesDir, changedTiles, tileCache);

				// Create downsampled layers
				bounds = downsample(visibleTiles, changedTiles, exportDir, layer, baseTilesDir, tileCache);
				tileCache.closeTileCache();
			}
			
			outputIcons(exportDir, config, map, world, rasteriser);
			
			// Output world stats
			worldStats.outputBlockStats(new File(mapDir, "blockStats.js"), map.getId(), world.getBlockTypeRegistry());
			worldStats.outputWorldStats(new File(mapDir, "worldStats.js"), map.getId());
			
			// Output world vectors for this camera config
			worldVectors.outputWorldVectors(new File(mapDir, "worldVectors.js"), map, bounds, world,
					worldStats.numChunks(), portals, numZoomLevels, tileWidth, tileHeight);
		}

		// TODO: Should only load texture pack once and share between this and world loading
		outputHtmlResources(new TexturePack(rasteriser, config.minecraftJar(), config.getTexturePack(), config.getMap(0).getModJars(), config),
				playerIconAssembler, config, exportDir, numZoomLevels, tileWidth, tileHeight);
		
		outputContents(new File(new File(exportDir, "Scripts"), "contents.js"), config);
		
		
		// Output html
		File outputHtmlFile = null;
		try {
			progressListener.onTaskStarted(Task.OUTPUT_HTML.toString());
			outputHtmlFile = outputHtml(exportDir, config);
		} catch (IOException e) {
			e.printStackTrace();
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
		
		for (tectonicus.configuration.Map map : config.getMaps())
		{
			File mapDir = new File(exportDir, map.getId());
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

	private WorldStats preProcess(World world, tectonicus.configuration.Map map, File portalsFile, File signsFile, File viewsFile)
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
		
		stats.setNumPlayers(world.getPlayers(map.getDimension()).size());
		
		return stats;
	}
	
	private WorldStats preProcess(World world, tectonicus.configuration.Map map, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs, HddObjectListWriter<Sign> views)
	{
		// Pre-render pass - calc chunk hashes and project signs
		if (progressListener != null)
			progressListener.onTaskStarted(Task.CALCULATING_CHUNK_HASHES.toString());
		
		WorldStats worldStats = new WorldStats();
		
		regionHashStore = new RegionHashStore(config.getCacheDir());
		
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
			if (regionFile != null && regionFile.length() > 0)
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
                                        
                                        RegionLoadQueue regionLoadQueue = new RegionLoadQueue(config.getNumDownsampleThreads());
					
					ChunkCoord[] chunkCoords = region.getContainedChunks();
					for (ChunkCoord coord : chunkCoords)
					{
						// For every chunk coord...
						
						Chunk c = null;

                                                try
                                                {
                                                        c = region.loadChunk(coord, world.getBiomeCache(), world.getBlockFilter(), worldStats, world.getWorldInfo());
                                                }
                                                catch (Exception e)
                                                {
                                                        // Catch exception, log it and skip the chunk
                                                        log.error(String.format("Chunk %1$d,%2$d in region %3$d,%4$d is probably corrupted.", coord.x, coord.z, region.getRegionCoord().x, region.getRegionCoord().z));
                                                        e.printStackTrace();
                                                }
                                                
						if (c != null)
						{
                                                        worldStats.incNumChunks();
                                                        
                                                        ConcurrentLinkedQueue<ContainerEntity> chests = world.getChests();

                                                        try
                                                        {
                                                                // MessageDigest is not thread safe, so we need to create a new instance for each chunk processed in separate thread...
                                                                regionLoadQueue.load(c, regionHashStore, (MessageDigest)hashAlgorithm.clone(), map, portals, signs, views, chests);
                                                        }
                                                        catch (CloneNotSupportedException e)
                                                        {
                                                                e.printStackTrace();
                                                        }
						}
					}
					
                                        regionLoadQueue.waitUntilFinished();

                                        System.out.print("\tfound " + worldStats.numChunks() + " chunks so far\r"); //prints a carriage return after line
                                        log.trace("found {} chunks so far", worldStats.numChunks());
                                        
                                        try {
                                                portals.flush();
                                                signs.flush();
                                                views.flush();
                                        } catch (Exception e) {
                                                e.printStackTrace();
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

		ImageWriteQueue imageWriteQueue = new ImageWriteQueue(config.getNumDownsampleThreads());

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
			if (regionFile == null || regionFile.length() == 0)
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
	
	private TileCoordBounds downsample(HddTileList baseTiles, HddTileList changedTiles, File exportDir, Layer layer, File baseDir, TileCache tileCache)
	{
		final Date downsampleStart = new Date();
		
		int zoomLevel = config.getNumZoomLevels() - 1;

		if (!tileCache.hasCreatedDownsampleCache()) {
			tileCache.calculateDownsampledTileCoordinates(baseTiles, zoomLevel);
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
			
			HddTileList nextTiles = tileCache.findTilesForDownsampling(hddTileListFactory, zoomLevel);
			File nextDir = DirUtils.getZoomDir(exportDir, layer, zoomLevel);
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
		
		return new TileCoordBounds(prevTiles.getAbsoluteMinCoord(), prevTiles.getAbsoluteMaxCoord());
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
}
