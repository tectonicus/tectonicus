/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.Getter;
import org.jnbt.NBTInputStream.Compression;
import tectonicus.cache.BiomeCache;
import tectonicus.world.filter.BlockFilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

	@Getter
	private final RegionCoord regionCoord;

	@Getter
	private final File regionFile;
	private final byte[] bytes;

	private final ChunkInfo[] info;

	public Region(File regionFile) throws Exception {
		this.regionFile = regionFile;

		this.regionCoord = extractRegionCoord(regionFile);
		if (regionCoord == null)
			throw new Exception("Couldn't extract region coord from " + regionFile.getName());

		actualFileSizeBytes = regionFile.length();

		info = new ChunkInfo[RegionCoord.REGION_WIDTH * RegionCoord.REGION_HEIGHT];
		for (int i = 0; i < info.length; i++)
			info[i] = new ChunkInfo();

		try (RandomAccessFile file = new RandomAccessFile(regionFile, "r")) {
			ByteBuffer buffer = ByteBuffer.allocate(1024 * 4);

			// Read chunk locations
			if (!read(file, buffer))
				throw new Exception("Failed to read chunk locations");

			buffer.rewind();

			// Parse chunk locations
			for (ChunkInfo chunkInfo : info) {
				final int val = buffer.getInt();
				final int offset = ((val >> 8) & 0xFFFFFF);
				final int numSectors = val & 0xFF;

				assert (offset < MAX_SECTORS);

				chunkInfo.sectorOffset = offset;
				chunkInfo.numSectors = numSectors;
			}

			bytes = new byte[(int) file.length()];
			file.seek(0);
			read(file, bytes);
		}
	}

	public boolean containsChunk(ChunkCoord chunkCoord) {
		// First check to see if the chunk would be contained within this region
		RegionCoord actualRegion = RegionCoord.fromChunkCoord(chunkCoord);
		if (!actualRegion.equals(this.regionCoord))
			return false;

		// Now check to see if it actually exists
		final int header = getHeaderOffsetForChunk(chunkCoord);
		return info[header].sectorOffset != 0 && info[header].numSectors != 0;
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
				if (info[header].sectorOffset != 0 && info[header].numSectors != 0) {
					result.add(new ChunkCoord(chunkX, chunkZ));
				}
			}
		}

		int count = 0;
		for (ChunkInfo chunkInfo : info) {
			if (chunkInfo.sectorOffset != 0 && chunkInfo.numSectors != 0) {
				count++;
			}
		}
		if (count != result.size()) {
			System.out.println("Mismatch!");
		}

		return result.toArray(new ChunkCoord[result.size()]);
	}

	private static int getHeaderOffsetForChunk(final ChunkCoord coord) {
		return getHeaderOffsetForChunk(coord.x, coord.z);
	}

	private static int getHeaderOffsetForChunk(final long chunkX, final long chunkZ) {
		// 4 * ((x mod 32) + (z mod 32) * 32
		final int offset = (int) ((chunkX & 31) + (chunkZ & 31) * 32);
		assert (offset >= 0);
		assert (offset < 1024);
		return offset;
	}

	private int getSectorOffsetForChunk(final ChunkCoord coord) {
		final int header = getHeaderOffsetForChunk(coord);
		return info[header].sectorOffset;
	}

	private static boolean read(RandomAccessFile file, ByteBuffer buffer) throws IOException {
		int res;
		do {
			res = file.getChannel().read(buffer);
		}
		while (res != -1 && buffer.hasRemaining());
		return res != -1;
	}

	private static boolean read(RandomAccessFile file, byte[] dest) throws IOException {
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

		return res != -1;
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
			e.printStackTrace();
		}

		return coord;
	}

	private static class ChunkInfo {
		/**
		 * Position of chunk from beginning of file, in sectors
		 */
		public int sectorOffset;

		/**
		 * Length of chunk, in sectors
		 */
		public int numSectors;
	}

	public Chunk loadChunk(ChunkCoord chunkCoord, BiomeCache biomeCache, BlockFilter filter, WorldStats worldStats) {
		if (!containsChunk(chunkCoord))
			return null;

		final int sector = getSectorOffsetForChunk(chunkCoord);
		assert (sector >= 2); // First two sectors are the header info

		final long byteOffset = sector * 4 * 1024L;
		assert (byteOffset < actualFileSizeBytes);

		final int actualLengthBytes2 = readInt((int) byteOffset);
		final int compressionType2 = bytes[(int) (byteOffset + 4)];

		assert (byteOffset + actualLengthBytes2 <= MAX_SIZE_BYTES);

		Compression compression;
		if (compressionType2 == COMPRESSION_TYPE_GZIP)
			compression = Compression.Gzip;
		else if (compressionType2 == COMPRESSION_TYPE_DEFLATE)
			compression = Compression.Deflate;
		else
			throw new RuntimeException("Unrecognised compression type:" + compressionType2);

		// Make a new byte array of the chunk data
		byte[] chunkData = new byte[actualLengthBytes2];
		for (int i = 0; i < chunkData.length; i++) {
			chunkData[i] = bytes[(int) (byteOffset + i + 4 + 1)]; // +4 to skip chunk length, +1 to skip compression type
		}

		Chunk chunk = null;
		try (InputStream in = new ByteArrayInputStream(chunkData, 0, chunkData.length);) {
			chunk = new Chunk(chunkCoord, biomeCache);
			chunk.loadRaw(in, compression, filter, worldStats);
		} catch (Exception e) {
			System.err.println("Error while trying to load chunk at (" + chunkCoord.x + ", " + chunkCoord.z + ") from region " + regionFile.getAbsolutePath());
			e.printStackTrace();
		}

		return chunk;
	}

	private int readInt(final int position) {
		final byte b0 = bytes[position];
		final byte b1 = bytes[position + 1];
		final byte b2 = bytes[position + 2];
		final byte b3 = bytes[position + 3];

		return ((b0 & 0xFF) << 24)
				| ((b1 & 0xFF) << 16)
				| ((b2 & 0xFF) << 8)
				| (b3 & 0xFF);
	}
}
