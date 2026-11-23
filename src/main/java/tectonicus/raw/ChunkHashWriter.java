/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Feeds chunk data to a {@link MessageDigest} for the chunk hash.
 * <p>
 * The serialized bytes are independent of the digest algorithm. Saved chunk hashes depend on them, so any change here
 * must keep writing exactly the same bytes; {@code ChunkHashWriterTest} checks this.
 * The writer batches updates and shares the byte encoding of repeated block names.
 * Not thread safe; use one writer per hash.
 */
final class ChunkHashWriter {
	private static final int BUFFER_SIZE = 16 * 1024;
	/** Writes an int into a byte array as four bytes, least significant first. */
	private static final VarHandle INT_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

	private final MessageDigest digest;
	private final byte[] buffer = new byte[BUFFER_SIZE];
	private int position;

	// Block names in a section share String instances (one per palette entry), so look them up by identity.
	private final Map<String, byte[]> encodedStrings = new IdentityHashMap<>();
	private String lastString;
	private byte[] lastEncoded;

	ChunkHashWriter(MessageDigest digest) {
		this.digest = digest;
		digest.reset();
	}

	void update(byte value) {
		if (position == BUFFER_SIZE)
			flush();
		buffer[position++] = value;
	}

	void update(byte[] bytes) {
		if (bytes.length > BUFFER_SIZE - position) {
			flush();
			if (bytes.length > BUFFER_SIZE / 2) {
				digest.update(bytes);
				return;
			}
		}
		System.arraycopy(bytes, 0, buffer, position, bytes.length);
		position += bytes.length;
	}

	/** Four bytes, least significant first. */
	void updateInt(int value) {
		if (BUFFER_SIZE - position < Integer.BYTES)
			flush();
		INT_LE.set(buffer, position, value);
		position += Integer.BYTES;
	}

	/** Each value as {@link #updateInt(int)} would write it. */
	void updateInts(int[] values) {
		int index = 0;
		while (index < values.length) {
			if (BUFFER_SIZE - position < Integer.BYTES)
				flush();
			// Copy as many ints as fit before the buffer is full, without checking the space for each one. Counting in
			// a local rather than the field is measurably faster.
			int end = index + Math.min(values.length - index, (BUFFER_SIZE - position) / Integer.BYTES);
			int at = position;
			for (; index < end; index++) {
				INT_LE.set(buffer, at, values[index]);
				at += Integer.BYTES;
			}
			position = at;
		}
	}

	/** Each non-null string in the platform's default charset, without separators. */
	void updateStrings(String[] values) {
		updateStrings(values, value -> value);
	}

	/**
	 * The name of each non-null value, as {@link #updateString(String)} would write it. Neighbouring values are usually
	 * the same object (one per palette entry), so each run of the same object is written in one call.
	 */
	<T> void updateStrings(T[] values, Function<T, String> name) {
		int index = 0;
		while (index < values.length) {
			T value = values[index];
			int runEnd = index + 1;
			while (runEnd < values.length && values[runEnd] == value)
				runEnd++;
			if (value != null)
				updateString(name.apply(value), runEnd - index);
			index = runEnd;
		}
	}

	void updateString(String value) {
		update(encode(value));
	}

	/** Writes {@code value} {@code count} times in a row; the same bytes as calling {@link #updateString(String)} that often. */
	void updateString(String value, int count) {
		byte[] bytes = encode(value);
		if (bytes.length == 0 || bytes.length > BUFFER_SIZE / 2) {
			for (int i = 0; i < count; i++)
				update(bytes);
			return;
		}
		while (count > 0) {
			if (BUFFER_SIZE - position < bytes.length)
				flush();
			// Write one copy, then copy everything written so far again, doubling it each time until the buffer is
			// full. A section of 4,096 identical blocks takes about a dozen array copies instead of 4,096.
			int start = position;
			System.arraycopy(bytes, 0, buffer, position, bytes.length);
			position += bytes.length;
			count--;
			int copiesWritten = 1;
			while (count > 0) {
				int copies = Math.min(Math.min(copiesWritten, count), (BUFFER_SIZE - position) / bytes.length);
				if (copies == 0)
					break; // The buffer is full: flush it and start again.
				System.arraycopy(buffer, start, buffer, position, copies * bytes.length);
				position += copies * bytes.length;
				count -= copies;
				copiesWritten += copies;
			}
		}
	}

	/**
	 * Block states by position: each distinct set of properties gets a number on first use, followed by its
	 * sorted keys and values. Blocks without properties are skipped; nothing is written if no block has any.
	 */
	void updateBlockStates(BlockProperties[] states, byte[] marker) {
		Map<Map<String, String>, Integer> palette = null;
		// Blocks from the same palette entry share one properties map, so most lookups hit this first.
		Map<Map<String, String>, Integer> paletteByIdentity = null;
		// Neighbouring blocks are usually the same palette entry; reuse its id without looking it up again.
		BlockProperties lastState = null;
		int lastId = 0;
		for (int index = 0; index < states.length; index++) {
			BlockProperties state = states[index];
			if (state != null && state == lastState) {
				updateInt(index);
				updateInt(lastId);
				continue;
			}
			if (state == null || state.getProperties().isEmpty())
				continue;
			if (palette == null) {
				update(marker);
				palette = new HashMap<>();
				paletteByIdentity = new IdentityHashMap<>();
			}

			Map<String, String> properties = state.getProperties();
			Integer id = paletteByIdentity.get(properties);
			boolean firstUse = false;
			if (id == null) {
				int nextId = palette.size();
				Integer existingId = palette.putIfAbsent(properties, nextId);
				firstUse = existingId == null;
				id = firstUse ? nextId : existingId;
				paletteByIdentity.put(properties, id);
			}
			updateInt(index);
			updateInt(id);
			// Emit each distinct state on its first occurrence, in key order.
			if (firstUse) {
				updateInt(properties.size());
				new TreeMap<>(properties).forEach((key, value) -> {
					updateLengthPrefixed(key);
					updateLengthPrefixed(value);
				});
			}
			lastState = state;
			lastId = id;
		}
		if (palette != null)
			updateInt(-1);
	}

	private void updateLengthPrefixed(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		updateInt(bytes.length);
		update(bytes);
	}

	private byte[] encode(String value) {
		if (value != lastString) {
			lastString = value;
			lastEncoded = encodedStrings.computeIfAbsent(value, String::getBytes);
		}
		return lastEncoded;
	}

	byte[] digest() {
		flush();
		return digest.digest();
	}

	private void flush() {
		if (position > 0) {
			digest.update(buffer, 0, position);
			position = 0;
		}
	}
}
