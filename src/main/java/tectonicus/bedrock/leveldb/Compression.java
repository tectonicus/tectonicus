/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Block compression types found in LevelDB tables (including Mojang's leveldb-mcpe additions). */
final class Compression {
	static final int NONE = 0;
	/** Snappy, the compression of standard LevelDB; Minecraft never writes it, so it is not supported. */
	static final int SNAPPY = 1;
	/** zlib stream with header (Mojang leveldb-mcpe {@code kZlibCompression}). */
	static final int ZLIB = 2;
	/** zstd (used by some leveldb forks; not supported). */
	static final int ZSTD = 3;
	/** raw deflate without zlib header (Mojang leveldb-mcpe {@code kZlibRawCompression}). */
	static final int ZLIB_RAW = 4;

	private Compression() {
	}

	static byte[] decompress(int type, byte[] data, int off, int len) throws LevelDbException {
		return switch (type) {
			case NONE -> {
				byte[] out = new byte[len];
				System.arraycopy(data, off, out, 0, len);
				yield out;
			}
			case SNAPPY -> throw new LevelDbException("Unsupported block compression: snappy (1)");
			case ZLIB -> inflate(data, off, len, false);
			case ZLIB_RAW -> inflate(data, off, len, true);
			case ZSTD -> throw new LevelDbException("Unsupported block compression: zstd (3)");
			default -> throw new LevelDbException("Unknown block compression type " + type);
		};
	}

	private static byte[] inflate(byte[] data, int off, int len, boolean nowrap) throws LevelDbException {
		Inflater inflater = new Inflater(nowrap);
		try {
			inflater.setInput(data, off, len);
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, len * 4));
			byte[] buf = new byte[Math.max(4096, len * 4)];
			while (!inflater.finished()) {
				int n = inflater.inflate(buf);
				if (n > 0) {
					out.write(buf, 0, n);
				} else if (inflater.needsInput()) {
					// Raw deflate streams may end without a final block marker being consumed; accept what we have
					if (nowrap) {
						break;
					}
					throw new LevelDbException("zlib: truncated compressed block");
				} else if (inflater.needsDictionary()) {
					throw new LevelDbException("zlib: compressed block requires a preset dictionary");
				}
			}
			return out.toByteArray();
		} catch (DataFormatException e) {
			throw new LevelDbException("Corrupt " + (nowrap ? "raw deflate" : "zlib") + " block", e);
		} finally {
			inflater.end();
		}
	}
}
