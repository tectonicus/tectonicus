/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.util.Arrays;

/**
 * An uncompressed table block: prefix-compressed entries followed by an array of fixed32 restart
 * offsets and a fixed32 restart count. Each entry is
 * {@code varint shared, varint nonShared, varint valueLength, keyDelta[nonShared], value[valueLength]}.
 * All keys must be internal keys (true for data and index blocks; the metaindex block is never read).
 * Immutable and safe to share between threads; iterators are per-thread.
 */
final class Block {
	private final byte[] data;
	private final int restartOffset;
	private final int numRestarts;

	Block(byte[] data) throws LevelDbException {
		if (data.length < 4) {
			throw new LevelDbException("Block too small: " + data.length + " bytes");
		}
		int n = SliceInput.getFixed32(data, data.length - 4);
		long maxRestarts = (data.length - 4) / 4;
		if (n < 0 || n > maxRestarts) {
			throw new LevelDbException("Bad restart count " + n + " in block of " + data.length + " bytes");
		}
		this.data = data;
		this.numRestarts = n;
		this.restartOffset = data.length - 4 - 4 * n;
	}

	int size() {
		return data.length;
	}

	Iter iterator() {
		return new Iter();
	}

	private int restartPoint(int index) {
		return SliceInput.getFixed32(data, restartOffset + 4 * index);
	}

	/** Iterator over the block's entries. Keys are compared as internal keys. */
	final class Iter implements InternalIterator {
		/** Offset of the current entry; {@code restartOffset} when invalid. */
		private int current = restartOffset;
		private int nextOffset = restartOffset;
		private byte[] key = new byte[0];
		private int valueOffset;
		private int valueLength;

		@Override
		public boolean isValid() {
			return current < restartOffset;
		}

		@Override
		public byte[] key() {
			return key;
		}

		@Override
		public byte[] value() {
			return Arrays.copyOfRange(data, valueOffset, valueOffset + valueLength);
		}

		@Override
		public void seekToFirst() throws LevelDbException {
			seekToRestart(0);
			parseNext();
		}

		@Override
		public void next() throws LevelDbException {
			parseNext();
		}

		@Override
		public void seek(byte[] target) throws LevelDbException {
			if (numRestarts == 0) {
				current = nextOffset = restartOffset;
				return;
			}
			int left = 0;
			int right = numRestarts - 1;
			while (left < right) {
				int mid = (left + right + 1) >>> 1;
				byte[] midKey = keyAtRestart(mid);
				if (InternalKeys.compare(midKey, target) < 0) {
					left = mid;
				} else {
					right = mid - 1;
				}
			}
			seekToRestart(left);
			while (parseNext()) {
				if (InternalKeys.compare(key, target) >= 0) {
					return;
				}
			}
		}

		@Override
		public void close() {
		}

		private byte[] keyAtRestart(int index) throws LevelDbException {
			int offset = checkRestart(index);
			SliceInput in = new SliceInput(data, offset, restartOffset - offset);
			int shared = in.readVarint32();
			int nonShared = in.readVarint32();
			in.readVarint32();
			if (shared != 0) {
				throw new LevelDbException("Corrupt block: restart entry has shared prefix");
			}
			if (nonShared < 8) {
				throw new LevelDbException("Corrupt block: restart key shorter than an internal key");
			}
			return in.readBytes(nonShared);
		}

		private int checkRestart(int index) throws LevelDbException {
			int offset = restartPoint(index);
			if (offset < 0 || offset > restartOffset) {
				throw new LevelDbException("Corrupt block: bad restart offset " + offset);
			}
			return offset;
		}

		private void seekToRestart(int index) throws LevelDbException {
			key = new byte[0];
			nextOffset = numRestarts == 0 ? restartOffset : checkRestart(index);
			current = nextOffset;
		}

		/** Advances to the entry at {@code nextOffset}; returns false (and becomes invalid) at the end. */
		private boolean parseNext() throws LevelDbException {
			current = nextOffset;
			if (current >= restartOffset) {
				current = nextOffset = restartOffset;
				return false;
			}
			SliceInput in = new SliceInput(data, current, restartOffset - current);
			int shared = in.readVarint32();
			int nonShared = in.readVarint32();
			int valueLen = in.readVarint32();
			if (shared > key.length || nonShared < 0 || valueLen < 0 || (long) nonShared + valueLen > in.remaining()) {
				throw new LevelDbException("Corrupt block entry at offset " + current);
			}
			if (shared + nonShared < 8) {
				throw new LevelDbException("Corrupt block entry at offset " + current + ": key shorter than an internal key");
			}
			byte[] newKey = new byte[shared + nonShared];
			System.arraycopy(key, 0, newKey, 0, shared);
			int p = in.position();
			System.arraycopy(data, p, newKey, shared, nonShared);
			key = newKey;
			valueOffset = p + nonShared;
			valueLength = valueLen;
			nextOffset = valueOffset + valueLen;
			return true;
		}
	}
}
