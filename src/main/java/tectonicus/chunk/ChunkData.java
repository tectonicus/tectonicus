/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.chunk;

import lombok.Getter;
import org.tectonicus.jnbt.NBTCompression;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * The compressed NBT of one chunk: either a whole array, or a range of a larger one such as a region file's bytes,
 * so it can be read without copying. The array is shared, not copied, and must not change while this is in use.
 */
public class ChunkData {
	private final byte[] bytes;
	private final int offset;
	@Getter
	private final int length;
	@Getter
	private final NBTCompression compressionType;

	public ChunkData(byte[] bytes, NBTCompression compressionType) {
		this(bytes, 0, bytes.length, compressionType);
	}

	/**
	 * @throws IndexOutOfBoundsException if the range does not lie within {@code bytes}
	 */
	public ChunkData(byte[] bytes, int offset, int length, NBTCompression compressionType) {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
		this.compressionType = compressionType;
	}

	/** A stream over this chunk's compressed bytes only. */
	public InputStream openStream() {
		return new ByteArrayInputStream(bytes, offset, length);
	}
}
