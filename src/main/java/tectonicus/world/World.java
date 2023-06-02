/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.Getter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;
import tectonicus.Block;
import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockMaskFactory;
import tectonicus.BlockRegistryParser;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Minecraft;
import tectonicus.NullBlockMaskFactory;
import tectonicus.RegionCache;
import tectonicus.RegionCoord;
import tectonicus.Version;
import tectonicus.blockTypes.Air;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.cache.PlayerSkinCache.CacheEntry;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkLocator;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.configuration.SignFilter;
import tectonicus.exceptions.IncompatibleVersionException;
import tectonicus.rasteriser.AlphaFunc;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.PrimativeType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.Biome;
import tectonicus.raw.BiomeIds;
import tectonicus.raw.BiomeUtils;
import tectonicus.raw.Biomes;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.LevelDat;
import tectonicus.raw.Player;
import tectonicus.raw.Player.RequestPlayerInfoTask;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.renderer.Camera;
import tectonicus.renderer.Geometry;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.Colour4f;
import tectonicus.util.DirUtils;
import tectonicus.util.Util;
import tectonicus.util.Vector3d;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.BlockFilter;
import tectonicus.world.filter.CompositeBlockFilter;
import tectonicus.world.filter.NullBlockFilter;
import tectonicus.world.subset.CircularWorldSubset;
import tectonicus.world.subset.RegionIterator;
import tectonicus.world.subset.WorldSubset;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static tectonicus.Version.VERSIONS_6_TO_8;
import static tectonicus.Version.VERSIONS_9_TO_11;
import static tectonicus.Version.VERSION_12;
import static tectonicus.Version.VERSION_13;
import static tectonicus.Version.VERSION_14;
import static tectonicus.Version.VERSION_15;
import static tectonicus.Version.VERSION_18;
import static tectonicus.Version.VERSION_4;
import static tectonicus.Version.VERSION_5;
import static tectonicus.Version.VERSION_UNKNOWN;

@Log4j2
public class World implements BlockContext
{
	private static final int BATCH_SIZE = 128;
	
	private final Version textureVersion;
	
	private final Rasteriser rasteriser;
	
	private final File worldDir;
	private final File dimensionDir;
	@Getter
	private final Dimension dimension;
	
	private BlockTypeRegistry registry;
	@Getter
	private BlockRegistry modelRegistry;
	
	private final LevelDat levelDat;

	@Getter
	private final WorldInfo worldInfo;
	
	private final List<Player> players;
	private final PlayerSkinCache playerSkinCache;
	
	private final ConcurrentLinkedQueue<ContainerEntity> chests;
	
	private final TexturePack texturePack;
	
	private final RegionCache regionCache;
	private final ChunkLocator chunkLocator;

	private final RawCache rawLoadedChunks;
	private final GeometryCache geometryLoadedChunks;
	
	private LightStyle lightStyle;
	@Getter
	private final boolean smoothLit;
	@Getter
	private final float nightLightAdjustment;

	private int defaultBlockId;
	private String defaultBlockName = Block.AIR.getName();
	
	private BlockFilter blockFilter;
	private BlockMaskFactory blockMaskFactory;
	
	private final BiomeCache biomeCache;

	@Getter
	private final WorldSubset worldSubset;
	
	private final Geometry daySkybox, nightSkybox;
	
	private final SignFilter signFilter;

	private final Map<Location, String> unknownBlocks;
	
	public World(Rasteriser rasteriser, tectonicus.configuration.Map map, BiomeCache biomeCache, PlayerSkinCache playerSkinCache, Configuration config)
	{
		this.rasteriser = rasteriser;
		this.signFilter = map.getSignFilter();
		
		this.defaultBlockId = BlockIds.AIR;
		this.blockFilter = new NullBlockFilter();

		this.worldDir = map.getWorldDir();
		this.dimension = map.getDimension();

		// Use the world dir and the dimension to find the dimension dir
		dimensionDir = DirUtils.getDimensionDir(worldDir, dimension);
		
		log.info("Loading world from base dir {} with dimension {}", worldDir.getPath(), dimension);
		log.debug("\tFull dimension dir: {}", dimensionDir.getAbsolutePath());
		
		this.biomeCache = biomeCache;
		this.playerSkinCache = playerSkinCache;
		
		// Check that this looks like a world dir
		if (!Minecraft.isValidWorldDir(worldDir.toPath()))
			throw new RuntimeException("Invalid world dir! No level.dat found at "+Minecraft.findLevelDat(worldDir.toPath()));
		if (!Minecraft.isValidDimensionDir(dimensionDir))
			throw new RuntimeException("Invalid dimension dir! No /region/*.mcr or /region/*.mca found in "+dimensionDir.getAbsolutePath());
		
		// TODO: Better error handling here.
		// World should throw Exception?
		try {
			log.info("Loading level.dat");
			levelDat = new LevelDat(Minecraft.findLevelDat(worldDir.toPath()), config.getSinglePlayerName());
			if (dimension == Dimension.END) {
				levelDat.setSpawnPosition(100, 49, 0);  // Location of obsidian platform where the player spawns
			}
			
			if (levelDat.isAlpha()) {
				throw new RuntimeException("Error: Alpha map format no longer supported");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Version version = VERSION_UNKNOWN;
		boolean sectionArrayOffset = false;
		String worldVersion = levelDat.getVersion();
		if (worldVersion != null) {
			String versionNumber = worldVersion.contains(".") ? worldVersion.split("\\.")[1] : "";
			if (StringUtils.isNotEmpty(versionNumber)) {
                                versionNumber = versionNumber.split(" Release Candidate ")[0];
				Minecraft.setWorldVersion(Integer.parseInt(versionNumber));
				version = Version.byName(worldVersion.substring(0, worldVersion.lastIndexOf(".")));
			}

			if (version.getNumVersion() < VERSION_18.getNumVersion() || dimension == Dimension.NETHER || dimension == Dimension.END) {
				Minecraft.setChunkHeight(256);
			} else {
				Minecraft.setChunkHeight(384);
				sectionArrayOffset = true;
			}
			log.info("Current world max chunk height: {}", Minecraft.getChunkHeight());
		}

		worldInfo = new WorldInfo(version, sectionArrayOffset);

		this.blockMaskFactory = new NullBlockMaskFactory();

		log.info("Loading textures");
		texturePack = new TexturePack(rasteriser, config.minecraftJar(), config.getTexturePack(), map.getModJars(), config);
		this.textureVersion = texturePack.getVersion();


		log.info("World version: " + worldVersion);
		// Is this actually helpful?
		if (worldVersion != null && textureVersion.getNumVersion() >= VERSIONS_9_TO_11.getNumVersion()) {
			switch (worldVersion.contains(".") ? worldVersion.split("\\.")[1] : "") {
				case "9":
				case "10":
				case "11":
					if (!textureVersion.equals(VERSIONS_9_TO_11))
						throw new IncompatibleVersionException(textureVersion, worldVersion);
					break;
				case "12":
					if (!textureVersion.equals(VERSION_12))
						throw new IncompatibleVersionException(textureVersion, worldVersion);
					break;
				case "13":
					if (!textureVersion.equals(VERSION_13))
						throw new IncompatibleVersionException(textureVersion, worldVersion);
					break;
				default:
			}
		}

		players = loadPlayers(worldDir, playerSkinCache);
		
		chests = new ConcurrentLinkedQueue<ContainerEntity>();
		
		regionCache = new RegionCache(dimensionDir);
		chunkLocator = new ChunkLocator(biomeCache, regionCache);
		
		//Set subset origin if none was set in config file
		WorldSubset subset = map.getWorldSubset();
		if (subset instanceof CircularWorldSubset) {
			CircularWorldSubset circularSubset = (CircularWorldSubset) subset;
			if (circularSubset.getOrigin() == null) {
				Vector3l origin = levelDat.getSpawnPosition();

				//For the Nether we try to find a player or Respawn Anchor closest to overworld spawn and use that as the origin
				//otherwise we just use the overworld spawn position as the origin
				if (dimension == Dimension.NETHER) {
					origin = getNetherOriginFromPlayers();
				}

				circularSubset.setOrigin(origin);
			}
			this.worldSubset = circularSubset;
		} else {
			this.worldSubset = subset;
		}
                
         	rawLoadedChunks = new RawCache(2048, (coord) -> {
                        CompositeBlockFilter composite = new CompositeBlockFilter();
                        composite.add(blockFilter);
                        composite.add(worldSubset.getBlockFilter(coord));

                        return chunkLocator.loadChunkFromRegion(coord, composite, worldInfo);
                });
		geometryLoadedChunks = new GeometryCache(512, (coord) -> {
                        Chunk chunk = rawLoadedChunks.get(coord);

                        final boolean ok = chunk.createGeometry(rasteriser, this, registry, modelRegistry, blockMaskFactory, texturePack);
                        assert ok;

                        return chunk;
                });
		
		this.lightStyle = LightStyle.None;
		
		this.daySkybox = SkyboxUtil.generateDaySkybox(rasteriser);
		this.nightSkybox = SkyboxUtil.generateNightSkybox(rasteriser);

		this.unknownBlocks = new HashMap<>();

		this.smoothLit = map.isSmoothLit();
		this.nightLightAdjustment = smoothLit ? 0.3f : 0.1f;
	}

	public Vector3l getNetherOriginFromPlayers() {
		Vector3l spawnPosition = levelDat.getSpawnPosition();
		Vector3l origin = new Vector3l(spawnPosition);
		double prevDistance = 99999999999d;

		//TODO: need to use player filter here
		for (Player player : players) {
			if (player.getDimension() == Dimension.NETHER) {
				Vector3d position = player.getPosition();
				double distance = Math.hypot(position.x - spawnPosition.x, position.z - spawnPosition.z);
				if (distance < prevDistance) {
					origin = new Vector3l((long) position.x, (long) position.y, (long) position.z);
				}
				prevDistance = distance;
			}

			if (player.getSpawnDimension() == Dimension.NETHER) {
				Vector3l position = player.getSpawnPosition();
				double distance = Math.hypot(position.x - (double) spawnPosition.x, position.z - (double) spawnPosition.z);
				if (distance < prevDistance) {
					origin = new Vector3l(position.x, position.y, position.z);
				}
				prevDistance = distance;
			}
		}

		return origin;
	}
	
	public void loadBlockRegistry(String customConfigPath, final boolean useDefaultBlocks)
	{
		registry = new BlockTypeRegistry();
		registry.setDefaultBlock(new Air());
		
		modelRegistry = new BlockRegistry(texturePack);  //model registry loads all states and models now
		
		BlockRegistryParser parser = new BlockRegistryParser(texturePack, biomeCache, signFilter);
		
		if (useDefaultBlocks && this.textureVersion == VERSION_4)
			parser.parse("defaultBlockConfigMC1.4.xml", registry);
		else if (useDefaultBlocks && this.textureVersion == VERSION_5)
			parser.parse("defaultBlockConfigMC1.5.xml", registry);
		else if (useDefaultBlocks && this.textureVersion == VERSIONS_6_TO_8)
			parser.parse("defaultBlockConfigMC1.8.xml", registry);
		else if (useDefaultBlocks && this.textureVersion == VERSIONS_9_TO_11)
			parser.parse("defaultBlockConfigMC1.9.xml", registry);
		else if (useDefaultBlocks && this.textureVersion == VERSION_12)
			parser.parse("defaultBlockConfigMC1.12.xml", registry);
		else if (useDefaultBlocks && this.textureVersion == VERSION_13)
			parser.parse("defaultBlockConfigMC1.13.xml", registry);
		else if (useDefaultBlocks && this.textureVersion == VERSION_14)
			parser.parse("defaultBlockConfigMC1.14.xml", registry);
		else
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

	public void setDefaultBlockName(final String blockName)
	{
		// Clear the geometry cache if block has changed
		if (!this.defaultBlockName.equals(blockName))
		{
			flushChunkCache();
		}

		this.defaultBlockName = blockName;
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
	
	public PlayerSkinCache getPlayerSkinCache()
	{
		return playerSkinCache;
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
		return worldSubset.createRegionIterator(regionCache.getFormat(), dimensionDir);
	}
	
	public boolean contains(ChunkCoord coord)
	{
		return worldSubset.contains(coord);
	}

	public List<Player> getAllPlayers()
	{
		return Collections.unmodifiableList(players);
	}

	/** Gets the players for a particular dimension */
	public List<Player> getPlayers(Dimension dimension)
	{
		List<Player> result = new ArrayList<>();
		for (Player p : players) {
			if (p.getDimension() == dimension)
				result.add(p);
		}
		return result;
	}
	
	public ConcurrentLinkedQueue<ContainerEntity> getChests()
	{
		return chests;
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
	public List<ChunkCoord> findVisible(Camera camera)
	{
		List<ChunkCoord> result = new ArrayList<>();
		
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
				BoundingBox regionBounds = new BoundingBox(new Vector3f(regionX*RegionCoord.REGION_WIDTH*RawChunk.WIDTH, 0, regionZ*RegionCoord.REGION_HEIGHT*RawChunk.DEPTH),
																RawChunk.WIDTH*RegionCoord.REGION_WIDTH, Minecraft.getChunkHeight(),
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
								BoundingBox chunkBounds = new BoundingBox(new Vector3f(chunkCoord.x*RawChunk.WIDTH, 0, chunkCoord.z*RawChunk.DEPTH), RawChunk.WIDTH, Minecraft.getChunkHeight(), RawChunk.DEPTH);
								if (chunkBounds.isVisible(camera) && chunkLocator.exists(chunkCoord))
								{
									result.add(chunkCoord);
								}
							}
						}
					}
					
				}
			}
		}
		
		return result;
	}
	
	public void draw(Camera camera, final boolean showSky, final boolean genAlphaMask)
	{
		// Find visible chunks
		List<ChunkCoord> visible = findVisible(camera);
			
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
				offset.mul(+0.20f); // Ugh. Arbitrary fudge factor because camera.getClearQuad doesn't seem to quite work for perspective camera
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
		List<ChunkCoord> toProcess = new ArrayList<>(visible);
		while (!toProcess.isEmpty())
		{
			List<ChunkCoord> batch = new ArrayList<>();
			
			final int nextBatchSize = Math.min(BATCH_SIZE, toProcess.size());
			for (int i=0; i<nextBatchSize; i++)
			{
				batch.add( toProcess.remove(0) );
			}
			
			draw(camera, batch, genAlphaMask);
		}
	}
	
	private void draw(Camera camera, List<ChunkCoord> visible, final boolean genAlphaMask)
	{
		// Find all visible chunks coordinates
		List<ChunkCoord> visibleChunks = new ArrayList<>();
		for (ChunkCoord coord : visible)
		{
                        if (worldSubset.contains(coord))
                        {
                                visibleChunks.add(coord);
                        }			
		}
		//System.out.println("Num visible chunks: " + visibleChunks.size());
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
                
                // Unload raw data and geometry from chunks that were evicted from cache
                rawLoadedChunks.unloadInvalidatedChunks();
                geometryLoadedChunks.unloadInvalidatedChunks();
	}
	
	private void drawGeometry(Camera camera, List<ChunkCoord> visible)
	{
		rasteriser.enableDepthWriting(true);
		
		// Solid pass
		
		rasteriser.enableBlending(false);
		rasteriser.enableAlphaTest(false);
		
		for (ChunkCoord coord : visible)
		{
                        geometryLoadedChunks.get(coord).drawSolid(camera);
		}
		
		// Alpha test pass
		
		rasteriser.enableAlphaTest(true);
		rasteriser.setAlphaFunc(AlphaFunc.Greater, 0.4f);
		rasteriser.enableBlending(false);
		
		for (ChunkCoord coord : visible)
		{	
			geometryLoadedChunks.get(coord).drawAlphaTestedSurfaces(camera);
		}
		
		// Transparency pass
		
		rasteriser.enableDepthWriting(false);  //TODO: This is the cause of the weirdness involving ice as well as glass blocks in a beam
		rasteriser.enableBlending(true);
		rasteriser.enableAlphaTest(false);
		
		for (ChunkCoord coord : visible)
		{	
			geometryLoadedChunks.get(coord).drawTransparentSurfaces(camera);
		}
		
		rasteriser.enableDepthWriting(true);
	}
	
	public static ChunkCoord getTileCoord(File datFile)
	{
		try {
			// "c.0.0.dat"
			String name = datFile.getName();
			
			final int datPos = name.indexOf(".dat");
			if (datPos != -1)
			{
				name = name.substring(0, datPos);
				final int dotPos = name.indexOf('.');
				if (dotPos != -1 && dotPos < name.length()-1)
				{
					name = name.substring(dotPos+1);
					
					long first = Util.fromBase36( name.substring(0, name.indexOf('.')));
					long second = Util.fromBase36( name.substring(name.indexOf('.')+1));
					
					return new ChunkCoord(first, second);
				}
			}
		} catch (Exception e) {
			log.warn("Couldn't get tile coord from {}. File will be ignored", datFile.getAbsolutePath(), e);
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
		if (y < 0 || y >= Minecraft.getChunkHeight())
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
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return registry.find(defaultBlockId, 0);
		
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		if (c == null)
		{
			return registry.find(defaultBlockId, 0);
		}
		else
		{
			final String name = c.getRawChunk().getBlockName(loc.x, loc.y, loc.z);

			if (name != null)
				return registry.find(name);

			final int id = c.getBlockId(loc.x, loc.y, loc.z, defaultBlockId);
			final int data = c.getRawChunk().getBlockData(loc.x, loc.y, loc.z);
			return registry.find(id, data);
		}
	}

	@Override
	public BlockStateWrapper getBlock(ChunkCoord chunkCoord, int x, int y, int z)
	{
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return modelRegistry.getBlock(defaultBlockName);

		Location location = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(location.coord);
		if (c == null)
		{
			return modelRegistry.getBlock(defaultBlockName);
		}
		else
		{
			final String name = c.getRawChunk().getBlockName(location.x, location.y, location.z);

			if (name != null) {
				BlockStateWrapper block = modelRegistry.getBlock(name);
				if (block != null) {
					return block;
				} else {
					unknownBlocks.computeIfAbsent(location, loc -> {
						log.warn("Unable to find {} block in registry. {}, Region file: {}", name, loc, RegionCoord.getFilenameFromChunkCoord(loc.getCoord()));
						return name;
					});

					return modelRegistry.getBlock(defaultBlockName);
				}
			} else {
				return modelRegistry.getBlock(defaultBlockName);
			}
		}
	}

	//Use with xyz that coords that don't go outside the chunk
	public BlockStateWrapper getBlock(RawChunk rawChunk, int x, int y, int z)
	{
		final String name = rawChunk.getBlockName(x, y, z);

		if (name != null) {
			BlockStateWrapper block = modelRegistry.getBlock(name);
			if (block != null) {
				return block;
			} else {
				log.warn("Unable to find {} block in registry.", name);
				return modelRegistry.getBlock(defaultBlockName);
			}
		} else {
			return modelRegistry.getBlock(defaultBlockName);
		}
	}

	@Override
	public BlockProperties getBlockState(ChunkCoord chunkCoord, int x, int y, int z)
	{
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return null;

		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		if (c == null)
		{
			return null;
		}
		else
		{
			return c.getRawChunk().getBlockState(loc.x, loc.y, loc.z);
		}
	}
	
	@Override
	public Biome getBiome(ChunkCoord chunkCoord, int x, int y, int z)
	{
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return Biomes.THE_VOID;

		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		if (c == null)
		{
			return Biomes.THE_VOID;
		}
		else
			return c.getBiome(loc.x, loc.y, loc.z);
	}
	
	@Override
	public float getLight(ChunkCoord chunkCoord, final int x, final int y, final int z, LightFace face)
	{
		Location loc = resolve(chunkCoord, x, y, z);
		Chunk c = rawLoadedChunks.get(loc.coord);
		RawChunk raw = c != null ? c.getRawChunk() : null;
		
		return Chunk.getLight(lightStyle, face, raw, loc.x, loc.y, loc.z, nightLightAdjustment);
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
			final float worldY = Minecraft.getChunkHeight() / 2.0f;
			final float worldZ = coord.z * RawChunk.DEPTH + (RawChunk.DEPTH / 2.0f);
			final float centerDist = Chunk.getDistance(camera, worldX, worldY, worldZ);
			return centerDist;
		}
	}

	@Value
	private static class Location
	{
		public ChunkCoord coord;
		public int x, y, z;
		
		public Location(ChunkCoord coord, final int x, final int y, final int z)
		{
			this.coord = coord;
			
			this.x = x;
			this.y = y;
			this.z =z;
		}
	}
	
	
	public static List<Player> loadPlayers(File worldDir, PlayerSkinCache playerSkinCache)
	{
		File playersDir = Minecraft.findPlayersDir(worldDir);
		
		log.info("Loading players from {}", playersDir.getAbsolutePath());
		
		ArrayList<Player> players = new ArrayList<>();
		File[] playerFiles = playersDir.listFiles();
		if (playerFiles != null)
		{
			ExecutorService executor = Executors.newCachedThreadPool();
			for (File playerFile : playerFiles)
			{
				if (playerFile.getName().endsWith(".dat"))
				{
					try
					{
						Player player = new Player(playerFile.toPath());
						
						CacheEntry ce = playerSkinCache.getCacheEntry(player.getUUID());
						if (ce != null)
						{
							final long age = System.currentTimeMillis() - ce.fetchedTime;
							if (age < 1000 * 60 * 60  * 60) // one hour in ms
							{
								player.setName(ce.playerName);
								player.setSkinURL(ce.skinURL);
							}
							else
							{
								//refresh name and skin
								RequestPlayerInfoTask task = player.new RequestPlayerInfoTask();
								executor.submit(task);
							}
						}
						else
						{
							RequestPlayerInfoTask task = player.new RequestPlayerInfoTask();
							executor.submit(task);
						}			            
			            
						players.add(player);
					}
					catch (Exception e)
					{
						log.warn("Couldn't load player info from {}", playerFile.getName());
					}
				}
			}

			try {
				executor.shutdown();
				executor.awaitTermination(2, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		log.debug("\tloaded {} players", players.size());
		
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
	
	public SignEntity[] getLoadedSigns()
	{
		Map<String, SignEntity> result = new HashMap<>();
		
		for (Chunk c : rawLoadedChunks.values())
		{
			result.putAll( c.getSigns() );
		}
		
		return result.values().toArray(new SignEntity[0]);
	}

	@Override
	public Colour4f getGrassColor(ChunkCoord chunkCoord, int x, int y, int z) {
		return getPlantTintColor(chunkCoord, x, y, z, false);
	}

	@Override
	public Colour4f getFoliageColor(ChunkCoord chunkCoord, int x, int y, int z) {
		return getPlantTintColor(chunkCoord, x, y, z, true);
	}

	public Colour4f getPlantTintColor(ChunkCoord chunkCoord, int x, int y, int z, boolean isFoliage) {
		final Biome biome = getBiome(chunkCoord, x, y, z);
		final Biome northBiome = getBiome(chunkCoord, x, y, z-1);
		final Biome southBiome = getBiome(chunkCoord, x, y, z+1);
		final Biome eastBiome = getBiome(chunkCoord, x+1, y, z);
		final Biome westBiome = getBiome(chunkCoord, x-1, y, z);
		final Biome northEastBiome = getBiome(chunkCoord, x+1, y, z-1);
		final Biome northWestBiome = getBiome(chunkCoord, x-1, y, z-1);
		final Biome southEastBiome = getBiome(chunkCoord, x+1, y, z+1);
		final Biome southWestBiome = getBiome(chunkCoord, x-1, y, z+1);

		Colour4f centerColor;
		Colour4f northColor;
		Colour4f southColor;
		Colour4f eastColor;
		Colour4f westColor;
		Colour4f northEastColor;
		Colour4f northWestColor;
		Colour4f southEastColor;
		Colour4f southWestColor;

		//TODO: this version checking may need to be based off the chunk version not the texturepack version.  Do some testing.
		//The new way to find biome colors was actually only added in 1.7.2, but for my own
		//sanity I'm not going to worry about letting the new method work with 1.6
		if (textureVersion.getNumVersion() < VERSIONS_6_TO_8.getNumVersion()) {
			centerColor = getTintColorOld(biome.getNumericId(), isFoliage);
			northColor = getTintColorOld(northBiome.getNumericId(), isFoliage);
			southColor = getTintColorOld(southBiome.getNumericId(), isFoliage);
			eastColor = getTintColorOld(eastBiome.getNumericId(), isFoliage);
			westColor = getTintColorOld(westBiome.getNumericId(), isFoliage);
			northEastColor = getTintColorOld(northEastBiome.getNumericId(), isFoliage);
			northWestColor = getTintColorOld(northWestBiome.getNumericId(), isFoliage);
			southEastColor = getTintColorOld(southEastBiome.getNumericId(), isFoliage);
			southWestColor = getTintColorOld(southWestBiome.getNumericId(), isFoliage);
		} else {
			if (textureVersion.getNumVersion() < VERSION_15.getNumVersion()) {
				int elevation = Math.max(y - 64, 0);

				centerColor = getTintColor(biome, elevation, isFoliage);
				northColor = getTintColor(northBiome, elevation, isFoliage);
				southColor = getTintColor(southBiome, elevation, isFoliage);
				eastColor = getTintColor(eastBiome, elevation, isFoliage);
				westColor = getTintColor(westBiome, elevation, isFoliage);
				northEastColor = getTintColor(northEastBiome, elevation, isFoliage);
				northWestColor = getTintColor(northWestBiome, elevation, isFoliage);
				southEastColor = getTintColor(southEastBiome, elevation, isFoliage);
				southWestColor = getTintColor(southWestBiome, elevation, isFoliage);
			} else { //1.15+
				centerColor = getTintColor(biome, isFoliage);
				northColor = getTintColor(northBiome, isFoliage);
				southColor = getTintColor(southBiome, isFoliage);
				eastColor = getTintColor(eastBiome, isFoliage);
				westColor = getTintColor(westBiome, isFoliage);
				northEastColor = getTintColor(northEastBiome, isFoliage);
				northWestColor = getTintColor(northWestBiome, isFoliage);
				southEastColor = getTintColor(southEastBiome, isFoliage);
				southWestColor = getTintColor(southWestBiome, isFoliage);
			}
		}

		Colour4f color = new Colour4f(centerColor);
		color.add(northColor);
		color.add(southColor);
		color.add(eastColor);
		color.add(westColor);
		color.add(northEastColor);
		color.add(northWestColor);
		color.add(southEastColor);
		color.add(southWestColor);
		color.divide(9);

		return color;
	}

	// 1.6 and earlier
	private Colour4f getTintColorOld(final int id, boolean isFoliage) {
		Point colorCoords = BiomeIds.getColourCoord(id);
		return getTintColorFromBiomeTextures(colorCoords.x, colorCoords.y, isFoliage);
	}

	//versions 1.7.2 - 1.14.4
	private Colour4f getTintColor(Biome biome, int elevation, boolean isFoliage)
	{
		if (biome == BiomesOld.SWAMP || biome == BiomesOld.SWAMP_HILLS) {
			return new Colour4f(106, 112, 57);
		} else if (biome == BiomesOld.BADLANDS || biome == BiomesOld.ERODED_BADLANDS || biome == BiomesOld.BADLANDS_PLATEAU
				|| biome == BiomesOld.WOODED_BADLANDS_PLATEAU || biome == BiomesOld.MODIFIED_BADLANDS_PLATEAU
				|| biome == BiomesOld.MODIFIED_WOODED_BADLANDS_PLATEAU) {
			if (isFoliage) {
				return new Colour4f(158, 129, 77);
			} else {
				return new Colour4f(144, 129, 77);
			}
		}

		float adjTemp = BiomeUtils.clamp(biome.getTemperature() - elevation * 0.00166667f);
		float adjRainfall = BiomeUtils.clamp(biome.getRainfall()) * adjTemp;

		if(!isFoliage && (biome == BiomesOld.DARK_FOREST || biome == BiomesOld.DARK_FOREST_HILLS)) {
			Colour4f origColor = getTintColorFromBiomeTextures(BiomeUtils.normalize(adjTemp), BiomeUtils.normalize(adjRainfall), false);
			return new Colour4f((origColor.toInt() & 16711422) + 2634762 >> 1);
		} else {
			return getTintColorFromBiomeTextures(BiomeUtils.normalize(adjTemp), BiomeUtils.normalize(adjRainfall), isFoliage);
		}
	}

	// 1.15+
	private Colour4f getTintColor(Biome biome, boolean isFoliage) {
		if (isFoliage) {
			return texturePack.getFoliageColor(biome);
		} else {
			return texturePack.getGrassColor(biome);
		}
	}

	private Colour4f getTintColorFromBiomeTextures(int x, int y, boolean isFoliage) {
		Color awtColour;
		if (isFoliage) {
			awtColour = texturePack.getFoliageColour(x, y);
		} else {
			awtColour = texturePack.getGrassColour(x, y);
		}
		return new Colour4f(awtColour);
	}

	@Override
	public Colour4f getWaterColor(RawChunk rawChunk, int x, int y, int z) {
		ChunkCoord chunkCoord = rawChunk.getChunkCoord();
		final Biome biome = rawChunk.getBiome(x, y, z);
		final Biome northBiome = getBiome(chunkCoord, x, y, z-1);
		final Biome southBiome = getBiome(chunkCoord, x, y, z+1);
		final Biome eastBiome = getBiome(chunkCoord, x+1, y, z);
		final Biome westBiome = getBiome(chunkCoord, x-1, y, z);
		final Biome northEastBiome = getBiome(chunkCoord, x+1, y, z-1);
		final Biome northWestBiome = getBiome(chunkCoord, x-1, y, z-1);
		final Biome southEastBiome = getBiome(chunkCoord, x+1, y, z+1);
		final Biome southWestBiome = getBiome(chunkCoord, x-1, y, z+1);

		Colour4f colour = new Colour4f(biome.getWaterColor());
		colour.add(northBiome.getWaterColor());
		colour.add(southBiome.getWaterColor());
		colour.add(eastBiome.getWaterColor());
		colour.add(westBiome.getWaterColor());
		colour.add(northEastBiome.getWaterColor());
		colour.add(northWestBiome.getWaterColor());
		colour.add(southEastBiome.getWaterColor());
		colour.add(southWestBiome.getWaterColor());
		colour.divide(9);

		return colour;
	}
}
