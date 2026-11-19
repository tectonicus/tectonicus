/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.EndTag;
import org.jnbt.FloatTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongArrayTag;
import org.jnbt.LongTag;
import org.jnbt.NBTUtils;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Bedrock Edition's on-disk NBT: the same tags as Java Edition, but with little-endian numbers and UTF-8
 * strings. Produces the same tag classes as the Java reader so the rest of Tectonicus can read both.
 */
public final class BedrockNbt {
	private static final int MAX_DEPTH = 512;

	private BedrockNbt() {}

	/** Reads one named root tag from the buffer's position, leaving the position after it. */
	public static Tag readTag(ByteBuffer buffer) throws IOException {
		ByteBuffer in = buffer.order(ByteOrder.LITTLE_ENDIAN);
		try {
			int type = in.get() & 0xFF;
			if (type == 0)
				return new EndTag();
			String name = readString(in);
			return readPayload(in, type, name, 0);
		} catch (BufferUnderflowException | IllegalArgumentException e) {
			throw new IOException("Truncated or invalid Bedrock NBT", e);
		}
	}

	/** Reads one compound from the start of the data. */
	public static CompoundTag readCompound(byte[] data) throws IOException {
		Tag tag = readTag(ByteBuffer.wrap(data));
		if (tag instanceof CompoundTag compound)
			return compound;
		throw new IOException("Bedrock NBT root is " + tag.getClass().getSimpleName() + ", not a compound");
	}

	/** Reads compounds stored back to back until the end of the data, as block entity and palette records are. */
	public static List<CompoundTag> readAll(byte[] data) throws IOException {
		return readAll(ByteBuffer.wrap(data));
	}

	public static List<CompoundTag> readAll(ByteBuffer buffer) throws IOException {
		List<CompoundTag> tags = new ArrayList<>();
		while (buffer.hasRemaining()) {
			Tag tag = readTag(buffer);
			if (tag instanceof CompoundTag compound)
				tags.add(compound);
			else if (tag instanceof EndTag)
				break;
		}
		return tags;
	}

	/** Reads {@code count} compounds stored back to back from the buffer's position. */
	public static List<CompoundTag> readCompounds(ByteBuffer buffer, int count) throws IOException {
		List<CompoundTag> tags = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Tag tag = readTag(buffer);
			if (!(tag instanceof CompoundTag compound))
				throw new IOException("Expected a compound in a Bedrock palette, found " + tag.getClass().getSimpleName());
			tags.add(compound);
		}
		return tags;
	}

	private static Tag readPayload(ByteBuffer in, int type, String name, int depth) throws IOException {
		if (depth > MAX_DEPTH)
			throw new IOException("Bedrock NBT nested too deeply");
		return switch (type) {
			case 1 -> new ByteTag(name, in.get());
			case 2 -> new ShortTag(name, in.getShort());
			case 3 -> new IntTag(name, in.getInt());
			case 4 -> new LongTag(name, in.getLong());
			case 5 -> new FloatTag(name, in.getFloat());
			case 6 -> new DoubleTag(name, in.getDouble());
			case 7 -> {
				byte[] bytes = new byte[length(in, 1)];
				in.get(bytes);
				yield new ByteArrayTag(name, bytes);
			}
			case 8 -> new StringTag(name, readString(in));
			case 9 -> {
				int elementType = in.get() & 0xFF;
				int count = in.getInt();
				if (count < 0)
					throw new IOException("Negative Bedrock NBT list length");
				List<Tag> items = new ArrayList<>(Math.min(count, 1024));
				for (int i = 0; i < count; i++)
					items.add(readPayload(in, elementType, "", depth + 1));
				Class<? extends Tag> elementClass = elementType == 0 ? EndTag.class : NBTUtils.getTypeClass(elementType);
				yield new ListTag(name, elementClass, items);
			}
			case 10 -> {
				Map<String, Tag> children = new HashMap<>();
				while (true) {
					int childType = in.get() & 0xFF;
					if (childType == 0)
						break;
					String childName = readString(in);
					children.put(childName, readPayload(in, childType, childName, depth + 1));
				}
				yield new CompoundTag(name, children);
			}
			case 11 -> {
				int[] ints = new int[length(in, 4)];
				for (int i = 0; i < ints.length; i++)
					ints[i] = in.getInt();
				yield new IntArrayTag(name, ints);
			}
			case 12 -> {
				long[] longs = new long[length(in, 8)];
				for (int i = 0; i < longs.length; i++)
					longs[i] = in.getLong();
				yield new LongArrayTag(name, longs);
			}
			default -> throw new IOException("Unknown Bedrock NBT tag type " + type);
		};
	}

	private static int length(ByteBuffer in, int elementSize) throws IOException {
		int length = in.getInt();
		if (length < 0 || (long) length * elementSize > in.remaining())
			throw new IOException("Invalid Bedrock NBT array length " + length);
		return length;
	}

	private static String readString(ByteBuffer in) throws IOException {
		int length = in.getShort() & 0xFFFF;
		if (length > in.remaining())
			throw new IOException("Invalid Bedrock NBT string length " + length);
		byte[] bytes = new byte[length];
		in.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
