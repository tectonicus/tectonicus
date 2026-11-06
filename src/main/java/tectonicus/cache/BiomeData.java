/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import biomeextractor.ByteCoord;
import biomeextractor.WorldProcessor;
import org.jnbt.*;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.NbtUtil;
import tectonicus.raw.RawChunk;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Immutable color-map coordinates for one chunk, serialized independently of textures. */
public class BiomeData {
	private static final int VERSION = 3;
	private static final int COORDINATE_BYTES = RawChunk.WIDTH * RawChunk.DEPTH * 2;
	private final byte[] swamps = new byte[RawChunk.WIDTH * RawChunk.DEPTH];
	private final ColourCoord[][] colourCoords = new ColourCoord[RawChunk.WIDTH][RawChunk.DEPTH];

	public BiomeData() {
		for (int x = 0; x < RawChunk.WIDTH; x++)
			for (int z = 0; z < RawChunk.DEPTH; z++)
				colourCoords[x][z] = new ColourCoord((byte)0, (byte)0);
	}

	public BiomeData(WorldProcessor processor, ChunkCoord chunk) {
		int baseX = Math.toIntExact(Math.multiplyExact(chunk.x, RawChunk.WIDTH));
		int baseZ = Math.toIntExact(Math.multiplyExact(chunk.z, RawChunk.DEPTH));
		try {
			boolean[] mask = processor.getSwampMaskAtBlocks(baseX, baseZ, RawChunk.WIDTH, RawChunk.DEPTH);
			for (int i = 0; i < mask.length; i++) swamps[i] = (byte)(mask[i] ? 1 : 0);
			ByteCoord[] batch = processor.getCoordsAtBlocks(null, null, null, baseX, baseZ, RawChunk.WIDTH, RawChunk.DEPTH);
			for (int z = 0; z < RawChunk.DEPTH; z++) {
				for (int x = 0; x < RawChunk.WIDTH; x++) {
					ByteCoord coordinate = batch[x + z * RawChunk.WIDTH];
					colourCoords[x][z] = new ColourCoord(coordinate.x, coordinate.y);
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Could not extract biome colors for chunk " + chunk, e);
		}
	}

	public BiomeData(File file) throws IOException {
		try (NBTInputStream input = new NBTInputStream(Files.newInputStream(file.toPath()))) {
			if (!(input.readTag() instanceof CompoundTag root) || NbtUtil.getInt(root, "version", 0) != VERSION)
				throw new IOException("Wrong biome cache version: " + file);
			if (!(root.getValue().get("colourCoords") instanceof ByteArrayTag tag) || tag.getValue().length != COORDINATE_BYTES)
				throw new IOException("Invalid biome coordinate array: " + file);
			if (!(root.getValue().get("swamps") instanceof ByteArrayTag mask) || mask.getValue().length != swamps.length)
				throw new IOException("Invalid swamp mask: " + file);
			System.arraycopy(mask.getValue(), 0, swamps, 0, swamps.length);
			byte[] coordinates = tag.getValue();
			int position = 0;
			for (int x = 0; x < RawChunk.WIDTH; x++) {
				for (int z = 0; z < RawChunk.DEPTH; z++) {
					colourCoords[x][z] = new ColourCoord(coordinates[position++], coordinates[position++]);
				}
			}
		}
	}

	public boolean isSwamp(int x, int z) { return swamps[x + z * RawChunk.WIDTH] != 0; }

	public ColourCoord getColourCoord(int x, int z) {
		return colourCoords[x][z];
	}

	public void write(File file) throws IOException {
		Files.createDirectories(file.toPath().toAbsolutePath().getParent());
		byte[] coordinates = new byte[COORDINATE_BYTES];
		int position = 0;
		for (int x = 0; x < RawChunk.WIDTH; x++) {
			for (int z = 0; z < RawChunk.DEPTH; z++) {
				coordinates[position++] = colourCoords[x][z].x();
				coordinates[position++] = colourCoords[x][z].y();
			}
		}
		Map<String, Tag> tags = new HashMap<>();
		tags.put("swamps", new ByteArrayTag("swamps", swamps));
		tags.put("version", new IntTag("version", VERSION));
		tags.put("colourCoords", new ByteArrayTag("colourCoords", coordinates));
		try (NBTOutputStream output = new NBTOutputStream(Files.newOutputStream(file.toPath()))) {
			output.writeTag(new CompoundTag("data", tags));
		}
	}

	public record ColourCoord(byte x, byte y) {
		public int getX() { return x & 0xff; }
		public int getY() { return y & 0xff; }
	}
}
