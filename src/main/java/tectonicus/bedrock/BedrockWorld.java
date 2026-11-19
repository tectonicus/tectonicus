/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import lombok.extern.slf4j.Slf4j;
import tectonicus.Minecraft;
import tectonicus.bedrock.leveldb.LevelDbReader;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.world.DimensionHeight;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Bedrock Edition world: {@code level.dat} plus a LevelDB database in {@code db}. The database is only ever read.
 * Worlds are opened once per process and shared, since every dimension and the portal scanner read the same database.
 */
@Slf4j
public final class BedrockWorld {
	public static final int OVERWORLD = 0;
	public static final int NETHER = 1;
	public static final int END = 2;

	private static final Map<Path, BedrockWorld> OPEN = new ConcurrentHashMap<>();

	private final Path worldDir;
	private final LevelDbReader db;
	private final Map<Integer, Set<ChunkCoord>> chunks = new ConcurrentHashMap<>();

	/** One dimension of a Bedrock world. Tectonicus passes dimensions around as directories, so this resolves them. */
	public record Dimension(BedrockWorld world, int id) {
		public boolean contains(ChunkCoord coord) {
			return world.chunks(id).contains(coord);
		}

		public List<ChunkCoord> chunkCoords() {
			return world.chunkCoords(id);
		}

		public RawChunk read(ChunkCoord coord) throws IOException {
			return world.read(id, coord);
		}
	}

	private BedrockWorld(Path worldDir, LevelDbReader db) {
		this.worldDir = worldDir;
		this.db = db;
	}

	/** Whether a folder holds a Bedrock world: a {@code db} folder and a Bedrock {@code level.dat}. */
	public static boolean isBedrockWorld(Path worldDir) {
		if (worldDir == null || !Files.isDirectory(worldDir.resolve("db")) || !Files.isRegularFile(worldDir.resolve("level.dat")))
			return false;
		try {
			return BedrockLevel.isBedrockLevelDat(worldDir.resolve("level.dat"));
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Resolves a dimension directory as Tectonicus names them: the world folder for the Overworld, and
	 * {@code DIM-1} and {@code DIM1} inside it for the Nether and End. Bedrock has no such folders, so they need not
	 * exist.
	 */
	public static Optional<Dimension> forDimensionDir(File dimensionDir) {
		if (dimensionDir == null)
			return Optional.empty();
		Path dir = dimensionDir.toPath().toAbsolutePath().normalize();
		if (isBedrockWorld(dir))
			return Optional.of(new Dimension(open(dir), OVERWORLD));
		Path parent = dir.getParent();
		if (parent == null || !isBedrockWorld(parent))
			return Optional.empty();
		return switch (dir.getFileName().toString()) {
			case "DIM-1" -> Optional.of(new Dimension(open(parent), NETHER));
			case "DIM1" -> Optional.of(new Dimension(open(parent), END));
			default -> Optional.empty();
		};
	}

	public static BedrockWorld open(Path worldDir) {
		Path key = worldDir.toAbsolutePath().normalize();
		return OPEN.computeIfAbsent(key, dir -> {
			try {
				log.info("Opening Bedrock world database in {}", dir.resolve("db"));
				return new BedrockWorld(dir, LevelDbReader.open(dir.resolve("db")));
			} catch (IOException e) {
				throw new UncheckedIOException("Could not open the Bedrock world database in " + dir, e);
			}
		});
	}

	public Path getWorldDir() {
		return worldDir;
	}

	Set<ChunkCoord> chunks(int dimension) {
		return chunks.computeIfAbsent(dimension, this::findChunks);
	}

	List<ChunkCoord> chunkCoords(int dimension) {
		List<ChunkCoord> coords = new ArrayList<>(chunks(dimension));
		coords.sort((a, b) -> a.x != b.x ? Long.compare(a.x, b.x) : Long.compare(a.z, b.z));
		return coords;
	}

	/** Lists a dimension's finished chunks from the keys of their version records. */
	private Set<ChunkCoord> findChunks(int dimension) {
		Set<ChunkCoord> found = new HashSet<>();
		int length = dimension == OVERWORLD ? 9 : 13;
		try {
			db.forEachKey(key -> {
				if (key.length != length)
					return;
				byte tag = key[length - 1];
				if (tag != BedrockChunkReader.TAG_VERSION && tag != BedrockChunkReader.TAG_VERSION_OLD)
					return;
				ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
				int x = buffer.getInt();
				int z = buffer.getInt();
				if (dimension != OVERWORLD && buffer.getInt() != dimension)
					return;
				found.add(new ChunkCoord(x, z));
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Could not list chunks in " + worldDir.resolve("db"), e);
		}
		BedrockChunkReader reader = reader(dimension);
		found.removeIf(coord -> {
			try {
				return !reader.isFinished((int) coord.x, (int) coord.z);
			} catch (IOException e) {
				log.warn("Could not read the state of Bedrock chunk {},{}", coord.x, coord.z, e);
				return true;
			}
		});
		log.info("Found {} chunks in Bedrock dimension {}", found.size(), dimension);
		return Collections.unmodifiableSet(found);
	}

	private BedrockChunkReader reader(int dimension) {
		return new BedrockChunkReader(db::get, dimension, BedrockBlockMapper.get());
	}

	/** Reads a chunk using the dimension height Tectonicus is currently rendering with. */
	RawChunk read(int dimension, ChunkCoord coord) throws IOException {
		DimensionHeight height = Minecraft.getDimensionHeight();
		return reader(dimension).read((int) coord.x, (int) coord.z, height.minSectionY(), height.height() / RawChunk.SECTION_HEIGHT);
	}

	/** The Bedrock dimension id for a Tectonicus dimension. */
	public static int dimensionId(tectonicus.configuration.Dimension dimension) {
		return switch (dimension) {
			case NETHER -> NETHER;
			case END -> END;
			default -> OVERWORLD;
		};
	}
}
