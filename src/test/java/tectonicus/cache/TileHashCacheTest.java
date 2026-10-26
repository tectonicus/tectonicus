/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.cache;

import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.TileCoord;
import tectonicus.chunk.ChunkCoord;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TileHashCacheTest {
	@TempDir
	Path directory;

	@Test
	void calculatesEachTileOnceIncludingEmptyHashesAndDistinctFullCoordinates() {
		AtomicInteger calculations = new AtomicInteger();
		Path file = directory.resolve("hashes.cache");
		try (TileHashCache hashes = new TileHashCache(file.toFile(), tile -> {
			calculations.incrementAndGet();
			return tile.x == 1 ? new byte[0] : new byte[]{42};
		})) {
			for (int layer = 0; layer < 3; layer++) {
				assertArrayEquals(new byte[0], hashes.getHash(new TileCoord(1, -2)));
				assertArrayEquals(new byte[]{42}, hashes.getHash(new TileCoord(-65535, -2)));
			}
			assertEquals(2, calculations.get());
		}
		assertFalse(Files.exists(file));
	}

	@Test
	void discardsHashesLeftByAnEarlierRender() {
		Path file = directory.resolve("hashes.cache");
		try (MVStore store = new MVStore.Builder().fileName(file.toString()).open()) {
			store.<String, byte[]>openMap("tileHashes").put("tile_1_2", new byte[]{1});
		}
		try (TileHashCache hashes = new TileHashCache(file.toFile(), tile -> new byte[]{2})) {
			assertArrayEquals(new byte[]{2}, hashes.getHash(new TileCoord(1, 2)));
		}
	}

	@Test
	void doesNotRetainHashesWithoutASharedCache() {
		AtomicInteger calculations = new AtomicInteger();
		try (TileHashCache hashes = new TileHashCache(null, tile -> new byte[]{(byte) calculations.incrementAndGet()})) {
			assertArrayEquals(new byte[]{1}, hashes.getHash(new TileCoord(1, 2)));
			assertArrayEquals(new byte[]{2}, hashes.getHash(new TileCoord(1, 2)));
		}
		assertEquals(0, directory.toFile().list().length);
	}

	@Test
	void preservesTheSavedHashFormatAndChunkOrdering() throws Exception {
		ChunkCoord first = new ChunkCoord(1, 2);
		ChunkCoord second = new ChunkCoord(1, -3);
		ChunkCoord missing = new ChunkCoord(-1, 0);
		List<ChunkCoord> chunks = new ArrayList<>(List.of(missing, second, first));
		MessageDigest expected = MessageDigest.getInstance("SHA-1");
		expected.update("biomeSeed=42\n".getBytes(StandardCharsets.UTF_8));
		expected.update(new byte[]{1, 2, 3, 4});

		byte[] actual = TileHashCache.hashChunks(chunks, chunk -> {
			if (chunk.equals(first))
				return new byte[]{1, 2};
			if (chunk.equals(second))
				return new byte[]{3, 4};
			return null;
		}, 42, MessageDigest.getInstance("SHA-1"));

		assertArrayEquals(expected.digest(), actual);
	}

	@Test
	void preservesEmptyTilesAndIncludesTheWorldSeed() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		assertArrayEquals(new byte[0], TileHashCache.hashChunks(List.of(), chunk -> fail("No chunks expected"), 42, digest));
		byte[] first = TileHashCache.hashChunks(new ArrayList<>(List.of(new ChunkCoord(1, 2))), chunk -> null, 42, digest);
		assertArrayEquals(digest.digest("biomeSeed=42\n".getBytes(StandardCharsets.UTF_8)), first);
		byte[] second = TileHashCache.hashChunks(new ArrayList<>(List.of(new ChunkCoord(1, 2))), chunk -> null, 43, digest);
		assertFalse(java.util.Arrays.equals(first, second));
	}
}
