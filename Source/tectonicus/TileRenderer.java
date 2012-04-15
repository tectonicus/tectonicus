/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.lwjgl.LWJGLException;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.cache.BiomeCache;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.FileTileCache;
import tectonicus.cache.FileViewCache;
import tectonicus.cache.NullTileCache;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.cache.RegionHashStore;
import tectonicus.cache.TileCache;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Configuration.Dimension;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.configuration.PlayerFilter;
import tectonicus.configuration.PortalFilter;
import tectonicus.configuration.SignFilter;
import tectonicus.configuration.Configuration.RenderStyle;
import tectonicus.configuration.ViewFilter;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.raw.LevelDat;
import tectonicus.raw.PlayerList;
import tectonicus.raw.Player;
import tectonicus.raw.RawChunk;
import tectonicus.raw.RawSign;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.FileUtils;
import tectonicus.util.JsObjectWriter;
import tectonicus.util.TempArea;
import tectonicus.util.Vector2f;
import tectonicus.util.Vector3d;
import tectonicus.util.Vector3l;
import tectonicus.world.Sign;
import tectonicus.world.World;
import tectonicus.world.filter.ExploredCaveFilter;
import tectonicus.world.subset.RegionIterator;
import tectonicus.world.subset.WorldSubsetFactory;

public class TileRenderer
{
	public enum Task
	{
		LoadingWorld,
		CalculatingChunkHashes,
		StartingRenderer,
		FindVisibleTiles,
		FindChangedTiles,
		RenderBaseTiles,
		Downsampling,
		OutputHtml,
		OutputChangedList,
		Finished
	};
	
	public static final Color clearColour = new Color(229, 227, 223);
	
	private final Configuration args;
	
	private final MessageDigest hashAlgorithm;
	
	private final int tileWidth, tileHeight;
	
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
		
		this.tileWidth = args.tileSize();
		this.tileHeight = args.tileSize();
		
		this.numZoomLevels = args.numZoomLevels();
		
		this.exportDir = args.outputDir();
		
		playerSkinCache = new PlayerSkinCache(args, hashAlgorithm);
		
		hddTileListFactory = new HddTileListFactory( new File(args.cacheDir(), "tileLists") );
		
		System.out.println("Creating player icon assembler");
		playerIconAssembler = new PlayerIconAssembler(playerSkinCache);
		
		memoryMonitor = new MemoryMonitor();
		
		{
			System.out.println("Initialising display...");
			
			rasteriser = RasteriserFactory.createRasteriser(args.getRasteriserType(), DisplayType.Offscreen, 2048, 2048, args.colourDepth(), args.alphaBits(), 16, args.numSamples());
		
			if (rasteriser != null)
			{
				System.out.println("Using rasteriser: "+rasteriser);
				rasteriser.printInfo();
			}
			else
			{
				throw new RuntimeException("Could not create drawing surface");
			}
		}
		
		System.out.println("Creating camera");
		camera = new OrthoCamera(rasteriser, tileWidth, tileHeight);
		
		System.out.println("TileRenderer init complete");
	}
	
	public void destroy()
	{
		System.out.println("Cleaning up...");
		
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
	
	public Result output() throws LWJGLException
	{
		progressListener.onTaskStarted(Task.StartingRenderer.toString());
		System.out.println("Starting tile renderer");
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(args.cacheDir());
		
		TempArea tempArea = new TempArea( new File(args.cacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(args.outputDir(), "changed.txt"));
		
		for (tectonicus.configuration.Map map : args.getMaps())
		{
			// Clear shared state?
			// ..
			
			// Clear region cache?
			// ..
			
			File mapDir = new File(exportDir, map.getId());
			FileUtils.ensureExists(mapDir);
			
			BiomeCache biomeCache = CacheUtil.createBiomeCache(args.minecraftJar(), args.cacheDir(), map, hashAlgorithm);
			
			WorldSubsetFactory subset = map.getWorldSubsetFactory();
			
			// Create the world for this map
			World world = new World(rasteriser, map.getWorldDir(), map.getDimension(), args.minecraftJar(), args.texturePack(),
									biomeCache, hashAlgorithm, args.getSinglePlayerName(), subset);
			
			// Setup camera
			setupInitialCamera(map);
			
			WorldVectors worldVectors = calcWorldVectors();
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");
			
			WorldStats worldStats = preProcess(world, map.getDimension(), map.getSignFilter(), map.getPortalFilter(), map.getViewFilter(), portalsFile, signsFile, viewsFile);
			
			// Find visible tiles
			HddTileList visibleTiles = findVisibleTiles(world, camera, worldStats.numChunks());
			
			
			// Figure out which tiles we need to render
			System.out.println("Finding changed tiles since last render...");
			progressListener.onTaskStarted(Task.FindChangedTiles.toString());
			
			// Output compass
			outputCompass(new File(mapDir, "Compass.png"), map, world.getBlockTypeRegistry(), world.getTexturePack());
			
			// Output signs
			outputSigns(new File(mapDir, "signs.js"), signsFile, map);
			
			// Output players
			outputPlayers(new File(mapDir, "players.js"), new File(exportDir, "Images"), map, map.getPlayerFilter(), world.players(map.getDimension()), world.getOps(), playerIconAssembler);
			
			// Output beds
			outputBeds(mapDir, map, map.getPlayerFilter(), world.players(null), world.getOps());
			
			// Output portals
			outputPortals(new File(mapDir, "portals.js"), portalsFile, map);
			
			// Output views
			outputViews(new File(mapDir, "views.js"), viewsFile, map, map.getViewConfig().getImageFormat());
			
			// Output world stats
			worldStats.outputBlockStats(new File(mapDir, "blockStats.js"), map.getId(), world.getBlockTypeRegistry());
			worldStats.outputWorldStats(new File(mapDir, "worldStats.js"), map.getId());
			
			// Render views
			FileViewCache viewCache = createViewCache(args.cacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, args.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
			
			TileCoordBounds bounds = null;
			
			for (Layer layer : map.getLayers())
			{
				// Setup per-layer config
				setupWorldForLayer(layer, world);
				
				// Set new tile cache for this layer
				String optionString = FileTileCache.calcOptionsString(args);
				TileCache tileCache = createTileCache(args.useCache(), optionString, layer.getImageFormat(), args.cacheDir(), map, layer, hashAlgorithm);
			
				File baseTilesDir = DirUtils.getZoomDir(exportDir, layer, numZoomLevels);
				FileUtils.ensureExists(baseTilesDir);
				
				// Find changed tiles
				HddTileList changedTiles = tileCache.findChangedTiles(hddTileListFactory, visibleTiles, regionHashStore, world, map, camera, map.getClosestZoomSize(), tileWidth, tileHeight, baseTilesDir);
				
				// Trim changed tiles to size
				changedTiles = trimTileList(changedTiles, args.maxTiles());
				
				// Render base tiles
				renderBaseTiles(world, map, layer.getImageFormat(), layer.getImageCompressionLevel(), baseTilesDir, changedTiles, tileCache);
				
				// Create downsampled layers
				bounds = downsample(changedTiles, exportDir, layer, baseTilesDir, tileCache);
			}
			
			// Output world vectors for this camera config
			outputWorldVectors( new File(mapDir, "worldVectors.js"), map.getId(), worldVectors, bounds, world.getLevelDat(), worldStats.numChunks(), world.numPlayers());
		}
		
		// Output html resources
		// TODO: Should only load texture pack once and share between this and world loading
		outputHtmlResources( new TexturePack(rasteriser, args.minecraftJar(), args.texturePack()), playerIconAssembler );
		
		outputContents(new File(new File(exportDir, "Scripts"), "contents.js"), args);
		
		
		// Output html
		final File outputHtmlFile = outputHtml();
		
		// ----
		
		Date endTime = new Date();
		String time = Util.getElapsedTime(startTime, endTime);
		
		outputRenderStats(time);
		
		outputChangedFile();
		
		System.out.println("Render complete - total time "+time);
		
		return new Result(abort, outputHtmlFile);
	}
	
	// Just renders views
	public Result outputViews() throws LWJGLException
	{
		progressListener.onTaskStarted(Task.StartingRenderer.toString());
		System.out.println("Starting view renderer");
		
		Date startTime = new Date();
		
		FileUtils.ensureExists(exportDir);
		FileUtils.ensureExists(args.cacheDir());
		
		TempArea tempArea = new TempArea( new File(args.cacheDir(), "temp") );
		
		changedFileList = new ChangeFile(new File(args.outputDir(), "changed.txt"));
		
		for (tectonicus.configuration.Map map : args.getMaps())
		{
			File mapDir = new File(exportDir, map.getId());
			FileUtils.ensureExists(mapDir);
			
			BiomeCache biomeCache = CacheUtil.createBiomeCache(args.minecraftJar(), args.cacheDir(), map, hashAlgorithm);
			
			WorldSubsetFactory subset = map.getWorldSubsetFactory();
			
			// Create the world for this map
			World world = new World(rasteriser, map.getWorldDir(), map.getDimension(), args.minecraftJar(), args.texturePack(),
									biomeCache, hashAlgorithm, args.getSinglePlayerName(), subset);
			
			// TODO: Load custom blocks here
			
			// FIXME: These paths need to change per map
			File portalsFile = tempArea.generateTempFile("portals", ".list");
			File signsFile = tempArea.generateTempFile("signs", ".list");
			File viewsFile = tempArea.generateTempFile("views", ".list");
			
			preProcess(world, map.getDimension(), map.getSignFilter(), map.getPortalFilter(), map.getViewFilter(), portalsFile, signsFile, viewsFile);
			
			// Output views
			outputViews(new File(mapDir, "views.js"), viewsFile, map, map.getViewConfig().getImageFormat());
			
			// Render views
			FileViewCache viewCache = createViewCache(args.cacheDir(), map, tempArea, hashAlgorithm, regionHashStore);
			ViewRenderer viewRenderer = new ViewRenderer(rasteriser, viewCache, args.getNumDownsampleThreads(), map.getViewConfig());
			viewRenderer.output(world, mapDir, viewsFile, changedFileList);
		}
		
		Date endTime = new Date();
		String time = Util.getElapsedTime(startTime, endTime);
		
		outputChangedFile();
		
		System.out.println("View render complete - total time "+time);
		
		return new Result(abort, null);
	}
	
	public static void setupWorldForLayer(Layer layer, World world)
	{
		world.loadBlockRegistry(layer.getCustomBlockConfig(), layer.useDefaultBlockConfig());
		
		world.setLightStyle(layer.getLightStyle());
		world.setDefaultBlockId(BlockIds.AIR);
		
		if (layer.getRenderStyle() == RenderStyle.Cave)
		{
			world.setDefaultBlockId(BlockIds.STONE);
			world.setBlockMaskFactory( new CaveMaskFactory() );
		}
		else if (layer.getRenderStyle() == RenderStyle.ExploredCaves)
		{
		//	world.setDefaultBlockId(BlockIds.STONE);
			world.setBlockFilter( new ExploredCaveFilter() );
			world.setBlockMaskFactory( new CaveMaskFactory() );
		}
		else if (layer.getRenderStyle() == RenderStyle.Nether)
		{
			world.setBlockFilter( new NetherBlockFilter() );
		}	
	}

	private WorldStats preProcess(World world, Dimension dimension, SignFilter signFilter, PortalFilter portalFilter, ViewFilter viewFilter, File portalsFile, File signsFile, File viewsFile)
	{
		WorldStats stats = null;
		
		HddObjectListWriter<Portal> portals = null;
		HddObjectListWriter<Sign> signs = null;
		HddObjectListWriter<Sign> views = null;
		
		try
		{
			portals = new HddObjectListWriter<Portal>(portalsFile, true);
			signs = new HddObjectListWriter<Sign>(signsFile, true);
			views = new HddObjectListWriter<Sign>(viewsFile, true);
			
			stats = preProcess(world, signFilter, portalFilter, viewFilter, portals, signs, views);
			
			System.out.println("Found "+views.size()+" views");
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
	
	private WorldStats preProcess(World world, SignFilter signFilter, PortalFilter portalFilter, ViewFilter viewFilter, HddObjectListWriter<Portal> portals, HddObjectListWriter<Sign> signs, HddObjectListWriter<Sign> views)
	{
		// Pre-render pass - calc chunk hashes and project signs
		if (progressListener != null)
			progressListener.onTaskStarted(Task.CalculatingChunkHashes.toString());
		
		WorldStats worldStats = new WorldStats();
		
		regionHashStore = new RegionHashStore(args.cacheDir());
		
		System.out.println("Discovering chunks...");
		//	Iterate over regions, then over chunks
		//		hash each chunk and store in region hashes file
		//		gather world stats and signs for each chunk
		
		RegionIterator it = world.createRegionIterator();
		
		System.out.println("Looking for chunks in "+it.getBaseDir().getAbsolutePath());
		
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
					
					regionHashStore.startRegion(region.getCoord());
					
					ChunkCoord[] chunkCoords = region.getContainedChunks();
					for (ChunkCoord coord : chunkCoords)
					{
						// For every chunk coord...
						
						Chunk c = region.loadChunk(coord, world.getBiomeCache(), world.getBlockFilter());
						if (c != null)
						{
							c.collectStats(worldStats);
							
							c.calculateHash(hashAlgorithm);
							regionHashStore.addHash(c.getCoord(), c.getHash());
							
							worldStats.incNumChunks();
							
							findSigns(c.getRawChunk(), signs, signFilter);
							
							findPortals(c.getRawChunk(), portals, portalFilter, worldStats);
							
							findViews(c.getRawChunk(), views, viewFilter);
							
							if (worldStats.numChunks() % 100 == 0)
								System.out.println("\tfound "+worldStats.numChunks()+" chunks so far");
						}
					}
					
					regionHashStore.endRegion();
				}
			}
		}
		
		System.out.println("Found "+worldStats.numChunks()+" chunks in total");
		
		if (worldStats.numChunks() == 0)
		{
			// Uh oh, didn't find any chunks!
			// Print some debugging info to help people figure out what they're doing wrong
			
			System.out.println("Failed to find any chunks!");
			System.out.println("Contents of "+it.getBaseDir().getAbsolutePath());
			File[] contents = it.getBaseDir().listFiles();
			if (contents != null)
			{
				for (File f : contents)
				{
					System.out.println("\t"+f.getName());
				}
			}
		}
		
		return worldStats;
	}
	
	private static void findSigns(RawChunk chunk, HddObjectListWriter<Sign> signs, SignFilter filter)
	{
		try
		{
			for (RawSign s : chunk.getSigns())
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
	
	private static void findPortals(RawChunk chunk, HddObjectListWriter<Portal> portals, PortalFilter filter, WorldStats stats)
	{
		try
		{
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int y=1; y<RawChunk.HEIGHT-1; y++)
				{
					for (int z=0; z<RawChunk.DEPTH; z++)
					{
						final int id = chunk.getBlockId(x, y, z);
						final int above = chunk.getBlockId(x, y+1, z);
						final int below = chunk.getBlockId(x, y-1, z);
						if (id == BlockIds.PORTAL && above == BlockIds.PORTAL && below == BlockIds.PORTAL)
						{
							// Must be one of the two center portal blocks
							final int sum = x + z;
							if (sum % 2 == 0)
							{
								// A portal block!
								
								stats.incNumPortals();
								
								ChunkCoord coord = chunk.getChunkCoord();
								Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + x,
															y,
															coord.z * RawChunk.DEPTH + z);
								
								if (filter.passesFilter(coord, pos))
								{
									portals.add( new Portal(pos.x, pos.y, pos.z) );
								}
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
			for (RawSign s : chunk.getSigns())
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
	
	private void renderBaseTiles(World world, tectonicus.configuration.Map map, ImageFormat imageFormat, final float imageCompressionLevel, File layerDir, HddTileList tiles, TileCache tileCache)
	{
		if (abort)
			return;
		
		progressListener.onTaskStarted(Task.RenderBaseTiles.toString());
		
		final int zoom = map.getClosestZoomSize();
		
		System.out.println("Base render is at zoom "+zoom+" with "+tileWidth+"x"+tileHeight+" tiles");
		
		setupInitialCamera(map);
		
		progressListener.onTaskStarted(Task.RenderBaseTiles.toString());
		
		int done = 0;
		
		ImageWriteQueue imageWriteQueue = new ImageWriteQueue(args.getNumDownsampleThreads());
		
		for (TileCoord t : tiles)
		{
			System.out.println("Rendering tile @ "+t.x+","+t.y+" (tile "+(done+1)+" of "+tiles.size()+")");
			progressListener.onTaskUpdate(done, tiles.size());
			
			setupCameraForTile(camera, t, tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), zoom);

			rasteriser.resetState();
			rasteriser.clear(clearColour);
			
			world.draw(camera, false, true);
			
			File outputFile = getImageFile(layerDir, t.x, t.y, imageFormat);
			BufferedImage tileImage = rasteriser.takeScreenshot(0, 0, tileWidth, tileHeight, imageFormat);
			if (tileImage != null)
			{
				imageWriteQueue.write(outputFile, tileImage, imageFormat, imageCompressionLevel);
			}
			else
			{
				System.err.println("Error: Rasteriser.takeScreenshot gave us a null image (width:"+tileWidth+" height:"+tileHeight+" format:"+imageFormat+")");
			}
			
			tileCache.writeImageCache(t);
			
			changedFileList.writeLine( outputFile.getAbsolutePath() );
			
			done++;
			
			if (abort)
				break;
		}
		
		imageWriteQueue.waitUntilFinished();
		
		System.out.println("Base tile render complete");
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
		
		System.out.println("Finding visible tiles...");
		progressListener.onTaskStarted(Task.FindVisibleTiles.toString());
		
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
						BoundingBox bounds = new BoundingBox(new Vector3l(coord.x * RawChunk.WIDTH, 0, coord.z * RawChunk.DEPTH), RawChunk.WIDTH, RawChunk.HEIGHT, RawChunk.DEPTH);
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
						
						// Find tiles that scren rect overlaps
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
							System.out.println(percentage+"%");
						}
						progressListener.onTaskUpdate(count, numChunks);
					}
				}
			}
			
			if (abort)
				break;
		}
		System.out.println("100%");
		
		System.out.println("found "+visible.size()+" total tiles to output");
		
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
		
		int zoomLevel = args.numZoomLevels() - 1;
		
		File prevDir = baseDir;
		HddTileList prevTiles = baseTiles;
		while (zoomLevel >= 0)
		{
			if (abort)
				break;
			
			System.out.println("Downsampling to create zoom level "+zoomLevel);
			progressListener.onTaskStarted(Task.Downsampling.toString() + " level " + zoomLevel);
			
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
			
			System.out.println("\tDownsampling "+prevTiles.size()+" tiles into "+nextTiles.size()+" tiles");
			
			Downsampler downsampler = new Downsampler(args.getNumDownsampleThreads(), changedFileList);
			downsampler.downsample(prevDir, nextDir, nextTiles, layer.getImageFormat(), layer.getImageCompressionLevel(), tileWidth, tileHeight, progressListener);
			
			zoomLevel--;
			prevDir = nextDir;
			prevTiles = nextTiles;
		}
		
		final Date downsampleEnd = new Date();
		final String downsampleTime = Util.getElapsedTime(downsampleStart, downsampleEnd);
		System.out.println("Downsampling took "+downsampleTime);
		
		return new TileCoordBounds(prevTiles.getAbsoluteMinCoord(), prevTiles.getAbsoluteMaxCoord());
	}
	
	private static TileCache createTileCache(final boolean useCache, String optionString, ImageFormat imageFormat, File rootCacheDir, tectonicus.configuration.Map map, Layer layer, MessageDigest hashAlgorithm)
	{
		if (useCache)
		{
			File subDir = new File(rootCacheDir, "tileHashes");
			File mapDir = new File(subDir, layer.getMapId());
			File layerDir = new File(mapDir, layer.getId());
			
			return new FileTileCache(layerDir, imageFormat, map, layer, optionString, hashAlgorithm);
		}
		else
		{
			return new NullTileCache();
		}
	}
	
	private static FileViewCache createViewCache(File cacheDir, tectonicus.configuration.Map map, TempArea tempArea, MessageDigest hashAlgorithm, RegionHashStore regionHashStore)
	{
		File viewsCache = new File(cacheDir, "views");
		File mapViewsCache = new File(viewsCache, map.getId());
		
		FileViewCache cache = new FileViewCache(mapViewsCache, tempArea, hashAlgorithm, regionHashStore);
		return cache;
	}
	
	private static class TileCoordBounds
	{
		public TileCoord min, max;
		
		public TileCoordBounds(TileCoord min, TileCoord max)
		{
			this.min = min;
			this.max = max;
		}
	}
	
	private static HddTileList findNextZoomTiles(HddTileList baseTiles, HddTileListFactory factory)
	{
		System.out.println("\tScanning for next zoom tiles...");
		
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
		System.out.println("Exporting stats...");
		
		File statsFile = new File(new File(exportDir, "Scripts"), "stats.js");
		if (statsFile.exists())
			statsFile.delete();
		
		System.out.println("Outputting stats to "+statsFile.getAbsolutePath());
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm z");
		final String renderedDateStr = dateFormat.format( new Date() );
		final String renderedTimeStr = timeFormat.format( new Date() );
		
		JsObjectWriter jsWriter = null;
		try
		{
			jsWriter = new JsObjectWriter(statsFile);
			
			Map<String, Object> stats = new HashMap<String, Object>();
			
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
	
	private File outputHtml()
	{
		System.out.println("Exporting html...");
		progressListener.onTaskStarted(Task.OutputHtml.toString());
		
		File outputHtmlFile = new File(exportDir, args.getOutputHtmlName());
		System.out.println("\twriting html to "+outputHtmlFile.getAbsolutePath());
		
		final int scale = (int)Math.pow(2, numZoomLevels);
		
		InputStream in = null;
		try
		{
			in = getClass().getClassLoader().getResourceAsStream("mapWithSigns.html");
			Scanner scanner = new Scanner(in);
			
			OutputStream out = new FileOutputStream(outputHtmlFile);
			PrintWriter writer = new PrintWriter(out);
			
			while (scanner.hasNext())
			{
				String line = scanner.nextLine();
				String outLine = "";
				
				ArrayList<Util.Token> tokens = Util.split(line);
				
				while (!tokens.isEmpty())
				{
					Util.Token first = tokens.remove(0);
					if (first.isReplaceable)
					{
						if (first.value.equals("tileWidth"))
						{
							outLine += tileWidth;
						}
						else if (first.value.equals("tileHeight"))
						{
							outLine += tileHeight;
						}
					/*	else if (first.value.equals("mapXMin"))
						{
							outLine += mapXMin;
						}
						else if (first.value.equals("mapYMin"))
						{
							outLine += mapYMin;
						}
						else if (first.value.equals("mapWidth"))
						{
							outLine += (mapXMax - mapXMin);
						}
						else if (first.value.equals("mapHeight"))
						{
							outLine += (mapYMax - mapYMin);
						}
					*/	else if (first.value.equals("maxZoom"))
						{
							outLine += numZoomLevels;
						}
						else if (first.value.equals("mapCoordScaleFactor"))
						{
							outLine += scale;
							outLine += ".0"; // Append .0 so that it's treated as float in the javascript
						}
				/*		else if (first.value.equals("origin"))
						{
							outLine += (worldVectors.origin.x / scale);
							outLine += ", ";
							outLine += (worldVectors.origin.y / scale);
						}
						else if (first.value.equals("xAxis"))
						{
							outLine += (worldVectors.xAxis.x / scale);
							outLine += ", ";
							outLine += (worldVectors.xAxis.y / scale);
						}
						else if (first.value.equals("yAxis"))
						{
							outLine += (worldVectors.yAxis.x / scale);
							outLine += ", ";
							outLine += (worldVectors.yAxis.y / scale);
						}
						else if (first.value.equals("zAxis"))
						{
							outLine += (worldVectors.zAxis.x / scale);
							outLine += ", ";
							outLine += (worldVectors.zAxis.y / scale);
						}
						else if (first.value.equals("mapXUnit"))
						{
							outLine += (worldVectors.mapXUnit.x * scale);
							outLine += ", ";
							outLine += (worldVectors.mapXUnit.y * scale);
						}
						else if (first.value.equals("mapYUnit"))
						{
							outLine += (worldVectors.mapYUnit.x * scale);
							outLine += ", ";
							outLine += (worldVectors.mapYUnit.y * scale);
						}
					*/	else if (first.value.equals("showSpawn"))
						{
							outLine += args.showSpawn();
						}
					/*	else if (first.value.equals("spawnX"))
						{
							outLine += levelDat.getSpawnPosition().x;
						}
						else if (first.value.equals("spawnY"))
						{
							outLine += levelDat.getSpawnPosition().y;
						}
						else if (first.value.equals("spawnZ"))
						{
							outLine += levelDat.getSpawnPosition().z;
						}
					*/
						else if (first.value.equals("signsInitiallyVisible"))
						{
							outLine += args.areSignsInitiallyVisible();
						}
						else if (first.value.equals("playersInitiallyVisible"))
						{
							outLine += args.arePlayersInitiallyVisible();
						}
						else if (first.value.equals("portalsInitiallyVisible"))
						{
							outLine += args.arePortalsInitiallyVisible();
						}
						else if (first.value.equals("bedsInitiallyVisible"))
						{
							outLine += args.areBedsInitiallyVisible();
						}
						else if (first.value.equals("spawnInitiallyVisible"))
						{
							outLine += args.isSpawnInitiallyVisible();
						}
						else if (first.value.equals("viewsInitiallyVisible"))
						{
							outLine += args.areViewsInitiallyVisible();
						}
						else if (first.value.equals("includes"))
						{
							String templateStart = "		<script type=\"text/javascript\" src=\"";
							String templateEnd = "\"></script>\n";
							
							for (tectonicus.configuration.Map map : args.getMaps())
							{
								outLine += templateStart;
								outLine += map.getId()+"/players.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/beds.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/portals.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/signs.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/views.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/worldVectors.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/blockStats.js";
								outLine += templateEnd;
								
								outLine += templateStart;
								outLine += map.getId()+"/worldStats.js";
								outLine += templateEnd;
								
								// Any per layer includes?
							}
						}
					}
					else
					{
						outLine += first.value;
					}
				}
				
				writer.write(outLine + "\n");
			}
			
			writer.flush();
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();
			}
			catch (Exception e) {}
		}
		
		return outputHtmlFile;
	}
	
	private static void outputMergedJs(File outFile, ArrayList<String> inputResources)
	{
		InputStream in = null;
		OutputStream out = null;
		
		try
		{
			out = new FileOutputStream(outFile);
			PrintWriter writer = new PrintWriter(out);
			
			for (String res : inputResources)
			{
				in = TileRenderer.class.getClassLoader().getResourceAsStream(res);
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				
				String line = null;
				while ((line = reader.readLine()) != null)
				{
					writer.write(line);
					writer.write('\n');
				}
				
				writer.flush();
				
				in.close();
				in = null;
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
				if (in != null)
					in.close();
			}
			catch (Exception e) {}
			try
			{
				if (out != null)
					out.close();
			}
			catch (Exception e) {}
		}
	}
	
	private void outputWorldVectors(File vectorsFile, String varNamePrefix, WorldVectors worldVectors, TileCoordBounds bounds, LevelDat levelDat, final int numChunks, final int numPlayers)
	{
		if (vectorsFile.exists())
			vectorsFile.delete();
		
		System.out.println("Outputting world vectors to "+vectorsFile.getAbsolutePath());
		
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
			
			// Spawn point
			json.writeWorldCoord("spawnPosition", levelDat.getSpawnPosition());
			
			Vector2f origin = new Vector2f();
			origin.x = (worldVectors.origin.x / scale);
			origin.y = (worldVectors.origin.y / scale);
			json.writeMapsPoint("origin", origin);
			
			// Axies
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
		
		System.out.println("World vectors done");
	}
	
	private void outputContents(File outputFile, Configuration config)
	{
		if (outputFile.exists())
			outputFile.delete();
		
		System.out.println("Outputting master contents to "+outputFile.getAbsolutePath());
		
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(outputFile);
			
			writer.println("tileSize = "+config.tileSize()+";");
			writer.println("maxZoom = "+config.numZoomLevels()+";");
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
				writer.println("\t\tsigns: "+m.getId()+"_signData,");
				writer.println("\t\tportals: "+m.getId()+"_portalData,");
				writer.println("\t\tviews: "+m.getId()+"_viewData,");
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
		finally
		{
			if (writer != null)
				writer.close();
		}
	}
	
	public static void outputPlayers(File playersFile, File imagesDir, tectonicus.configuration.Map map, PlayerFilter filter, ArrayList<Player> players, PlayerList ops, PlayerIconAssembler playerIconAssembler)
	{
		if (playersFile.exists())
			playersFile.delete();
		
		FileUtils.ensureExists(imagesDir);
		
		System.out.println("Outputting players to "+playersFile.getAbsolutePath());
		
		int numOutput = 0;
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(playersFile, map.getId()+"_playerData");
			
			for (Player player : players)
			{
				if (filter.passesFilter(player, ops))
				{
					System.out.println("\toutputting "+player.getName());
					
					HashMap<String, String> args = new HashMap<String, String>();
					
					Vector3d pos = player.getPosition();
					args.put("name", "\"" + player.getName() + "\"");
					
					String posStr = "new WorldCoord("+pos.x+", "+pos.y+", "+pos.z+")";
					args.put("worldPos", posStr);
					
					args.put("health", ""+player.getHealth());
					args.put("food", ""+player.getFood());
					args.put("air", ""+player.getAir());
					
					args.put("xpLevel", ""+player.getXpLevel());
					args.put("xpTotal", ""+player.getXpTotal());
					
					jsWriter.write(args);
					
					File iconFile = new File(imagesDir, player.getName()+".png");
					playerIconAssembler.writeIcon(player.getName(), iconFile);
					
					numOutput++;
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
		
		System.out.println("Outputted "+numOutput+" players");
	}
	
	public static void outputBeds(File exportDir, tectonicus.configuration.Map map, PlayerFilter filter, ArrayList<Player> players, PlayerList ops)
	{
		File bedsFile = new File(exportDir, "beds.js");
		if (bedsFile.exists())
			bedsFile.delete();
		
		System.out.println("Outputting beds to "+bedsFile.getAbsolutePath());
		
		int numOutput = 0;
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(bedsFile, map.getId()+"_bedData");
			
			if (map.getDimension() == Dimension.Terra) // Beds only exist in the terra dimension for now
			{
				for (Player player : players)
				{
					if (filter.passesFilter(player, ops) && player.getSpawnPosition() != null)
					{
						System.out.println("\toutputting "+player.getName()+"'s bed");
						
						HashMap<String, String> args = new HashMap<String, String>();
						
						Vector3l spawn = player.getSpawnPosition();
						
						args.put("playerName", "\"" + player.getName() + "\"");
						
						String posStr = "new WorldCoord("+spawn.x+", "+spawn.y+", "+spawn.z+")";
						args.put("worldPos", posStr);
						
						jsWriter.write(args);
						
						numOutput++;
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
		
		System.out.println("Outputted "+numOutput+" beds");
	}
	
	private void outputCompass(File compassFile, tectonicus.configuration.Map map, BlockTypeRegistry registry, TexturePack texturePack)
	{
		try
		{
			ItemRenderer itemRenderer = new ItemRenderer(args, rasteriser);
			
			itemRenderer.renderCompass(map, compassFile);
			itemRenderer.renderPortal(new File(args.outputDir(), "Images/Portal.png"), registry, texturePack);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void outputHtmlResources(TexturePack texturePack, PlayerIconAssembler playerIconAssembler)
	{
		File imagesDir = new File(exportDir, "Images");
		imagesDir.mkdirs();
		
		FileUtils.extractResource("Images/Spawn.png", new File(imagesDir, "Spawn.png"));
		FileUtils.extractResource("Images/Logo.png", new File(imagesDir, "Logo.png"));
		
		FileUtils.extractResource("Images/Spacer.png", new File(imagesDir, "Spacer.png"));
		
		writeImage(texturePack.getItem(10, 2), 32, 32, new File(imagesDir, "Sign.png"));
		writeImage(texturePack.getItem(10, 1), 32, 32, new File(imagesDir, "Picture.png"));
		
		writeImage(texturePack.getItem(7, 1), 32, 32, new File(imagesDir, "IronIcon.png"));
		writeImage(texturePack.getItem(7, 2), 32, 32, new File(imagesDir, "GoldIcon.png"));
		writeImage(texturePack.getItem(7, 3), 32, 32, new File(imagesDir, "DiamondIcon.png"));
		writeImage(texturePack.getItem(13, 2), 32, 32, new File(imagesDir, "Bed.png"));
		
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
		
		// Write default player icon
		playerIconAssembler.writeDefaultIcon(new File(imagesDir, "DefaultPlayer.png"));
		
		// And pull out the jQuery code
		File scriptsDir = new File(exportDir, "Scripts");
		scriptsDir.mkdirs();
		
		FileUtils.extractResource("jquery.js", new File(scriptsDir, "jquery.js"));
		
		FileUtils.extractResource("styles.css", new File(scriptsDir, "styles.css"));
		
		FileUtils.extractResource("math.js", new File(scriptsDir, "math.js"));
		
		ArrayList<String> scriptResources = new ArrayList<String>();
		scriptResources.add("controls.js");
		scriptResources.add("minecraftProjection.js");
		scriptResources.add("main.js");
		outputMergedJs(new File(scriptsDir, "tectonicus.js"), scriptResources);
	}
	
	private static void writeImage(BufferedImage img, final int width, final int height, File file)
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
		// Calculate origin and axies needed for the js to convert from world to map coords
		
		WorldVectors worldVectors = new WorldVectors();
		
		// Vectors for world->map projection
		Vector2f originScreenPos = camera.projectf( new Vector3f(0, 0, 0) );
		Vector2f p100 = camera.projectf( new Vector3f(1, 0, 0) );
		Vector2f p010 = camera.projectf( new Vector3f(0, 1, 0) );
		Vector2f p001 = camera.projectf( new Vector3f(0, 0, 1) );
		
		worldVectors.origin = new Vector2f(originScreenPos.x, originScreenPos.y);
		worldVectors.xAxis = new Vector2f((float)p100.x - (float)originScreenPos.x, (float)p100.y - (float)originScreenPos.y);
		worldVectors.yAxis = new Vector2f((float)p010.x - (float)originScreenPos.x, (float)p010.y - (float)originScreenPos.y);
		worldVectors.zAxis = new Vector2f((float)p001.x - (float)originScreenPos.x, (float)p001.y - (float)originScreenPos.y);
		
		// Vectors for map->world projection
		Vector3f base = camera.unproject(new Vector2f(0, 0));
		Vector3f mapXUnit = camera.unproject(new Vector2f(1, 0));
		Vector3f mapYUnit = camera.unproject(new Vector2f(0, 1));
		
		worldVectors.mapXUnit = new Vector2f(mapXUnit.x - base.x, mapXUnit.z - base.z);
		worldVectors.mapYUnit = new Vector2f(mapYUnit.x - base.x, mapYUnit.z - base.z);
		
		return worldVectors;
	}
	
	private void outputSigns(File outputFile, File signListFile, tectonicus.configuration.Map map)
	{
		HddObjectListReader<Sign> signsIn = null;
		try
		{
			signsIn = new HddObjectListReader<Sign>(signListFile);
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
	
	private void outputSigns(File signFile, HddObjectListReader<Sign> signs, tectonicus.configuration.Map map)
	{
		System.out.println("Writing signs to "+signFile.getAbsolutePath());
		
		if (signFile.exists())
			signFile.delete();
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(signFile, map.getId()+"_signData");
			
			Sign sign = new Sign();
			while (signs.hasNext())
			{				
				signs.read(sign);
				
				String message = "\"" + jsEscape(sign.getText(0)) + "\\n" + jsEscape(sign.getText(1)) + "\\n" + jsEscape(sign.getText(2)) + "\\n" + jsEscape(sign.getText(3)) + "\"";
				
				HashMap<String, String> args = new HashMap<String, String>();
				
				final float worldX = sign.getX() + 0.5f;
				final float worldY = sign.getY();
				final float worldZ = sign.getZ() + 0.5f;				
				
				String posStr = "new WorldCoord("+worldX+", "+worldY+", "+worldZ+")";
				args.put("worldPos", posStr);
				
				args.put("message", message);
				args.put("text1", "\"" + jsEscape(sign.getText(0)) + "\"");
				args.put("text2", "\"" + jsEscape(sign.getText(1)) + "\"");
				args.put("text3", "\"" + jsEscape(sign.getText(2)) + "\"");
				args.put("text4", "\"" + jsEscape(sign.getText(3)) + "\"");
				
				jsWriter.write(args);
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
	
	private void outputPortals(File outFile, File portalListFile, tectonicus.configuration.Map map)
	{
		try
		{
			HddObjectListReader<Portal> portalsIn = new HddObjectListReader<Portal>(portalListFile);
			outputPortals(outFile, portalsIn, map);
			portalsIn.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void outputPortals(File portalFile, HddObjectListReader<Portal> portalPositions, tectonicus.configuration.Map map)
	{
		System.out.println("Writing portals...");
		
		if (portalFile.exists())
			portalFile.delete();
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(portalFile, map.getId()+"_portalData");
			
			Portal portal = new Portal();
			while (portalPositions.hasNext())
			{				
				portalPositions.read(portal);
				
				HashMap<String, String> args = new HashMap<String, String>();
				
				final float worldX = portal.getX() + 0.5f;
				final float worldY = portal.getY();
				final float worldZ = portal.getZ() + 0.5f;				
				
				String posStr = "new WorldCoord("+worldX+", "+worldY+", "+worldZ+")";
				args.put("worldPos", posStr);
				
				jsWriter.write(args);
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
	
	private void outputViews(File outputFile, File viewsListFile, tectonicus.configuration.Map map, ImageFormat imageFormat)
	{
		HddObjectListReader<Sign> viewsIn = null;
		try
		{
			viewsIn = new HddObjectListReader<Sign>(viewsListFile);
			outputViews(outputFile, viewsIn, map, imageFormat);
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
	
	private void outputViews(File viewsFile, HddObjectListReader<Sign> views, tectonicus.configuration.Map map, ImageFormat imageFormat)
	{
		System.out.println("Writing views...");
		
		if (viewsFile.exists())
			viewsFile.delete();
		
		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(viewsFile, map.getId()+"_viewData");
			
			Sign sign = new Sign();
			while (views.hasNext())
			{				
				views.read(sign);
				
				HashMap<String, String> args = new HashMap<String, String>();
				
				final float worldX = sign.getX() + 0.5f;
				final float worldY = sign.getY();
				final float worldZ = sign.getZ() + 0.5f;				
				
				String posStr = "new WorldCoord("+worldX+", "+worldY+", "+worldZ+")";
				args.put("worldPos", posStr);
				
				String text = sign.getText(1) + " " + sign.getText(2) + " " + sign.getText(3);
				text = text.trim();
				args.put("text", "\'" + jsEscape(text) + "\'");
				
				String filename = map.getId()+"/Views/View_"+sign.getX()+"_"+sign.getY()+"_"+sign.getZ()+"."+imageFormat.getExtension();
				args.put("imageFile", "\'" + filename + "\'");
				
				jsWriter.write(args);
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
	
	private void outputChangedFile()
	{
		System.out.println("Writing changed file list...");
		progressListener.onTaskStarted(Task.OutputChangedList.toString());
		
		changedFileList.close();
		changedFileList = null;
	}
	
	public TileCoord screenToTile(Point screenPos)
	{
		final float tileX = (float)screenPos.x / (float)tileWidth;
		final float tileY = (float)screenPos.y / (float)tileHeight;
		
		final int wholeTileX = (int)Math.floor(tileX);
		final int wholeTileY = (int)Math.floor(tileY);
		
		return new TileCoord(wholeTileX, wholeTileY);
	}
	
	public static Point tileToScreen(TileCoord tilePos, final int tileWidth, final int tileHeight)
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
	
	private static boolean passesFilter(RawSign s, SignFilter filter)
	{
		// Empty signs (those with no text) are used for asthetic reasons, like building chairs
		// Always skip these
		if (s.text1.trim().isEmpty() && s.text2.trim().isEmpty() && s.text3.trim().isEmpty() && s.text4.trim().isEmpty())
			return false;
		
		// Always skip view signs
		if (s.text1.startsWith("#view"))
			return false;
		
		if (filter == SignFilter.None)
		{
			return false;
		}
		else if (filter == SignFilter.All)
		{
			return true;
		}
		else if (filter == SignFilter.Special)
		{
			String line = "" + s.text1 + s.text2 + s.text3 + s.text4;
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
		Vector2f xAxis, yAxis, zAxis;
		Vector2f mapXUnit, mapYUnit;
		
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
