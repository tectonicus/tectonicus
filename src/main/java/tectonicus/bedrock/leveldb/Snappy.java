/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

/**
 * Decompressor for the raw (unframed) Snappy format as used by LevelDB table blocks.
 * <p>
 * Format: a varint uncompressed length followed by elements. The low two bits of each tag byte select
 * a literal (0) or a copy with a 1, 2 or 4 byte offset (1, 2, 3). Copies may overlap their output.
 */
final class Snappy {
	private Snappy() {
	}

	static byte[] decompress(byte[] in, int offset, int length) throws LevelDbException {
		SliceInput header = new SliceInput(in, offset, length);
		long expected = header.readVarint64();
		if (expected > Integer.MAX_VALUE - 8) {
			throw new LevelDbException("Snappy: uncompressed length too large: " + expected);
		}
		byte[] out = new byte[(int) expected];
		int ip = header.position();
		int ipLimit = offset + length;
		int op = 0;

		while (ip < ipLimit) {
			int tag = in[ip++] & 0xff;
			int kind = tag & 3;
			if (kind == 0) {
				int len = tag >>> 2;
				if (len >= 60) {
					int extra = len - 59;
					if (ip + extra > ipLimit) {
						throw new LevelDbException("Snappy: truncated literal length");
					}
					long l = 0;
					for (int i = 0; i < extra; i++) {
						l |= (long) (in[ip + i] & 0xff) << (8 * i);
					}
					ip += extra;
					if (l >= Integer.MAX_VALUE) {
						throw new LevelDbException("Snappy: literal too long");
					}
					len = (int) l;
				}
				len += 1;
				if (len > ipLimit - ip || len > out.length - op) {
					throw new LevelDbException("Snappy: literal overruns buffer");
				}
				System.arraycopy(in, ip, out, op, len);
				ip += len;
				op += len;
			} else {
				int len;
				int copyOffset;
				if (kind == 1) {
					if (ip >= ipLimit) {
						throw new LevelDbException("Snappy: truncated copy");
					}
					len = 4 + ((tag >>> 2) & 7);
					copyOffset = ((tag >>> 5) << 8) | (in[ip++] & 0xff);
				} else if (kind == 2) {
					if (ip + 2 > ipLimit) {
						throw new LevelDbException("Snappy: truncated copy");
					}
					len = (tag >>> 2) + 1;
					copyOffset = (in[ip] & 0xff) | (in[ip + 1] & 0xff) << 8;
					ip += 2;
				} else {
					if (ip + 4 > ipLimit) {
						throw new LevelDbException("Snappy: truncated copy");
					}
					len = (tag >>> 2) + 1;
					copyOffset = SliceInput.getFixed32(in, ip);
					ip += 4;
				}
				if (copyOffset <= 0 || copyOffset > op || len > out.length - op) {
					throw new LevelDbException("Snappy: invalid copy (offset " + copyOffset + ", length " + len + ")");
				}
				int src = op - copyOffset;
				if (copyOffset >= len) {
					System.arraycopy(out, src, out, op, len);
					op += len;
				} else {
					for (int i = 0; i < len; i++) {
						out[op++] = out[src++];
					}
				}
			}
		}
		if (op != out.length) {
			throw new LevelDbException("Snappy: expected " + out.length + " bytes, got " + op);
		}
		return out;
	}
}
