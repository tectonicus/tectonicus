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
import org.tectonicus.jnbt.ByteArrayTag;
import org.tectonicus.jnbt.CompoundTag;
import tectonicus.Minecraft;
import tectonicus.RegionCoord;
import tectonicus.bedrock.leveldb.LevelDbReader;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.world.DimensionHeight;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

	/** Chunks whose blocks are kept for their neighbours; Tectonicus renders a few rows of chunks at a time. */
	private static final int BLOCK_CACHE_SIZE = 128;
	private static final Map<Path, BedrockWorld> OPEN = new ConcurrentHashMap<>();

	private final Path worldDir;
	private final LevelDbReader db;
	private final Map<Integer, Set<ChunkCoord>> chunks = new ConcurrentHashMap<>();
	private final Map<Integer, Set<RegionCoord>> regions = new ConcurrentHashMap<>();
	private final Map<BlockKey, Blocks> blockCache = new LinkedHashMap<>(BLOCK_CACHE_SIZE, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<BlockKey, Blocks> eldest) {
			return size() > BLOCK_CACHE_SIZE;
		}
	};

	/** One dimension of a Bedrock world. Tectonicus passes dimensions around as directories, so this resolves them. */
	public record Dimension(BedrockWorld world, int id) {
		public boolean contains(ChunkCoord coord) {
			return world.chunks(id).contains(coord);
		}

		public List<ChunkCoord> chunkCoords() {
			return world.chunkCoords(id);
		}

		/** Whether any chunk of this dimension lies in the 32x32 chunk area of a Java region. */
		public boolean hasRegion(RegionCoord coord) {
			return world.regions(id).contains(coord);
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

	Set<RegionCoord> regions(int dimension) {
		return regions.computeIfAbsent(dimension, d -> {
			Set<RegionCoord> found = new HashSet<>();
			for (ChunkCoord coord : chunks(d))
				found.add(RegionCoord.fromChunkCoord(coord));
			return Collections.unmodifiableSet(found);
		});
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

	/**
	 * Reads a chunk using the dimension height Tectonicus is currently rendering with. Light and the block states that
	 * depend on neighbouring blocks come from the chunk and the eight around it.
	 */
	RawChunk read(int dimension, ChunkCoord coord) throws IOException {
		DimensionHeight height = Minecraft.getDimensionHeight();
		int minSectionY = height.minSectionY();
		int sectionCount = height.height() / RawChunk.SECTION_HEIGHT;
		int x = (int) coord.x;
		int z = (int) coord.z;
		Blocks[] around = new Blocks[9];
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++)
				around[(dz + 1) * 3 + dx + 1] = blocks(dimension, x + dx, z + dz, minSectionY, sectionCount);
		if (around[4] == null)
			around[4] = reader(dimension).readBlocks(x, z, minSectionY, sectionCount);
		Neighbourhood neighbourhood = Neighbourhood.of((dx, dz) -> around[(dz + 1) * 3 + dx + 1]);

		RawChunk chunk = reader(dimension).read(neighbourhood.centre(), neighbourhood);
		BlockStateFixups.apply(chunk, neighbourhood);
		LightEngine.light(chunk, neighbourhood, dimension == OVERWORLD);
		return chunk;
	}

	/** A chunk's blocks for use as a neighbour, from a small cache since each chunk is a neighbour of eight others. */
	private Blocks blocks(int dimension, int x, int z, int minSectionY, int sectionCount) throws IOException {
		ChunkCoord coord = new ChunkCoord(x, z);
		if (!chunks(dimension).contains(coord))
			return null;
		BlockKey key = new BlockKey(dimension, x, z, minSectionY, sectionCount);
		synchronized (blockCache) {
			Blocks cached = blockCache.get(key);
			if (cached != null)
				return cached;
		}
		Blocks blocks = reader(dimension).readBlocks(x, z, minSectionY, sectionCount);
		synchronized (blockCache) {
			blockCache.put(key, blocks);
		}
		return blocks;
	}

	private record BlockKey(int dimension, int x, int z, int minSectionY, int sectionCount) {}

	/** The local player, the one playing a single-player world, as Java player NBT, or null. */
	public CompoundTag localPlayer() {
		try {
			byte[] data = db.get("~local_player".getBytes(StandardCharsets.US_ASCII));
			return data == null ? null : BedrockPlayers.toJava(BedrockNbt.readCompound(data), BedrockBlockMapper.get());
		} catch (IOException | RuntimeException e) {
			log.warn("Could not read the local player of the Bedrock world in {}", worldDir, e);
			return null;
		}
	}

	/** Other players who have joined the world, as Java player NBT. */
	public List<CompoundTag> serverPlayers() {
		List<CompoundTag> players = new ArrayList<>();
		try {
			db.forEachWithPrefix("player_server_".getBytes(StandardCharsets.US_ASCII), (key, value) -> {
				try {
					players.add(BedrockPlayers.toJava(BedrockNbt.readCompound(value), BedrockBlockMapper.get()));
				} catch (IOException | RuntimeException e) {
					log.warn("Could not read Bedrock player {}", new String(key, StandardCharsets.US_ASCII), e);
				}
			});
		} catch (IOException e) {
			log.warn("Could not list the players of the Bedrock world in {}", worldDir, e);
		}
		return players;
	}

	/** A map's pixels as 128 by 128 RGBA bytes, or null when the world has no such map. */
	public byte[] mapPixels(long mapUuid) throws IOException {
		byte[] data = db.get(("map_" + mapUuid).getBytes(StandardCharsets.US_ASCII));
		if (data == null)
			return null;
		CompoundTag map = BedrockNbt.readCompound(data);
		return map.getValue().get("colors") instanceof ByteArrayTag colors ? colors.getValue() : null;
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
