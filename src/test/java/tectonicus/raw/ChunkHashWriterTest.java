/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * The writer's fast paths must feed the digest exactly the bytes of the plain encodings, or saved chunk hashes would
 * stop matching. Each test compares against a straightforward reference encoding.
 */
class ChunkHashWriterTest {
	@Test
	void intsAreLittleEndianAcrossBufferBoundaries() {
		Random random = new Random(1);
		int[] values = new int[20_000]; // more than one 16 KiB buffer
		for (int i = 0; i < values.length; i++)
			values[i] = random.nextInt();

		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		expected.write(7);
		for (int value : values)
			writeInt(expected, value);
		writeInt(expected, -42);

		RecordingDigest digest = new RecordingDigest();
		ChunkHashWriter writer = new ChunkHashWriter(digest);
		writer.update((byte) 7); // misalign the ints
		writer.updateInts(values);
		writer.updateInt(-42);
		assertArrayEquals(expected.toByteArray(), writer.digest());
	}

	@Test
	void repeatedStringsMatchOneCallPerCopy() {
		String[] names = {"minecraft:air", "minecraft:stone", "x".repeat(9000), "a"};
		int[] counts = {1, 2, 3, 8, 9, 100, 1234, 4096};
		for (String name : names) {
			for (int count : counts) {
				ByteArrayOutputStream expected = new ByteArrayOutputStream();
				expected.write(1);
				for (int i = 0; i < count; i++)
					expected.writeBytes(name.getBytes());

				RecordingDigest digest = new RecordingDigest();
				ChunkHashWriter writer = new ChunkHashWriter(digest);
				writer.update((byte) 1);
				writer.updateString(name, count);
				assertArrayEquals(expected.toByteArray(), writer.digest(), name.length() + " x " + count);
			}
		}
	}

	@Test
	void stringArraysSkipNullsAndKeepOrder() {
		String air = "minecraft:air";
		String plains = "minecraft:plains";
		String[] values = new String[5000];
		Random random = new Random(2);
		for (int i = 0; i < values.length; ) {
			String value = random.nextInt(10) == 0 ? null : random.nextBoolean() ? air : plains;
			for (int run = 1 + random.nextInt(50); run > 0 && i < values.length; run--)
				values[i++] = value;
		}

		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		for (String value : values)
			if (value != null)
				expected.writeBytes(value.getBytes());

		ChunkHashWriter writer = new ChunkHashWriter(new RecordingDigest());
		writer.updateStrings(values);
		assertArrayEquals(expected.toByteArray(), writer.digest());
	}

	@Test
	void blockStatesMatchTheReferenceEncoding() {
		BlockProperties empty = new BlockProperties(Map.of());
		BlockProperties stairs = new BlockProperties(Map.of("facing", "north", "half", "top"));
		BlockProperties equalStairs = new BlockProperties(Map.of("half", "top", "facing", "north"));
		BlockProperties water = new BlockProperties(Map.of("level", "0"));
		BlockProperties[] palette = {null, empty, stairs, equalStairs, water};

		Random random = new Random(3);
		BlockProperties[] states = new BlockProperties[4096];
		for (int i = 0; i < states.length; ) {
			BlockProperties state = palette[random.nextInt(palette.length)];
			for (int run = 1 + random.nextInt(40); run > 0 && i < states.length; run--)
				states[i++] = state;
		}
		byte[] marker = "blockStates-v1".getBytes(StandardCharsets.UTF_8);

		ChunkHashWriter writer = new ChunkHashWriter(new RecordingDigest());
		writer.updateBlockStates(states, marker);
		assertArrayEquals(referenceBlockStates(states, marker), writer.digest());

		// Nothing at all is written when no block has properties.
		BlockProperties[] plain = {null, empty, empty, null};
		ChunkHashWriter plainWriter = new ChunkHashWriter(new RecordingDigest());
		plainWriter.updateBlockStates(plain, marker);
		assertArrayEquals(new byte[0], plainWriter.digest());
	}

	/** The block state encoding, written out one value at a time. */
	private static byte[] referenceBlockStates(BlockProperties[] states, byte[] marker) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<Map<String, String>, Integer> palette = null;
		for (int index = 0; index < states.length; index++) {
			BlockProperties state = states[index];
			if (state == null || state.getProperties().isEmpty())
				continue;
			if (palette == null) {
				out.writeBytes(marker);
				palette = new HashMap<>();
			}
			Map<String, String> properties = state.getProperties();
			Integer id = palette.get(properties);
			boolean firstUse = id == null;
			if (firstUse) {
				id = palette.size();
				palette.put(properties, id);
			}
			writeInt(out, index);
			writeInt(out, id);
			if (firstUse) {
				writeInt(out, properties.size());
				new TreeMap<>(properties).forEach((key, value) -> {
					byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
					byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
					writeInt(out, keyBytes.length);
					out.writeBytes(keyBytes);
					writeInt(out, valueBytes.length);
					out.writeBytes(valueBytes);
				});
			}
		}
		if (palette != null)
			writeInt(out, -1);
		return out.toByteArray();
	}

	private static void writeInt(ByteArrayOutputStream out, int value) {
		out.write(value);
		out.write(value >> 8);
		out.write(value >> 16);
		out.write(value >> 24);
	}

	/** Returns every byte it was given as the "hash", so tests can compare the exact input. */
	private static final class RecordingDigest extends MessageDigest {
		private ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		RecordingDigest() {
			super("recording");
		}

		@Override
		protected void engineUpdate(byte input) {
			bytes.write(input);
		}

		@Override
		protected void engineUpdate(byte[] input, int offset, int length) {
			bytes.write(input, offset, length);
		}

		@Override
		protected byte[] engineDigest() {
			byte[] result = bytes.toByteArray();
			engineReset();
			return result;
		}

		@Override
		protected void engineReset() {
			bytes = new ByteArrayOutputStream();
		}
	}
}
