/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Feeds chunk data to a {@link MessageDigest} for the chunk hash.
 * <p>
 * The serialized bytes are independent of the digest algorithm. The writer batches updates and shares
 * the byte encoding of repeated block names.
 * Not thread safe; use one writer per hash.
 */
final class ChunkHashWriter {
	private static final int BUFFER_SIZE = 16 * 1024;

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
		buffer[position++] = (byte) value;
		buffer[position++] = (byte) (value >> 8);
		buffer[position++] = (byte) (value >> 16);
		buffer[position++] = (byte) (value >> 24);
	}

	void updateInts(int[] values) {
		for (int value : values)
			updateInt(value);
	}

	/** Each non-null string in the platform's default charset, without separators. */
	void updateStrings(String[] values) {
		for (String value : values) {
			if (value != null)
				update(encode(value));
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
		for (int index = 0; index < states.length; index++) {
			BlockProperties state = states[index];
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
