/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jnbt.NBTInputStream.Compression;
import tectonicus.cache.BiomeCache;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkData;
import tectonicus.exceptions.RegionProcessingException;
import tectonicus.exceptions.UnknownCompressionTypeException;
import tectonicus.world.WorldInfo;
import tectonicus.world.filter.BlockFilter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Region {
	private static final int COMPRESSION_TYPE_GZIP = 1;
	private static final int COMPRESSION_TYPE_DEFLATE = 2;

	public static final long WIDTH_IN_CHUNKS = 32;

	private static final long SECTOR_SIZE_BYTES = 1024 * 4L;
	private static final long MAX_CHUNKS = WIDTH_IN_CHUNKS * WIDTH_IN_CHUNKS;
	private static final long MAX_SECTORS_PER_CHUNK = 1024;
	private static final long MAX_SECTORS = MAX_CHUNKS * MAX_SECTORS_PER_CHUNK + 2; // +2 for header
	private static final long MAX_SIZE_BYTES = MAX_SECTORS * SECTOR_SIZE_BYTES;

	private final long actualFileSizeBytes;
	private long entityFileSizeBytes;

	@Getter
	private final RegionCoord regionCoord;

	@Getter
	private final File regionFile;
	@Getter
	private File entityRegionFile;
	private final byte[] bytes;
	private byte[] entityBytes;

	private final ChunkInfo[] info;

	public Region(File regionFile) throws RegionProcessingException, IOException {
		this.regionFile = regionFile;

		this.regionCoord = extractRegionCoord(regionFile);
		if (regionCoord == null)
			throw new RegionProcessingException("Couldn't extract region coord from " + regionFile.getName());

		info = new ChunkInfo[RegionCoord.REGION_WIDTH * RegionCoord.REGION_HEIGHT];
		for (int i = 0; i < info.length; i++)
			info[i] = new ChunkInfo();

		actualFileSizeBytes = regionFile.length();
		bytes = loadRegionFile(regionFile, false);

		Path entityFile = regionFile.toPath().getParent().getParent().resolve("entities/" + regionFile.getName());
		if (Files.exists(entityFile) && Files.size(entityFile) > 0) {
			this.entityRegionFile = entityFile.toFile();
			entityFileSizeBytes = Files.size(entityFile);
			entityBytes = loadRegionFile(entityFile.toFile(), true);
		}
	}

	private byte[] loadRegionFile(File regionFile, boolean isEntityFile) throws RegionProcessingException, IOException {
		try (RandomAccessFile file = new RandomAccessFile(regionFile, "r")) {
			ByteBuffer buffer = ByteBuffer.allocate(1024 * 4);

			// Read chunk locations
			if (!read(file, buffer))
				throw new RegionProcessingException(String.format("Failed to read chunk locations for region file %s", regionFile.getAbsolutePath()));

			buffer.rewind();

			// Parse chunk locations
			for (ChunkInfo chunkInfo : info) {
				final int val = buffer.getInt();
				final int offset = ((val >> 8) & 0xFFFFFF);
				final int numSectors = val & 0xFF;

				assert (offset < MAX_SECTORS);

				if (isEntityFile) {
					chunkInfo.setEntitySectorOffset(offset);
					chunkInfo.setEntityNumSectors(numSectors);
				} else {
					chunkInfo.setSectorOffset(offset);
					chunkInfo.setNumSectors(numSectors);
				}
			}

			byte[] regionBytes = new byte[(int) file.length()];
			file.seek(0);
			read(file, regionBytes);
			return regionBytes;
		}
	}

	public boolean containsChunk(ChunkCoord chunkCoord) {
		// First check to see if the chunk would be contained within this region
		RegionCoord actualRegion = RegionCoord.fromChunkCoord(chunkCoord);
		if (!actualRegion.equals(this.regionCoord))
			return false;

		// Now check to see if it actually exists
		final int header = getHeaderOffsetForChunk(chunkCoord);
		return info[header].getSectorOffset() != 0 && info[header].getNumSectors() != 0;
	}

	public ChunkCoord[] getContainedChunks() {
		List<ChunkCoord> result = new ArrayList<>();

		final long baseChunkX = regionCoord.x * RegionCoord.REGION_WIDTH;
		final long baseChunkZ = regionCoord.z * RegionCoord.REGION_HEIGHT;

		for (long x = 0; x < RegionCoord.REGION_WIDTH; x++) {
			for (long z = 0; z < RegionCoord.REGION_HEIGHT; z++) {
				final long chunkX = baseChunkX + x;
				final long chunkZ = baseChunkZ + z;

				final int header = getHeaderOffsetForChunk(chunkX, chunkZ);
				if (info[header].getSectorOffset() != 0 && info[header].getNumSectors() != 0) {
					result.add(new ChunkCoord(chunkX, chunkZ));
				}
			}
		}

		int count = 0;
		for (ChunkInfo chunkInfo : info) {
			if (chunkInfo.getSectorOffset() != 0 && chunkInfo.getNumSectors() != 0) {
				count++;
			}
		}
		if (count != result.size()) {
			log.error("Chunk count mismatch!");
		}

		return result.toArray(new ChunkCoord[0]);
	}

	private static int getHeaderOffsetForChunk(final ChunkCoord coord) {
		return getHeaderOffsetForChunk(coord.getX(), coord.getZ());
	}

	private static int getHeaderOffsetForChunk(final long chunkX, final long chunkZ) {
		// 4 * ((x mod 32) + (z mod 32) * 32
		return (int) ((chunkX & 31) + (chunkZ & 31) * 32);
	}

	private static boolean read(RandomAccessFile file, ByteBuffer buffer) throws IOException {
		int res;
		do {
			res = file.getChannel().read(buffer);
		}
		while (res != -1 && buffer.hasRemaining());
		return res != -1;
	}

	private static void read(RandomAccessFile file, byte[] dest) throws IOException {
		int position = 0;
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 4);
		int res;

		while (true) {
			buffer.clear();
			res = file.getChannel().read(buffer);
			if (res == -1)
				break;

			for (int i = 0; i < res; i++) {
				dest[position + i] = buffer.get(i);
			}

			position += res;
			if (position >= dest.length)
				break;
		}
	}

	public static RegionCoord extractRegionCoord(File file) {
		RegionCoord coord = null;

		try {
			String name = file.getName();
			if (name.startsWith("r.")
					&& (name.endsWith(SaveFormat.McRegion.extension) || name.endsWith(SaveFormat.Anvil.extension))) {
				// Looks valid

				final int firstDot = name.indexOf('.');
				final int secondDot = name.indexOf('.', firstDot + 1);
				final int thirdDot = name.indexOf('.', secondDot + 1);

				String xStr = name.substring(firstDot + 1, secondDot);
				String zStr = name.substring(secondDot + 1, thirdDot);

				final long x = Long.parseLong(xStr);
				final long z = Long.parseLong(zStr);

				coord = new RegionCoord(x, z);
			}
		} catch (Exception e) {
			log.error("Exception: ", e);
		}

		return coord;
	}

	@Getter
	@Setter
	private static class ChunkInfo {
		/**
		 * Position of chunk from beginning of file, in sectors
		 */
		private int sectorOffset;

		/**
		 * Length of chunk, in sectors
		 */
		private int numSectors;

		/**
		 * Position of entity chunk from beginning of file, in sectors
		 */
		private int entitySectorOffset;

		/**
		 * Length of entity chunk, in sectors
		 */
		private int entityNumSectors;
	}

	public Chunk loadChunk(ChunkCoord chunkCoord, BiomeCache biomeCache, BlockFilter filter, WorldStats worldStats, WorldInfo worldInfo) {
		if (!containsChunk(chunkCoord))
			return null;

		ChunkInfo chunkInfo = info[getHeaderOffsetForChunk(chunkCoord)];
		final int sectorOffset = chunkInfo.getSectorOffset();
		assert (sectorOffset >= 2); // First two sectors are the header info
		final int entitySectorOffset = chunkInfo.getEntitySectorOffset();

		Chunk chunk = new Chunk(chunkCoord, biomeCache);
		ChunkData chunkData = getChunkData(sectorOffset, actualFileSizeBytes, bytes);
		ChunkData entityChunkData;
		if (entitySectorOffset > 0) {
			entityChunkData = getChunkData(entitySectorOffset, entityFileSizeBytes, entityBytes);
			try {
				chunk.loadRaw(chunkData, entityChunkData, filter, worldStats, worldInfo);
			} catch (IOException e) {
				log.error("Error while trying to load entities for chunk at ({}, {}) from region {}", chunkCoord.getX(), chunkCoord.getZ(), entityRegionFile.getAbsolutePath(), e);
			}
		}

		try {
			chunk.loadRaw(chunkData, filter, worldStats, worldInfo);
		} catch (IOException e) {
			log.error("Error while trying to load chunk at ({}, {}) from region {}", chunkCoord.getX(), chunkCoord.getZ(), entityRegionFile.getAbsolutePath(), e);
		}

		return chunk;
	}

	private ChunkData getChunkData(int sectorOffset, long fileSizeBytes, byte[] regionBytes) {
		final long byteOffset = sectorOffset * 4 * 1024L;
		assert (byteOffset < fileSizeBytes);

		final int actualLengthBytes = readInt((int) byteOffset, regionBytes);
		final int compressionByte = regionBytes[(int) (byteOffset + 4)];
                // Length includes +1 byte (compression byte). We already read compression byte and the actual chunk data is 1 byte smaller...
                final int chunkDataLengthBytes = actualLengthBytes - 1;

		assert (byteOffset + actualLengthBytes <= MAX_SIZE_BYTES);

		Compression compressionType;
		if (compressionByte == COMPRESSION_TYPE_GZIP)
			compressionType = Compression.Gzip;
		else if (compressionByte == COMPRESSION_TYPE_DEFLATE)
			compressionType = Compression.Deflate;
		else
			throw new UnknownCompressionTypeException("Unrecognised compression type:" + compressionByte);

		// Make a new byte array of the chunk data
		byte[] chunkData = new byte[chunkDataLengthBytes];     
                // +4 to skip chunk length, +1 to skip compression type
                System.arraycopy(regionBytes, (int)(byteOffset + 4 + 1), chunkData, 0, chunkDataLengthBytes);

		return new ChunkData(chunkData, compressionType);
	}

	private int readInt(final int position, byte[] regionBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(regionBytes, position, 4).order(ByteOrder.BIG_ENDIAN);
                return buffer.getInt();
        }
}
