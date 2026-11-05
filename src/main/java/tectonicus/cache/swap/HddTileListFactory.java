/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.LongDataType;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Owns all temporary tile sets for a render and their shared disk/cache budget. Not thread safe.
 * Closing the factory invalidates every list, including lists not explicitly closed by callers.
 */
public final class HddTileListFactory implements AutoCloseable {
	private final Path cacheFile;
	private MVStore store;
	private int counter;
	private boolean closed;

	public HddTileListFactory(File cacheDir) {
		Path directory = cacheDir.toPath();
		cacheFile = directory.resolve("tileLists.mv");
		try {
			// Also removes legacy directories and leftovers from interrupted runs.
			// walkFileTree does not follow links outside this temporary directory.
			if (Files.exists(directory)) {
				Files.walkFileTree(directory, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
						if (error != null)
							throw error;
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			}
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not prepare temporary tile lists at " + directory, e);
		}
	}

	public HddTileList createList() {
		MVMap<Long, Boolean> tiles = store().openMap("autoList" + counter++,
				new MVMap.Builder<Long, Boolean>().keyType(LongDataType.INSTANCE));
		return new HddTileList(this, tiles);
	}

	boolean isClosed() {
		return closed;
	}

	void remove(MVMap<Long, Boolean> tiles) {
		store.removeMap(tiles);
	}

	private MVStore store() {
		if (closed)
			throw new IllegalStateException("Tile list factory is closed");
		if (store == null)
			store = new MVStore.Builder().fileName(cacheFile.toString()).cacheSize(16).autoCommitBufferSize(1024).open();
		return store;
	}

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		// Nothing here survives the render. Release handles without flushing disposable data.
		if (store != null)
			store.closeImmediately();
		try {
			Files.deleteIfExists(cacheFile);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not delete temporary tile lists at " + cacheFile, e);
		}
	}
}
