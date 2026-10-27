/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import tectonicus.BlockRegistryParser;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class FileTileCache implements TileCache
{
	// Change this every time we have a major renderer change and need to invalidate the cache
	private static final int RENDERER_VERSION = 18;

	private final File tileCacheDir;
	
	private final ImageFormat imageFormat;
	
	private final Map<TileCoord, byte[]> tileHashes;
	
	private boolean wasExistingCacheValid;

	private final MVStore store;
	private final MVMap<String, byte[]> hashCache;
	private final MVMap<String, Boolean> downsampleCache;
	private final MVMap<String, Boolean> emptyTiles;

	public FileTileCache(File tileCacheDir, ImageFormat imageFormat, tectonicus.configuration.Map map, Layer layer, String optionString, MessageDigest hashAlgorithm)
	{
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
		emptyTiles = store.openMap("emptyTiles");

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
		buffer.append(args.getMinecraftJar().getAbsolutePath());
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
		mapStr += "biomeBlendRadius=" + map.getBiomeBlendRadius() + "\n";
		mapStr += "biomeBlendRadiusConfigured=" + map.isBiomeBlendRadiusConfigured() + "\n";
		
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
	public HddTileList findChangedTiles(HddTileListFactory factory, HddTileList visibleTiles, TileHashCache currentTileHashes, File layerDir)
	{
		final long start = System.currentTimeMillis();
		
		HddTileList result = factory.createList();
		
		int count = 0;
		
		for (TileCoord coord : visibleTiles)
		{
			// Check to see if the output tile exists
			
			boolean cacheOk = false;
			
			final byte[] newHash = currentTileHashes.getHash(coord);
			
			String key = "tile_" + coord.x + "_" + coord.y;
			if (Boolean.TRUE.equals(emptyTiles.get(key)) || TileRenderer.getImageFile(layerDir, coord.x, coord.y, imageFormat).exists()) {
				final byte[] cachedHash = hashCache.get(key);

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
				if (nextTiles.add(new TileCoord(x, y))) {
					downsampleCache.put("tile_" + x + "_" + y + "_zoom" + zoomLevel, false);
				}
			}

			zoomLevel--;
			prevTiles = nextTiles;
		}
	}

	@Override
	public HddTileList findTilesForDownsampling(HddTileListFactory factory, int zoomLevel, File baseDir, ImageFormat imageFormat) {
		HddTileList result = factory.createList();
		String zoomSuffix = "_zoom" + zoomLevel;

		for (Map.Entry<String, Boolean> entry : downsampleCache.entrySet()) {
			String key = entry.getKey();
			if (!key.endsWith(zoomSuffix))
				continue;

			boolean pending = !Boolean.TRUE.equals(entry.getValue());
			if (!pending && Boolean.TRUE.equals(emptyTiles.get(key)))
				continue;

			String[] keyStr = key.split("_");
			TileCoord coord = new TileCoord(Integer.parseInt(keyStr[1]), Integer.parseInt(keyStr[2]));
			if (pending || !TileRenderer.getImageFile(baseDir, coord.x, coord.y, imageFormat).exists()) {
				result.add(coord);
			}
		}

		return result;
	}
	
	public void writeImageCache(TileCoord coord)
	{
		writeImageCache(coord, false);
	}

	@Override
	public void writeImageCache(TileCoord coord, boolean empty)
	{
		assert (coord != null);

		byte[] hash = tileHashes.get(coord);
		if (hash == null)
			throw new RuntimeException("No hash for tile coord "+coord);

		String key = "tile_" + coord.x + "_" + coord.y;
		setEmpty(key, empty);
		hashCache.put(key, hash);
	}

	@Override
	public void updateTileDownsampleStatus(TileCoord coord, int zoomLevel) {
		updateTileDownsampleStatus(coord, zoomLevel, false);
	}

	@Override
	public void updateTileDownsampleStatus(TileCoord coord, int zoomLevel, boolean empty) {
		String key = "tile_" + coord.x + "_" + coord.y + "_zoom" + zoomLevel;
		setEmpty(key, empty);
		downsampleCache.put(key, true);
	}

	private void setEmpty(String key, boolean empty) {
		if (empty)
			emptyTiles.put(key, true);
		else
			emptyTiles.remove(key);
	}
	
	private static File getCacheFile(File cacheDir, TileCoord coord)
	{
		return new File(cacheDir, "tile_"+coord.x+"_"+coord.y+".cache");
	}

}
