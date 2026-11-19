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
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.bedrock.BedrockBlockMapper.JavaState;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Decodes one Bedrock Edition chunk from its LevelDB records into a {@link RawChunk} holding Java Edition block
 * states, biomes and signs. Bedrock saves no light, so sky light is estimated per column; block light is left dark.
 */
@Slf4j
public final class BedrockChunkReader {
	public static final byte TAG_DATA_3D = 0x2B;
	public static final byte TAG_VERSION = 0x2C;
	public static final byte TAG_DATA_2D = 0x2D;
	public static final byte TAG_SUB_CHUNK = 0x2F;
	public static final byte TAG_BLOCK_ENTITY = 0x31;
	public static final byte TAG_FINALIZED_STATE = 0x36;
	public static final byte TAG_VERSION_OLD = 0x76;

	private static final int SIZE = RawChunk.SECTION_SIZE;
	private static final String AIR = "minecraft:air";
	private static final BlockProperties NO_PROPERTIES = new BlockProperties();
	private static final Pattern FORMATTING = Pattern.compile("§.");
	private static final Set<Integer> reportedVersions = ConcurrentHashMap.newKeySet();

	/** Reads a LevelDB value by key, returning null when the key is absent. */
	@FunctionalInterface
	public interface Records {
		byte[] get(byte[] key) throws IOException;
	}

	private final Records records;
	private final int dimension;
	private final BedrockBlockMapper mapper;

	/** @param dimension 0 for the Overworld, 1 for the Nether, 2 for the End */
	public BedrockChunkReader(Records records, int dimension, BedrockBlockMapper mapper) {
		this.records = records;
		this.dimension = dimension;
		this.mapper = mapper;
	}

	/** The LevelDB key of a chunk record, with the sub-chunk index for {@link #TAG_SUB_CHUNK}. */
	public static byte[] key(int chunkX, int chunkZ, int dimension, byte tag, Integer subChunk) {
		int length = 9 + (dimension != 0 ? 4 : 0) + (subChunk != null ? 1 : 0);
		ByteBuffer key = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
		key.putInt(chunkX).putInt(chunkZ);
		if (dimension != 0)
			key.putInt(dimension);
		key.put(tag);
		if (subChunk != null)
			key.put((byte) (int) subChunk);
		return key.array();
	}

	private byte[] get(int chunkX, int chunkZ, byte tag) throws IOException {
		return records.get(key(chunkX, chunkZ, dimension, tag, null));
	}

	/** Whether the game has finished generating the chunk. Bedrock leaves partly generated chunks at the edges. */
	public boolean isFinished(int chunkX, int chunkZ) throws IOException {
		if (get(chunkX, chunkZ, TAG_VERSION) == null && get(chunkX, chunkZ, TAG_VERSION_OLD) == null)
			return false;
		byte[] state = get(chunkX, chunkZ, TAG_FINALIZED_STATE);
		return state == null || state.length < 4 || ByteBuffer.wrap(state).order(ByteOrder.LITTLE_ENDIAN).getInt() >= 2;
	}

	/**
	 * Reads a chunk.
	 *
	 * @param minSectionY  the dimension's lowest section, such as -4 in the 1.18+ Overworld
	 * @param sectionCount the number of sections in the dimension
	 */
	public RawChunk read(int chunkX, int chunkZ, int minSectionY, int sectionCount) throws IOException {
		RawChunk chunk = new RawChunk(chunkX, chunkZ, minSectionY);
		sectionCount = Math.min(sectionCount, chunk.getSectionCount());

		for (int section = 0; section < sectionCount; section++) {
			byte[] data = records.get(key(chunkX, chunkZ, dimension, TAG_SUB_CHUNK, minSectionY + section));
			if (data == null)
				continue;
			try {
				readSubChunk(chunk, section, data);
			} catch (IOException | RuntimeException e) {
				log.warn("Skipping unreadable sub-chunk {} of Bedrock chunk {},{}", minSectionY + section, chunkX, chunkZ, e);
			}
		}

		readBiomes(chunk, chunkX, chunkZ, sectionCount);
		estimateSkyLight(chunk, sectionCount);
		readBlockEntities(chunk, chunkX, chunkZ);
		return chunk;
	}

	private void readSubChunk(RawChunk chunk, int section, byte[] data) throws IOException {
		ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		int version = in.get() & 0xFF;
		int layers;
		switch (version) {
			case 1 -> layers = 1;
			case 8 -> layers = in.get() & 0xFF;
			case 9 -> {
				layers = in.get() & 0xFF;
				in.get(); // The sub-chunk's own Y index, already known from its key
			}
			default -> {
				if (reportedVersions.add(version))
					log.warn("Bedrock sub-chunk format {} (from before 1.2.13) is not supported; those sub-chunks are skipped", version);
				return;
			}
		}
		if (layers == 0)
			return;

		JavaState[] blocks = readLayer(in);
		JavaState[] liquids = layers > 1 ? readLayer(in) : null;

		String[] names = new String[SIZE];
		BlockProperties[] states = new BlockProperties[SIZE];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 16; y++) {
					int bedrockIndex = (x << 8) | (z << 4) | y;
					JavaState block = blocks[bedrockIndex];
					if (liquids != null) {
						JavaState liquid = liquids[bedrockIndex];
						if (liquid != null && isWater(liquid.name())) {
							if (block.waterloggable())
								block = block.withProperty("waterlogged", "true");
							else if (AIR.equals(block.name()))
								block = liquid;
						}
					}
					int index = RawChunk.sectionIndex(x, y, z);
					names[index] = block.name();
					states[index] = block.properties();
				}
			}
		}
		chunk.setSection(section, names, states);
	}

	private static boolean isWater(String name) {
		return name.equals("minecraft:water") || name.equals("minecraft:flowing_water");
	}

	/** Reads one block storage and returns the Java state of every block, in Bedrock's x, z, y order. */
	private JavaState[] readLayer(ByteBuffer in) throws IOException {
		try {
			int header = in.get() & 0xFF;
			int bits = header >> 1;
			if ((header & 1) != 0)
				throw new IOException("Sub-chunk uses runtime block ids, which are only used on the network");
			int[] indices = readIndices(in, bits);
			int paletteSize = bits == 0 ? 1 : in.getInt();
			if (paletteSize <= 0 || paletteSize > SIZE)
				throw new IOException("Invalid sub-chunk palette size " + paletteSize);
			List<CompoundTag> palette = BedrockNbt.readCompounds(in, paletteSize);
			JavaState[] states = new JavaState[paletteSize];
			for (int i = 0; i < paletteSize; i++)
				states[i] = mapper.map(palette.get(i));
			JavaState[] blocks = new JavaState[SIZE];
			for (int i = 0; i < SIZE; i++) {
				int index = indices == null ? 0 : indices[i];
				blocks[i] = index < states.length ? states[index] : states[0];
			}
			return blocks;
		} catch (BufferUnderflowException e) {
			throw new IOException("Truncated sub-chunk", e);
		}
	}

	/** Reads packed palette indices. Indices never span two 32-bit words. Returns null when every index is 0. */
	static int[] readIndices(ByteBuffer in, int bits) throws IOException {
		if (bits == 0)
			return null;
		if (bits > 16)
			throw new IOException("Invalid bits per block " + bits);
		int perWord = 32 / bits;
		int words = (SIZE + perWord - 1) / perWord;
		int mask = (1 << bits) - 1;
		int[] indices = new int[SIZE];
		int i = 0;
		for (int w = 0; w < words; w++) {
			int word = in.getInt();
			for (int j = 0; j < perWord && i < SIZE; j++, i++)
				indices[i] = (word >>> (j * bits)) & mask;
		}
		return indices;
	}

	private void readBiomes(RawChunk chunk, int chunkX, int chunkZ, int sectionCount) throws IOException {
		byte[] data3d = get(chunkX, chunkZ, TAG_DATA_3D);
		if (data3d != null) {
			try {
				readBiomes3d(chunk, data3d, sectionCount);
				return;
			} catch (IOException | RuntimeException e) {
				log.warn("Unreadable biomes in Bedrock chunk {},{}", chunkX, chunkZ, e);
			}
		}
		byte[] data2d = get(chunkX, chunkZ, TAG_DATA_2D);
		if (data2d != null && data2d.length >= 768) {
			String[] column = new String[256];
			for (int i = 0; i < 256; i++)
				column[i] = BedrockBiomes.javaId(data2d[512 + i] & 0xFF);
			String[] biomes = new String[SIZE];
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					for (int y = 0; y < 16; y++)
						biomes[biomeIndex(x, y, z)] = column[(x << 4) | z];
			for (int section = 0; section < sectionCount; section++)
				chunk.setSectionBiomes(section, biomes);
		}
	}

	private static void readBiomes3d(RawChunk chunk, byte[] data, int sectionCount) throws IOException {
		ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		in.position(512); // Height map
		String[] previous = null;
		for (int section = 0; section < sectionCount && in.hasRemaining(); section++) {
			int header = in.get() & 0xFF;
			int bits = header >> 1;
			if (bits == 127) { // Same biomes as the section below
				if (previous != null)
					chunk.setSectionBiomes(section, previous);
				continue;
			}
			int[] indices = readIndices(in, bits);
			int paletteSize = bits == 0 ? 1 : in.getInt();
			if (paletteSize <= 0 || paletteSize > SIZE)
				throw new IOException("Invalid biome palette size " + paletteSize);
			String[] palette = new String[paletteSize];
			for (int i = 0; i < paletteSize; i++)
				palette[i] = BedrockBiomes.javaId(in.getInt());
			String[] biomes = new String[SIZE];
			if (indices == null) {
				Arrays.fill(biomes, palette[0]);
			} else {
				for (int x = 0; x < 16; x++)
					for (int z = 0; z < 16; z++)
						for (int y = 0; y < 16; y++) {
							int index = indices[(x << 8) | (z << 4) | y];
							biomes[biomeIndex(x, y, z)] = palette[index < paletteSize ? index : 0];
						}
			}
			chunk.setSectionBiomes(section, biomes);
			previous = biomes;
		}
	}

	private static int biomeIndex(int x, int y, int z) {
		return (y * 16 + z) * 16 + x;
	}

	/**
	 * Bedrock saves no light. Until Tectonicus computes it, give each column full sky light down to the first block
	 * that stops light, dimming it through water and leaves, so the Day light style looks right outdoors.
	 */
	private static void estimateSkyLight(RawChunk chunk, int sectionCount) {
		int height = sectionCount * 16;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int light = 15;
				for (int y = height - 1; y >= 0 && light > 0; y--) {
					chunk.setSkyLight(x, y, z, (byte) light);
					String name = chunk.getBlockName(x, y, z);
					if (name == null || name.equals(AIR) || name.equals("minecraft:cave_air") || name.equals("minecraft:void_air"))
						continue;
					light -= SkyLight.opacity(name, chunk.getBlockState(x, y, z));
				}
			}
		}
	}

	private void readBlockEntities(RawChunk chunk, int chunkX, int chunkZ) throws IOException {
		byte[] data = get(chunkX, chunkZ, TAG_BLOCK_ENTITY);
		if (data == null)
			return;
		List<CompoundTag> entities;
		try {
			entities = BedrockNbt.readAll(data);
		} catch (IOException e) {
			log.warn("Unreadable block entities in Bedrock chunk {},{}", chunkX, chunkZ, e);
			return;
		}
		int minY = chunk.getWorldY(0);
		for (CompoundTag entity : entities) {
			String id = string(entity, "id", "");
			Integer x = integer(entity, "x");
			Integer y = integer(entity, "y");
			Integer z = integer(entity, "z");
			if (x == null || y == null || z == null)
				continue;
			switch (id) {
				case "Sign", "HangingSign" -> {
					CompoundTag frontTag = compound(entity, "FrontText");
					CompoundTag backTag = compound(entity, "BackText");
					SignEntity.Face front = signFace(frontTag != null ? frontTag : entity);
					SignEntity.Face back = backTag != null ? signFace(backTag) : null;
					chunk.addSign(x, y, z, front, back);
				}
				case "Chest" -> pairChest(chunk, entity, x - chunkX * 16, y - minY, z - chunkZ * 16);
				default -> {
					// Other block entities come in a later phase
				}
			}
		}
	}

	private static void pairChest(RawChunk chunk, CompoundTag entity, int x, int y, int z) {
		Integer pairX = integer(entity, "pairx");
		Integer pairZ = integer(entity, "pairz");
		if (pairX == null || pairZ == null || x < 0 || x >= 16 || z < 0 || z >= 16 || y < 0)
			return;
		BlockProperties state = chunk.getBlockState(x, y, z);
		if (state == null || !"single".equals(state.get("type")) || state.get("facing") == null)
			return;
		int dx = pairX - (entity.getValue().get("x") instanceof IntTag t ? t.getValue() : 0);
		int dz = pairZ - (entity.getValue().get("z") instanceof IntTag t ? t.getValue() : 0);
		// Java calls a half "left" when its partner is clockwise from the way the chest faces.
		int[] clockwise = switch (state.get("facing")) {
			case "north" -> new int[] {1, 0};
			case "east" -> new int[] {0, 1};
			case "south" -> new int[] {-1, 0};
			default -> new int[] {0, -1};
		};
		String type;
		if (dx == clockwise[0] && dz == clockwise[1])
			type = "left";
		else if (dx == -clockwise[0] && dz == -clockwise[1])
			type = "right";
		else
			return;
		var props = new java.util.TreeMap<>(state.getProperties());
		props.put("type", type);
		chunk.setBlockState(x, y, z, new BlockProperties(props));
	}

	private static SignEntity.Face signFace(CompoundTag text) {
		String raw = FORMATTING.matcher(string(text, "Text", "")).replaceAll("");
		List<String> lines = new ArrayList<>(Arrays.asList(raw.split("\n", -1)));
		while (lines.size() < 4)
			lines.add("");
		lines = List.copyOf(lines.subList(0, 4));
		Integer color = integer(text, "SignTextColor");
		boolean glowing = text.getValue().get("IgnoreLighting") instanceof ByteTag b && b.getValue() != 0;
		return new SignEntity.Face(lines, SignColors.dyeName(color == null ? 0xFF000000 : color), glowing,
				List.of(List.of(), List.of(), List.of(), List.of()));
	}

	private static String string(CompoundTag tag, String name, String fallback) {
		Tag child = tag.getValue().get(name);
		return child instanceof StringTag s ? s.getValue() : fallback;
	}

	private static Integer integer(CompoundTag tag, String name) {
		Tag child = tag.getValue().get(name);
		return child instanceof IntTag i ? i.getValue() : null;
	}

	private static CompoundTag compound(CompoundTag tag, String name) {
		Tag child = tag.getValue().get(name);
		return child instanceof CompoundTag c ? c : null;
	}
}
