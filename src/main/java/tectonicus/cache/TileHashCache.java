/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.cache;

import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.DimensionConfig;
import tectonicus.renderer.OrthoCamera;
import tectonicus.world.World;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Shares current tile hashes across layers of one map during one render. Not thread safe. */
@Slf4j
public final class TileHashCache implements AutoCloseable {

	private final File cacheFile;
	private final MVStore store;
	private final MVMap<String, byte[]> hashes;
	private final Function<TileCoord, byte[]> calculator;

	/** A null cache file disables memoization for single-layer or uncached renders. */
	public TileHashCache(File cacheFile, World world, DimensionConfig map, OrthoCamera camera, RegionHashStore regionHashes,
			int tileWidth, int tileHeight, MessageDigest hashAlgorithm) {
		this(cacheFile, tile -> {
			TileRenderer.setupCameraForTile(camera, tile, tileWidth, tileHeight,
					map.getCameraAngleRad(), map.getCameraElevationRad(), map.getClosestZoomSize());
			return hashChunks(world.findVisible(camera), regionHashes::getChunkHash,
					world.getLevelDat().getWorldSeed(), hashAlgorithm);
		});
	}

	TileHashCache(File cacheFile, Function<TileCoord, byte[]> calculator) {
		this.cacheFile = cacheFile;
		this.calculator = calculator;
		store = cacheFile == null ? null : new MVStore.Builder().fileName(cacheFile.getAbsolutePath()).open();
		hashes = store == null ? null : store.openMap("tileHashes");
		if (hashes != null)
			hashes.clear(); // Never reuse hashes from a previous render, including an interrupted one.
	}

	public byte[] getHash(TileCoord tile) {
		if (hashes == null)
			return calculator.apply(tile);

		String key = "tile_" + tile.x + "_" + tile.y;
		byte[] hash = hashes.get(key);
		if (hash == null) {
			hash = calculator.apply(tile);
			hashes.put(key, hash);
		}
		return hash;
	}

	static byte[] hashChunks(List<ChunkCoord> chunks, Function<ChunkCoord, byte[]> chunkHashes,
			long worldSeed, MessageDigest hashAlgorithm) {
		if (chunks.isEmpty())
			return new byte[0];

		// Preserve the existing tile hash format so saved layer caches remain valid.
		chunks.sort(Comparator.naturalOrder());
		hashAlgorithm.reset();
		hashAlgorithm.update(("biomeSeed=" + worldSeed + "\n").getBytes(StandardCharsets.UTF_8));
		for (ChunkCoord chunk : chunks) {
			byte[] hash = chunkHashes.apply(chunk);
			if (hash != null)
				hashAlgorithm.update(hash);
		}
		return hashAlgorithm.digest();
	}

	@Override
	public void close() {
		if (store != null) {
			store.close();
			if (!cacheFile.delete())
				log.warn("Could not delete temporary tile hashes at {}", cacheFile);
		}
	}
}
