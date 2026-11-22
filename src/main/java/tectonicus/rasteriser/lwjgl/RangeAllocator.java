/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser.lwjgl;

import java.util.Map;
import java.util.TreeMap;

/**
 * Hands out byte ranges of a fixed-size block, first fit, and merges freed ranges with free neighbours so
 * the block does not fragment into pieces too small to reuse. Not thread-safe.
 */
final class RangeAllocator {
	private final long size;
	// Free ranges by start offset, mapped to their length. Neighbouring free ranges are always merged.
	private final TreeMap<Long, Long> free = new TreeMap<>();
	private long usedBytes;

	RangeAllocator(long size) {
		if (size <= 0)
			throw new IllegalArgumentException("size must be positive");
		this.size = size;
		free.put(0L, size);
	}

	/** @return the start of a free range of {@code bytes}, or -1 if no free range is large enough */
	long allocate(long bytes) {
		if (bytes <= 0)
			throw new IllegalArgumentException("bytes must be positive");
		for (Map.Entry<Long, Long> range : free.entrySet()) {
			if (range.getValue() >= bytes) {
				long offset = range.getKey();
				long remaining = range.getValue() - bytes;
				free.remove(offset);
				if (remaining > 0)
					free.put(offset + bytes, remaining);
				usedBytes += bytes;
				return offset;
			}
		}
		return -1;
	}

	/** Returns a range given out by {@link #allocate(long)}. */
	void free(long offset, long bytes) {
		if (offset < 0 || bytes <= 0 || offset + bytes > size)
			throw new IllegalArgumentException("range " + offset + "+" + bytes + " is outside the block");
		long start = offset;
		long length = bytes;
		Map.Entry<Long, Long> previous = free.floorEntry(offset);
		Map.Entry<Long, Long> following = free.ceilingEntry(offset);
		if ((previous != null && previous.getKey() + previous.getValue() > offset)
				|| (following != null && following.getKey() < offset + bytes))
			throw new IllegalStateException("range " + offset + "+" + bytes + " is already free");
		if (previous != null && previous.getKey() + previous.getValue() == offset) {
			start = previous.getKey();
			length += previous.getValue();
			free.remove(start);
		}
		Long next = free.remove(offset + bytes);
		if (next != null)
			length += next;
		free.put(start, length);
		usedBytes -= bytes;
	}

	long size() {
		return size;
	}

	long usedBytes() {
		return usedBytes;
	}

	int freeRangeCount() {
		return free.size();
	}
}
