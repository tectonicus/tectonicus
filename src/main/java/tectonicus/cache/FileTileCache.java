/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import tectonicus.BlockRegistryParser;
import tectonicus.chunk.ChunkCoord;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.renderer.OrthoCamera;
import tectonicus.util.FileUtils;
import tectonicus.world.World;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class FileTileCache implements TileCache
{
	// Change this every time we have a major renderer change and need to invalidate the cache
	private static final int RENDERER_VERSION = 16;

	private final File tileCacheDir;
	
	private final ImageFormat imageFormat;
	
	private final MessageDigest hashAlgorithm;
	
	private final Map<TileCoord, byte[]> tileHashes;
	
	private boolean wasExistingCacheValid;

	private final MVStore store;
	private final MVMap<String, byte[]> hashCache;
	private final MVMap<String, Boolean> downsampleCache;

	public FileTileCache(File tileCacheDir, ImageFormat imageFormat, tectonicus.configuration.Map map, Layer layer, String optionString, MessageDigest hashAlgorithm)
	{
		this.hashAlgorithm = hashAlgorithm;
		this.imageFormat = imageFormat;
		
		this.tileCacheDir = tileCacheDir;
		tileCacheDir.mkdirs();
		
		// Verify the cache
		final String cacheString = optionString + getMapString(map) + getLayerString(layer);
		if (isCacheValid(tileCacheDir, CacheUtil.hash(cacheString, hashAlgorithm), hashAlgorithm))
		{
			// Ok!
			log.info("Tile cache directory found and is valid. Using cache at {}", tileCacheDir.getAbsolutePath());
			wasExistingCacheValid = true;
		}
		else
		{
			// Not valid! Delete the directory and start from scratch
			log.info("Settings changed! Deleting tile cache at {}", tileCacheDir.getAbsolutePath());
			log.info("New tile cache will be created for this render");
			FileUtils.deleteDirectory(tileCacheDir);
			tileCacheDir.mkdirs();
			CacheUtil.writeCacheFile(getMasterCacheFile(tileCacheDir), cacheString.getBytes());
		}

		store = new MVStore.Builder().fileName(tileCacheDir + "/tileRender.cache").compressHigh().open();
		hashCache = store.openMap("tileHashes");
		downsampleCache = store.openMap("tileDownsample");

		tileHashes = new HashMap<>();
	}
	
	public boolean isUsingExistingCache()
	{
		return wasExistingCacheValid;
	}

	@Override
	public boolean hasCreatedDownsampleCache() {
		return downsampleCache.size() > 0;
	}
	
	private static boolean isCacheValid(File cacheDir, byte[] expectedHash, MessageDigest hashAlgo)
	{
		final byte[] cacheContents = CacheUtil.calcHash( getMasterCacheFile(cacheDir), hashAlgo );
		
		return CacheUtil.equal(expectedHash, cacheContents);		
	}
	
	private static File getMasterCacheFile(File cacheDir)
	{
		return new File(cacheDir, "tiles.cache");
	}
	
	public static String calcOptionsString(Configuration args)
	{
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(RENDERER_VERSION);
		buffer.append('\n');
		
		// Rasteriser config
		buffer.append(args.getRasteriserType());
		buffer.append('\n');
		
		// Render config
		buffer.append(args.getMaxTiles());
		buffer.append('\n');
		buffer.append(args.getNumZoomLevels());
		buffer.append('\n');
		buffer.append(args.getTileSize());
		buffer.append('\n');
		
		// Framebuffer config
		buffer.append(args.getColourDepth());
		buffer.append('\n');
		buffer.append(args.getAlphaBits());
		buffer.append('\n');
		buffer.append(args.getNumSamples());
		buffer.append('\n');
		
		// Paths
		buffer.append(args.getOutputDir().getAbsolutePath());
		buffer.append('\n');
		buffer.append(args.minecraftJar().getAbsolutePath());
		buffer.append('\n');
		buffer.append(args.getTexturePack() != null ? args.getTexturePack().getAbsolutePath() : "");
		buffer.append('\n');
		
		return buffer.toString();
	}
	
	private static String getMapString(tectonicus.configuration.Map map)
	{
		String mapStr = "";
		
		mapStr += map.getId() + "\n";
		mapStr += map.getName() + "\n";
		mapStr += map.getCameraAngleRad() + "\n";
		mapStr += map.getCameraElevationRad() + "\n";
		mapStr += map.getDimension() + "\n";
		mapStr += map.getClosestZoomSize() + "\n";
		mapStr += map.getWorldSubset() + "\n";
		
		return mapStr;
	}
		
	private static String getLayerString(Layer layer)
	{
		String layerStr = "";
		
		layerStr += layer.getId() + "\n";
		layerStr += layer.getName() + "\n";
		layerStr += layer.getLightStyle() + "\n";
		layerStr += layer.getRenderStyle() + "\n";
		layerStr += layer.getImageFormat() + "\n";
		layerStr += layer.getImageCompressionLevel() + "\n";
		
		layerStr += layer.getCustomBlockConfig() + "\n";
		layerStr += layer.useDefaultBlockConfig() + "\n";
		
		layerStr += getCustomBlocksString(layer);
		
		return layerStr;
	}
	
	public static String getCustomBlocksString(Layer layer)
	{
		String configStr = "";
		
		// Also hash the custom block config file
		if (layer.getCustomBlockConfig() != null)
		{
			InputStream in = null;
			try
			{
				in = BlockRegistryParser.openStream(layer.getCustomBlockConfig());

				MessageDigest hashAlgo = MessageDigest.getInstance("sha1");
				hashAlgo.reset();

				while (true)
				{
					byte[] buffer = new byte[1024 * 4];
					final int bytesRead = in.read(buffer);
					if (bytesRead == -1)
						break;
					hashAlgo.update(buffer, 0, bytesRead);
				}

				byte[] result = hashAlgo.digest();

				configStr += "custom-blocks";

				for (byte b : result)
					configStr += b;

				configStr += '\n';
			}
			catch (Exception e) {}
			finally
			{
				try
				{
					if (in != null)
						in.close();
				}
				catch (Exception e) {}
			}
			
		}
		
		return configStr;
	}
	
	public void reset()
	{
		tileHashes.clear();
	}

	public void closeTileCache() {
		store.close();
	}

	@Override
	public HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles, RegionHashStore regionHashStore, World world, tectonicus.configuration.Map map, OrthoCamera camera, final int zoom, final int tileWidth, final int tileHeight, File layerDir)
	{
		final long start = System.currentTimeMillis();
		
		HddTileList result = factory.createList();
		
		int count = 0;
		
		for (TileCoord coord : visibleTiles)
		{
			// Check to see if the output tile exists
			
			boolean cacheOk = false;
			
			final byte[] newHash = calculateTileHash(world, map, regionHashStore, camera, coord, zoom, tileWidth, tileHeight);
			
			File imgFile = TileRenderer.getImageFile(layerDir, coord.x, coord.y, imageFormat);
			if (imgFile.exists()) {
				final byte[] cachedHash = hashCache.get("tile_"+coord.x+"_"+coord.y);

				if (cachedHash != null) {
					cacheOk = CacheUtil.equal(cachedHash, newHash);
				}
			}
			
			if (!cacheOk)
			{
				assert(newHash != null);
				
				result.add(coord);
				tileHashes.put(coord, newHash);
			}
			
			count++;
			if (count % 100 == 0)
			{
				final int percentage = (int)Math.floor((count / (float)visibleTiles.size()) * 100);
				System.out.print(percentage+"%\r"); //prints a carriage return after line
			}
		}
		
		System.out.println("100%");
		
		final long end = System.currentTimeMillis();
		final long diff = end - start;
		log.debug("FindChangedTiles took: "+(diff/1000.0f)+" seconds");
		
		return result;
	}

	@Override
	public void calculateDownsampledTileCoordinates(HddTileList baseTiles, int zoomLevel) {
		Set<TileCoord> prevTiles = baseTiles.toSet();

		while (zoomLevel >= 0) {
			Set<TileCoord> nextTiles = new HashSet<>();
			for (TileCoord c : prevTiles) {
				final int x = (int) Math.floor(c.x / 2.0f);
				final int y = (int) Math.floor(c.y / 2.0f);
				nextTiles.add(new TileCoord(x, y));
				downsampleCache.put("tile_" + x + "_" + y + "_zoom" + zoomLevel, false);
			}

			zoomLevel--;
			prevTiles = nextTiles;
		}
	}

	@Override
	public HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat) {
		HddTileList result = factory.createList();

		for (Map.Entry<String, Boolean> entry : downsampleCache.entrySet()) {
			String key = entry.getKey();
                        String[] keyStr = key.split("_");
                        
                        TileCoord coord = new TileCoord(Integer.parseInt(keyStr[1]), Integer.parseInt(keyStr[2]));
                        File tileFile = TileRenderer.getImageFile(baseDir, coord.x, coord.y, imageFormat);
                        
                        if (key.contains("zoom" + zoomLevel) && (Boolean.TRUE.equals(!entry.getValue()) || !tileFile.exists())) {
				result.add(coord);
			}
		}

		return result;
	}
	
	public void writeImageCache(TileCoord coord)
	{
		assert (coord != null);

		byte[] hash = tileHashes.get(coord);
		if (hash == null)
			throw new RuntimeException("No hash for tile coord "+coord);

		hashCache.put("tile_" + coord.x + "_" + coord.y, hash);
	}

	@Override
	public void updateTileDownsampleStatus(TileCoord coord, int zoomLevel) {
		downsampleCache.put("tile_" + coord.x + "_" + coord.y + "_zoom" + zoomLevel, true);
	}
	
	private byte[] calculateTileHash(World world, tectonicus.configuration.Map map, RegionHashStore regionHashStore, OrthoCamera camera, TileCoord tile, final int zoom, final int tileWidth, final int tileHeight)
	{
		assert (world != null);
		assert (camera != null);
		assert (tile != null);
		
		TileRenderer.setupCameraForTile(camera, tile, tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), zoom);
		
		List<ChunkCoord> chunks = world.findVisible(camera);
		
		if (chunks.isEmpty())
			return new byte[0];
		
		chunks.sort(ChunkSorter.instance);
		
		// Find hash of each chunk
		List<byte[]> hashes = new ArrayList<>();
		for (ChunkCoord coord : chunks)
		{
			final byte[] hash = regionHashStore.getChunkHash(coord);
			if (hash != null)
				hashes.add(hash);
		}
		
		// Create uber hash of all chunk's hashes combined
		hashAlgorithm.reset();
		for (byte[] b : hashes)
		{
			hashAlgorithm.update(b);
		}
		byte[] fullHash = hashAlgorithm.digest();
		
		assert (fullHash != null);
		
		return fullHash;
	}
	
	private static File getCacheFile(File cacheDir, TileCoord coord)
	{
		return new File(cacheDir, "tile_"+coord.x+"_"+coord.y+".cache");
	}

	public static class ChunkSorter implements Comparator<ChunkCoord>
	{
		public static final ChunkSorter instance = new ChunkSorter();
		
		@Override
		public int compare(ChunkCoord lhs, ChunkCoord rhs)
		{
			return lhs.compareTo(rhs);
		}
	}
}
