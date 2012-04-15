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
package tectonicus.cache;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import tectonicus.BlockRegistryParser;
import tectonicus.ChunkCoord;
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

public class FileTileCache implements TileCache
{
	// Change this every time we have a major renderer change and need to invalidate the cache
	private static final int RENDERER_VERSION = 14;
	
	private final File tileCacheDir;
	
	private final ImageFormat imageFormat;
	
	private final MessageDigest hashAlgorithm;
	
	private Map<TileCoord, byte[]> tileHashes;
	
	private boolean wasExistingCacheValid;
	
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
			System.out.println("Tile cache directory found and is valid. Using cache at "+tileCacheDir.getAbsolutePath());
			wasExistingCacheValid = true;
		}
		else
		{
			// Not valid! Delete the directory and start from scratch
			System.out.println("Settings changed! Deleting tile cache at "+tileCacheDir.getAbsolutePath());
			System.out.println("New tile cache will be created for this render");
			FileUtils.deleteDirectory(tileCacheDir);
			tileCacheDir.mkdirs();
			CacheUtil.writeCacheFile(getMasterCacheFile(tileCacheDir), cacheString.getBytes());
		}
		
		tileHashes = new HashMap<TileCoord, byte[]>();
	}
	
	public boolean isUsingExistingCache()
	{
		return wasExistingCacheValid;
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
		buffer.append(args.maxTiles());
		buffer.append('\n');
		buffer.append(args.numZoomLevels());
		buffer.append('\n');
		buffer.append(args.tileSize());
		buffer.append('\n');
		
		// Framebuffer config
		buffer.append(args.colourDepth());
		buffer.append('\n');
		buffer.append(args.alphaBits());
		buffer.append('\n');
		buffer.append(args.numSamples());
		buffer.append('\n');
		
		// Paths
		buffer.append(args.outputDir().getAbsolutePath());
		buffer.append('\n');
		buffer.append(args.minecraftJar().getAbsolutePath());
		buffer.append('\n');
		buffer.append(args.texturePack() != null ? args.texturePack().getAbsolutePath() : "");
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
		mapStr += map.getWorldSubsetFactory().getDescription() + "\n";
		
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
			if (imgFile.exists())
			{
				File hashFile = getCacheFile(tileCacheDir, coord);
				final byte[] cachedHash = CacheUtil.readHash(hashFile);
				
				cacheOk = CacheUtil.equal(cachedHash, newHash);
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
				System.out.println(percentage+"%");
			}
		}
		
		System.out.println("100%");
		
		final long end = System.currentTimeMillis();
		final long diff = end - start;
		System.out.println("FindChangedTiles took: "+(diff/1000.0f)+" seconds");
		
		return result;
	}
	
	public void writeImageCache(TileCoord coord)
	{
		assert (coord != null);
		
		byte[] hash = tileHashes.get(coord);
		if (hash == null)
			throw new RuntimeException("No hash for tile coord "+coord);
		
		File hashFile = getCacheFile(tileCacheDir, coord);
		CacheUtil.writeCacheFile(hashFile, hash);
	}
	
	private byte[] calculateTileHash(World world, tectonicus.configuration.Map map, RegionHashStore regionHashStore, OrthoCamera camera, TileCoord tile, final int zoom, final int tileWidth, final int tileHeight)
	{
		assert (world != null);
		assert (camera != null);
		assert (tile != null);
		
		TileRenderer.setupCameraForTile(camera, tile, tileWidth, tileHeight, map.getCameraAngleRad(), map.getCameraElevationRad(), zoom);
		
		ArrayList<ChunkCoord> chunks = world.findVisible(camera);
		
		if (chunks.isEmpty())
			return new byte[0];
		
		Collections.sort(chunks, ChunkSorter.instance);
		
		// Find hash of each chunk
		ArrayList<byte[]> hashes = new ArrayList<byte[]>();		
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
