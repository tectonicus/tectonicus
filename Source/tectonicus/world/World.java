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
package tectonicus.world;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockMaskFactory;
import tectonicus.BlockRegistryParser;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.ChunkCoord;
import tectonicus.ChunkLocator;
import tectonicus.Minecraft;
import tectonicus.NullBlockFilter;
import tectonicus.NullBlockMaskFactory;
import tectonicus.RegionCache;
import tectonicus.RegionCoord;
import tectonicus.Util;
import tectonicus.blockTypes.Air;
import tectonicus.cache.BiomeCache;
import tectonicus.configuration.Configuration.Dimension;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.BiomeIds;
import tectonicus.raw.LevelDat;
import tectonicus.raw.PlayerList;
import tectonicus.raw.Player;
import tectonicus.raw.RawChunk;
import tectonicus.raw.RawSign;
import tectonicus.renderer.Camera;
import tectonicus.renderer.Geometry;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.Colour4f;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.BlockFilter;
import tectonicus.world.filter.CompositeBlockFilter;
import tectonicus.world.subset.RegionIterator;
import tectonicus.world.subset.WorldSubset;
import tectonicus.world.subset.WorldSubsetFactory;

public class World implements BlockContext
{
	private static final int BATCH_SIZE = 100;
	
	private final Rasteriser rasteriser;
	
	private final File worldDir;
	private final File dimensionDir;
	
	private BlockTypeRegistry registry;
	
	private LevelDat levelDat;
	
	private ArrayList<Player> players;
	private PlayerList ops;
	
	private TexturePack texturePack;
	
	private RegionCache regionCache;
	private ChunkLocator chunkLocator;
	
	private RawCache rawLoadedChunks;
	private GeometryCache geometryLoadedChunks;
	
	private LightStyle lightStyle;
	
	private int defaultBlockId;
	
	private BlockFilter blockFilter;
	private BlockMaskFactory blockMaskFactory;
	
	private final BiomeCache biomeCache;
	
	private WorldSubset worldSubset;
	
	private Geometry daySkybox, nightSkybox;
	
	public World(Rasteriser rasteriser, File baseDir, Dimension dimension, File minecraftJar, File texturePackFile, BiomeCache biomeCache, MessageDigest hashAlgorithm, String singlePlayerName, WorldSubsetFactory subsetFactory)
	{
		this.rasteriser = rasteriser;
		
		this.defaultBlockId = BlockIds.AIR;
		this.blockFilter = new NullBlockFilter();
		this.blockMaskFactory = new NullBlockMaskFactory();
		
		this.worldDir = baseDir;
		
		// Use the world dir and the dimension to find the dimension dir
		if (dimension == Dimension.Terra)
		{
			dimensionDir = worldDir;
		}
		else if (dimension == Dimension.Nether)
		{
			dimensionDir = new File(worldDir, "DIM-1");
		}
		else if (dimension == Dimension.Ender)
		{
			dimensionDir = new File(worldDir, "DIM1");
		}
		else
		{
			dimensionDir = worldDir;
		}
		
		this.biomeCache = biomeCache;
		
		// Check that this looks like a world dir
		if (!Minecraft.isValidWorldDir(baseDir))
			throw new RuntimeException("Invalid world dir! No level.dat found at "+Minecraft.findLevelDat(baseDir).getAbsolutePath());
		
		// TODO: Better error handling here.
		// World should throw Exception?
		try
		{
			System.out.println("Loading level.dat");
			levelDat = new LevelDat(Minecraft.findLevelDat(baseDir), singlePlayerName);
			
			if (levelDat.getVersion() == LevelDat.UNKNOWN_VERSION)
			{
				throw new RuntimeException("Error: Alpha map format no longer supported");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		System.out.println("Loading textures");
		texturePack = new TexturePack(rasteriser, minecraftJar, texturePackFile);
		
		System.out.println("Creating block registry");
		loadBlockRegistry(null, true);
		
		System.out.println("Loading players");
		players = loadPlayers(worldDir);
		
		Player singlePlayer = levelDat.getSinglePlayer();
		if (singlePlayer != null)
		{
			players.add(singlePlayer);
		}
		
		ops = PlayerList.loadOps(worldDir);
		
		regionCache = new RegionCache(dimensionDir);
		chunkLocator = new ChunkLocator(dimensionDir, biomeCache, regionCache);
		
		rawLoadedChunks = new RawCache(100);
		geometryLoadedChunks = new GeometryCache(100);
	
		this.worldSubset = subsetFactory.create(this);
		
		this.lightStyle = LightStyle.None;
		
		this.daySkybox = SkyboxUtil.generateDaySkybox(rasteriser);
		this.nightSkybox = SkyboxUtil.generateNightSkybox(rasteriser);
	}
	
	public void loadBlockRegistry(String customConfigPath, final boolean useDefaultBlocks)
	{
		registry = new BlockTypeRegistry();
		registry.setDefaultBlock(new Air());
		
		BlockRegistryParser parser = new BlockRegistryParser(texturePack, biomeCache);
		
		if (useDefaultBlocks)
			parser.parse("defaultBlockConfig.xml", registry);
		
		if (customConfigPath != null && customConfigPath.length() > 0)
			parser.parse(customConfigPath, registry);
		
		flushChunkCache();
		flushGeometryCache();
	}
	
	public BiomeCache getBiomeCache()
	{
		return biomeCache;
	}
	
	public void setLightStyle(LightStyle style)
	{
		// Clear the geometry cache if style has changed
		if (this.lightStyle != style)
		{
			flushChunkCache();
		}
		
		this.lightStyle = style;
	}
	
	@Override
	public LightStyle getLightStyle()
	{
		return lightStyle;
	}
	
	public void setDefaultBlockId(final int blockId)
	{
		// Clear the geometry cache if id has changed
		if (this.defaultBlockId != blockId)
		{
			flushChunkCache();
		}
		
		this.defaultBlockId = blockId;
	}
	
	public void setBlockFilter(BlockFilter filter)
	{
		if (filter == null)
			throw new NullPointerException();
		
		flushChunkCache();
		
		this.blockFilter = filter;
	}
	
	public BlockFilter getBlockFilter()
	{
		return blockFilter;
	}
	
	public void setBlockMaskFactory(BlockMaskFactory factory)
	{
		if (factory == null)
			throw new NullPointerException();
		
		flushChunkCache();
		
		this.blockMaskFactory = factory;
	}
	
	public RegionIterator createRegionIterator()
	{
		return worldSubset.createRegionIterator();
	}
	
	public boolean contains(ChunkCoord coord)
	{
		return worldSubset.contains(coord);
	}
	
	/** Gets the players for a particular dimension, or null for all players */
	public ArrayList<Player> players(Dimension dimension)
	{
		ArrayList<Player> result = new ArrayList<Player>();
		for (Player p : players)
		{
			if (dimension == null || p.getDimension() == dimension)
				result.add(p);
		}
		return result;
	}
	
	public int numPlayers()
	{
		return players.size();
	}
	
	public PlayerList getOps()
	{
		return ops;
	}
	
	public LevelDat getLevelDat()
	{
		return levelDat;
	}
	
	public TexturePack getTexturePack()
	{
		return texturePack;
	}
	
	/* New, optimised version.
	 * Project the frustum vertices down onto the ground plane, and find the bounding box.
	 * Then iterate over each region that touches this box and check against the frustum
	 * For regions which touch the frustum, then check all contained chunks.
	 * This gives us a quad-tree-esque lookup method without having to actually store a heavy quad tree in memory.
	 */
	public ArrayList<ChunkCoord> findVisible(Camera camera)
	{
		ArrayList<ChunkCoord> result = new ArrayList<ChunkCoord>();
		
		// Cast verts down onto landscape
		long minX = Long.MAX_VALUE;
		long minZ = Long.MAX_VALUE;
		long maxX = Long.MIN_VALUE;
		long maxZ = Long.MIN_VALUE;
		for (Vector3f corner : camera.getFrustumVertices())
		{
			RegionCoord coord = RegionCoord.fromWorldCoord(corner.x, corner.z);
			minX = Math.min(minX, coord.x);
			minZ = Math.min(minZ, coord.z);
			maxX = Math.max(maxX, coord.x);
			maxZ = Math.max(maxZ, coord.z);
		}
		
		// First iterate over the touched regions and find which ones touch the camera
		for (long regionX=minX; regionX<=maxX; regionX++)
		{
			for (long regionZ=minZ; regionZ<=maxZ; regionZ++)
			{
				BoundingBox regionBounds = new BoundingBox(new Vector3l(regionX*RegionCoord.REGION_WIDTH*RawChunk.WIDTH, 0, regionZ*RegionCoord.REGION_HEIGHT*RawChunk.DEPTH),
																RawChunk.WIDTH*RegionCoord.REGION_WIDTH, RawChunk.HEIGHT,
																RawChunk.DEPTH*RegionCoord.REGION_HEIGHT);
				if (regionBounds.isVisible(camera))
				{
					
					// Now iterate over all chunks within the region
					for (long chunkX=0; chunkX<RegionCoord.REGION_WIDTH; chunkX++)
					{
						for (long chunkZ=0; chunkZ<RegionCoord.REGION_HEIGHT; chunkZ++)
						{
							ChunkCoord chunkCoord = new ChunkCoord(	regionX*RegionCoord.REGION_WIDTH + chunkX,
																	regionZ*RegionCoord.REGION_HEIGHT + chunkZ);
							
							if (worldSubset.contains(chunkCoord))
							{
								BoundingBox chunkBounds = new BoundingBox(new Vector3l(chunkCoord.x*RawChunk.WIDTH, 0, chunkCoord.z*RawChunk.DEPTH), RawChunk.WIDTH, RawChunk.HEIGHT, RawChunk.DEPTH);
								if (chunkBounds.isVisible(camera))
								{
									if (chunkLocator.exists(chunkCoord))
									{
										result.add(chunkCoord);
									}
								}
							}
						}
					}
					
				}
			}
		}
		
		return result;
	}
	
	/*
	private void unloadAll()
	{
		for (Chunk c : geometryLoadedChunks.values())
		{
			c.unloadGeometry();
		}
		
		for (Chunk c : rawLoadedChunks.values())
		{
			c.unloadRaw();
		}
		
		geometryLoadedChunks.clear();
		rawLoadedChunks.clear();
	}
	*/
	
	public void draw(Camera camera, final boolean showSky, final boolean genAlphaMask)
	{
		// Find visible chunks
		ArrayList<ChunkCoord> visible = findVisible(camera);
			
		// Sort back to front
		Collections.sort(visible, new BackToFrontSorter(camera));
		
		// Clear the alpha buffer
		if (genAlphaMask)
		{
			rasteriser.enableColourWriting(false, true);
			rasteriser.enableDepthTest(false);
			rasteriser.enableBlending(false);
			
			rasteriser.beginShape(PrimativeType.Quads);
			{
				if (genAlphaMask)
					rasteriser.colour(0, 0, 0, 0);
				else
					rasteriser.colour(0, 0, 0, 1);
				
				Vector3f[] quad = camera.getClearQuad();
				
				Vector3f offset = new Vector3f(camera.getForward());
				offset.scale(+0.20f); // Ugh. Arbitrary fudge factor because camera.getClearQuad doesn't seem to quite work for perspective camera 
				for (Vector3f v : quad)
				{
					v.x += offset.x;
					v.y += offset.y;
					v.z += offset.z;
				}
				
				rasteriser.vertex(quad[0].x, quad[0].y, quad[0].z);
				rasteriser.vertex(quad[1].x, quad[1].y, quad[1].z);
				rasteriser.vertex(quad[2].x, quad[2].y, quad[2].z);
				rasteriser.vertex(quad[3].x, quad[3].y, quad[3].z);			
			}
			rasteriser.endShape();
			
			rasteriser.enableDepthTest(true);
			rasteriser.enableColourWriting(true, false);
		}
		
		// Skybox first
		if (showSky)
		{
			Geometry skybox = lightStyle == LightStyle.Night ? nightSkybox : daySkybox;
			
			skybox.drawSolidSurfaces(camera.getEyePosition().x, camera.getEyePosition().y, camera.getEyePosition().z);
			rasteriser.clearDepthBuffer();
		}
		
		// Now the chunks in batches
		ArrayList<ChunkCoord> toProcess = new ArrayList<ChunkCoord>(visible);
		while (!toProcess.isEmpty())
		{
			ArrayList<ChunkCoord> batch = new ArrayList<ChunkCoord>();
			
			final int nextBatchSize = Math.min(BATCH_SIZE, toProcess.size());
			for (int i=0; i<nextBatchSize; i++)
			{
				batch.add( toProcess.remove(0) );
			}
			
			draw(camera, batch, genAlphaMask);
		}
	}
	
	private void draw(Camera camera, ArrayList<ChunkCoord> visible, final boolean genAlphaMask)
	{
		// Find adjacent chunks (need adjacent chunks loaded for proper geometry gen)
		Set<ChunkCoord> adjacent = new HashSet<ChunkCoord>();
		adjacent.addAll(visible);
		for (ChunkCoord base : visible)
		{
			adjacent.add( new ChunkCoord(base.x+1, base.z) );
			adjacent.add( new ChunkCoord(base.x-1, base.z) );
			adjacent.add( new ChunkCoord(base.x, base.z+1) );
			adjacent.add( new ChunkCoord(base.x, base.z-1) );
		}
		adjacent.remove(null);
		
		for (ChunkCoord coord : adjacent)
		{
			// Load raw if not present
			if (!rawLoadedChunks.contains(coord))
			{
				if (worldSubset.contains(coord))
				{
					CompositeBlockFilter composite = new CompositeBlockFilter();
					composite.add(blockFilter);
					composite.add(worldSubset.getBlockFilter(coord));
					
					// Not loaded, so load it now
					Chunk c = chunkLocator.loadChunk(coord, composite);
					if (c != null)
					{	
						rawLoadedChunks.put(coord, c);
					}
				}
			}
			else
			{
				rawLoadedChunks.touch(coord);
			}
		}
		
		for (ChunkCoord coord : visible)
		{
			// Create geometry if not present
			if (!geometryLoadedChunks.contains(coord))
			{
				Chunk c = rawLoadedChunks.get(coord);
				if (c != null)
				{	
					// Actually create the geometry
					
					final boolean ok = c.createGeometry(rasteriser, this, registry, blockMaskFactory, texturePack);
					assert ok;
					
					geometryLoadedChunks.put(coord, c);
				}
			}
			else
			{
				geometryLoadedChunks.touch(coord);
			}
		}
		
		// Now actually find all visible chunks we've managed to load
		ArrayList<Chunk> visibleChunks = new ArrayList<Chunk>();
		for (ChunkCoord coord : visible)
		{
			Chunk c = geometryLoadedChunks.get(coord);
			if (c != null)
				visibleChunks.add(c);
		}
		
		rasteriser.enableDepthTest(true);
		rasteriser.setBlendFunc(BlendFunc.Regular);
		
		
		drawGeometry(camera, visibleChunks);
		
		// Render the alpha mask
		if (genAlphaMask)
		{	
			rasteriser.enableBlending(true);
			rasteriser.enableDepthTest(true);
			
			rasteriser.enableColourWriting(false, true);
			
			// Write the new alpha values
			{
				rasteriser.setBlendFunc(BlendFunc.Additive);
				
				drawGeometry(camera, visibleChunks);
				
				rasteriser.setBlendFunc(BlendFunc.Regular);
			}
			
			rasteriser.enableColourWriting(true, false);
		}
		
		rawLoadedChunks.trimToMaxSize();
		geometryLoadedChunks.trimToMaxSize();
	}
	
	private void drawGeometry(Camera camera, ArrayList<Chunk> visible)
	{
		rasteriser.enableDepthWriting(true);
		
		// Solid pass
		
		rasteriser.enableBlending(false);
		rasteriser.enableAlphaTest(false);
		
		for (Chunk c : visible)
		{
			c.drawSolid(camera);
		}
		
		// Alpha test pass
		
		rasteriser.enableAlphaTest(true);
		rasteriser.setAlphaFunc(AlphaFunc.Greater, 0.6f); // TODO: Figure out what this value should actually be
		rasteriser.enableBlending(false);
		
		for (Chunk c : visible)
		{	
			c.drawAlphaTestedSurfaces(camera);
		}
		
		// Transparency pass
		
		rasteriser.enableDepthWriting(false);
		rasteriser.enableBlending(true);
		rasteriser.enableAlphaTest(false);
		
		for (Chunk c : visible)
		{	
			c.drawTransparentSurfaces(camera);
		}
		
		rasteriser.enableDepthWriting(true);
	}
	
	public static ChunkCoord getTileCoord(File datFile)
	{
		try
		{
			// "c.0.0.dat"
			String name = datFile.getName();
			
			final int datPos = name.indexOf(".dat");
			if (datPos != -1)
			{
				name = name.substring(0, datPos);
				final int dotPos = name.indexOf('.');
				if (dotPos != -1 && dotPos < name.length()-1)
				{
					name = name.substring(dotPos+1, name.length());
					
					long first = Util.fromBase36( name.substring(0, name.indexOf('.')));
					long second = Util.fromBase36( name.substring(name.indexOf('.')+1, name.length()));
					
					return new ChunkCoord(first, second);
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Couldn't get tile coord from "+datFile.getAbsolutePath()+" ("+e+")");
			System.err.println("File will be ignored");
		}
		
		return null;
	}
	
	
	private Location resolve(ChunkCoord chunkCoord, int x, int y, int z)
	{
		long chunkX = chunkCoord.x;
		long chunkZ = chunkCoord.z;
		
		while (x < 0)
		{
			x += RawChunk.WIDTH;
			chunkX--;
		}
		while (x >= RawChunk.WIDTH)
		{
			x -= RawChunk.WIDTH;
			chunkX++;
		}
		
		while (z < 0)
		{
			z += RawChunk.DEPTH;
			chunkZ--;
		}
		while (z >= RawChunk.DEPTH)
		{
			z -= RawChunk.DEPTH;
			chunkZ++;
		}
		
		return new Location(new ChunkCoord(chunkX, chunkZ), x, y, z);
	}
	
	public Vector3l getSpawnPosition()
	{
		return levelDat.getSpawnPosition();
	}
	
	@Override
	public int getBlockId(ChunkCoord chunkCoord, int x, int y, int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT)
			return defaultBlockId;
		
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		if (c == null)
		{
			return defaultBlockId;
		}
		else
			return c.getBlockId(loc.x, loc.y, loc.z, defaultBlockId);
	}
	
	@Override
	public BlockType getBlockType(ChunkCoord chunkCoord, int x, int y, int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT)
			return registry.find(defaultBlockId, 0);
		
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		if (c == null)
		{
			return registry.find(defaultBlockId, 0);
		}
		else
		{
			final int id = c.getBlockId(loc.x, loc.y, loc.z, defaultBlockId);
			final int data = c.getRawChunk().getBlockData(loc.x, loc.y, loc.z);
			return registry.find(id, data);
		}
	}
	
	@Override
	public int getBiomeId(ChunkCoord chunkCoord, int x, int y, int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT)
			return BiomeIds.UNKNOWN;
		
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		if (c == null)
		{
			return BiomeIds.UNKNOWN;
		}
		else
			return c.getBiomeId(loc.x, loc.y, loc.z);
	}
	
	@Override
	public float getLight(ChunkCoord chunkCoord, final int x, final int y, final int z, LightFace face)
	{
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		RawChunk raw = c != null ? c.getRawChunk() : null;
		
		return Chunk.getLight(lightStyle, face, raw, loc.x, loc.y, loc.z);
	}

	/*
	private float getSkyLight(ChunkCoord chunkCoord, final int x, final int y, final int z)
	{
		if (y < 0)
			return 0;
		if (y >= RawChunk.HEIGHT)
			return 1.0f;
		
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = chunks.get(loc.coord);
		if (c == null)
			return 1.0f;
		else
			return c.getSkyLight(loc.x, loc.y, loc.z) / (float)RawChunk.MAX_LIGHT;
	}
	
	private float getBlockLight(ChunkCoord chunkCoord, final int x, final int y, final int z)
	{
		if (y < 0)
			return 0;
		if (y >= RawChunk.HEIGHT)
			return 1.0f;
		
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = chunks.get(loc.coord);
		if (c == null)
			return 1.0f;
		else
			return c.getBlockLight(loc.x, loc.y, loc.z) / (float)RawChunk.MAX_LIGHT;
	}
	*/
	
	public void flushChunkCache()
	{
		if (rawLoadedChunks != null)
		{
			// Flush geometry
			geometryLoadedChunks.unloadAll();
			
			// Flush raw loaded chunks
			rawLoadedChunks.unloadAll();
		}
	}
	
	public void flushGeometryCache()
	{
		if (geometryLoadedChunks != null)
		{
			geometryLoadedChunks.unloadAll();
		}
	}
	
	public void dumpMemStats()
	{
		System.out.println("---------------");
		System.out.println("World Mem Stats");
		System.out.println("---------------");
		
	//	System.out.println("Total chunks: "+chunks.size());
		System.out.println("Loaded raw chunks: "+rawLoadedChunks.size());
		System.out.println("Loaded geometry chunks: "+geometryLoadedChunks.size());
		
		long rawMemTotal = rawLoadedChunks.getRawMemorySize();
		rawMemTotal = rawMemTotal / 1024 / 1024; // bytes to megabytes
		System.out.println("Estimated raw memory: "+rawMemTotal+" Mb");
		
		long geometryMemTotal = geometryLoadedChunks.getGeometryMemorySize();
		geometryMemTotal = geometryMemTotal / 1024 /  1024; // bytes to megabytes
		System.out.println("Estimated geometry memory: "+geometryMemTotal+" Mb");
		
		System.out.println();
		
		Runtime runtime = Runtime.getRuntime();

		
		final long maxMemory = runtime.maxMemory();
		final long allocatedMemory = runtime.totalMemory();
		final long freeMemory = runtime.freeMemory();
    
		NumberFormat format = NumberFormat.getInstance();
		System.out.println("Max memory: "+format.format(maxMemory/1024.0f)+"Mb");
		System.out.println("Allocated memory: "+format.format(allocatedMemory/1024.0f)+"Mb");
		System.out.println("Free memory: "+format.format(freeMemory/1024.0f)+"Mb");
		
		/*
		System.out.println("Geometry stats:");
		for (Chunk c : geometryLoadedChunks.values())
		{
			c.printGeometryStats();
		}
		*/
	}
	
	private static class BackToFrontSorter implements Comparator<ChunkCoord>
	{
		private Camera camera;
		
		public BackToFrontSorter(Camera camera)
		{
			this.camera = camera;
		}
		
		@Override
		public int compare(ChunkCoord lhs, ChunkCoord rhs)
		{
			
			final float lhsDist = Math.abs( getDistance(camera, lhs) );
			final float rhsDist = Math.abs( getDistance(camera, rhs) );
			
			final float scale = 1000000000;
			
		//	return Math.round(lhsDist * scale - rhsDist * scale); // correct
			return Math.round(rhsDist * scale - lhsDist * scale); // correct
			
			/*
			// Hacky and works, but won't work for perspective views
			Point lhsScreen = camera.project(lhs.getBounds().getCenter());
			Point rhsScreen = camera.project(rhs.getBounds().getCenter());
			
			return lhsScreen.y - rhsScreen.y;
			*/
		}
		private static float getDistance(Camera camera, ChunkCoord coord)
		{
			final float worldX = coord.x * RawChunk.WIDTH + (RawChunk.WIDTH / 2.0f);
			final float worldY = RawChunk.HEIGHT / 2.0f;
			final float worldZ = coord.z * RawChunk.DEPTH + (RawChunk.DEPTH / 2.0f);
			final float centerDist = Chunk.getDistance(camera, worldX, worldY, worldZ);
			return centerDist;
		}
	}
	
	private static class Location
	{
		public final ChunkCoord coord;
		public final int x, y, z;
		
		public Location(ChunkCoord coord, final int x, final int y, final int z)
		{
			this.coord = coord;
			
			this.x = x;
			this.y = y;
			this.z =z;
		}
	}
	
	
	public static ArrayList<Player> loadPlayers(File worldDir)
	{
		File playersDir = Minecraft.findPlayesDir(worldDir);
		
		System.out.println("Loading players from "+playersDir.getAbsolutePath());
		
		ArrayList<Player> players = new ArrayList<Player>();
		File[] playerFiles = playersDir.listFiles();
		if (playerFiles != null)
		{
			for (File playerFile : playerFiles)
			{
				if (playerFile.getName().endsWith(".dat"))
				{
					try
					{
						Player player = new Player(playerFile);
						players.add(player);
					}
					catch (Exception e)
					{
						System.err.println("Couldn't load player info from "+playerFile.getName());
						e.printStackTrace();
					}
				}
			}
		}
		
		System.out.println("\tloaded "+players.size()+" players");
		
		return players;
	}
	
	public BlockTypeRegistry getBlockTypeRegistry()
	{
		return registry;
	}
	
	public File getWorldDir()
	{
		return worldDir;
	}

	public File getDimensionDir()
	{
		return dimensionDir;
	}
	
	public RawSign[] getLoadedSigns()
	{
		ArrayList<RawSign> result = new ArrayList<RawSign>();
		
		for (Chunk c : rawLoadedChunks.values())
		{
			result.addAll( c.getSigns() );
		}
		
		return result.toArray(new RawSign[0]);
	}
	
	@Override
	public Colour4f getGrassColour(ChunkCoord chunkCoord, int x, int y, int z)
	{
		final int biomeId = getBiomeId(chunkCoord, x, y, z);
		final int northId = getBiomeId(chunkCoord, x, y, z-1);
		final int southId = getBiomeId(chunkCoord, x, y, z+1);
		final int eastId = getBiomeId(chunkCoord, x+1, y, z);
		final int westId = getBiomeId(chunkCoord, x-1, y, z);
		
		Colour4f centerColour = getGrassColour(biomeId);
		Colour4f northColour = getGrassColour(northId);
		Colour4f southColour = getGrassColour(southId);
		Colour4f eastColour = getGrassColour(eastId);
		Colour4f westColour = getGrassColour(westId);
		
		Colour4f colour = new Colour4f(centerColour);
		colour.add(northColour);
		colour.add(southColour);
		colour.add(eastColour);
		colour.add(westColour);
		colour.divide(5);
		
		return colour;
	}
	
	private Colour4f getGrassColour(final int id)
	{
		Point colourCoord = BiomeIds.getColourCoord(id);
		Color awtColour = texturePack.getGrassColour(colourCoord.x, colourCoord.y);
		Colour4f colour = new Colour4f(awtColour);
		return colour;
	}
}
