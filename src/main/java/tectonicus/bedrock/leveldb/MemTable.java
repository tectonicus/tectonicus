/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * In-memory view of the write-ahead logs that have not yet been flushed to tables: the newest entry
 * (value or tombstone) per user key. Built once at open time and immutable afterwards, so concurrent
 * reads are safe.
 */
@Slf4j
final class MemTable {
	record Entry(long sequence, byte[] value) {
		boolean isDeletion() {
			return value == null;
		}
	}

	private final TreeMap<ByteKey, Entry> entries = new TreeMap<>();
	private long maxSequence;

	int size() {
		return entries.size();
	}

	long maxSequence() {
		return maxSequence;
	}

	LookupResult get(byte[] userKey) {
		Entry e = entries.get(new ByteKey(userKey));
		if (e == null) {
			return null;
		}
		return e.isDeletion() ? LookupResult.DELETED : new LookupResult(e.value().clone());
	}

	/**
	 * Replays a write-ahead log file. Stops at the first corrupt or truncated record with a warning,
	 * keeping everything before it.
	 */
	void replayLog(Path logFile) throws IOException {
		byte[] data = Files.readAllBytes(logFile);
		String name = logFile.getFileName().toString();
		LogReader reader = new LogReader(data, name);
		int batches = 0;
		try {
			byte[] record;
			while ((record = reader.nextRecord()) != null) {
				applyBatch(record, name);
				batches++;
			}
			if (reader.hasTruncatedTail()) {
				log.warn("LevelDB log {} ends with an incomplete record at offset {}; ignoring the tail (the writer probably stopped mid-write)",
					name, reader.position());
			}
		} catch (LevelDbException e) {
			log.warn("Stopped replaying LevelDB log {} after {} batches: {}", name, batches, e.getMessage());
		}
	}

	/**
	 * Applies a WriteBatch: {@code fixed64 sequence, fixed32 count}, then {@code count} records of
	 * {@code 1 (put), varstring key, varstring value} or {@code 0 (delete), varstring key}.
	 * The batch is parsed completely before any of it is applied, so a corrupt batch has no effect.
	 */
	void applyBatch(byte[] batch, String source) throws LevelDbException {
		if (batch.length < 12) {
			throw new LevelDbException("WriteBatch too small (" + batch.length + " bytes) in " + source);
		}
		SliceInput in = new SliceInput(batch);
		long sequence = in.readFixed64();
		int count = in.readFixed32();
		TreeMap<ByteKey, Entry> pending = new TreeMap<>();
		int found = 0;
		while (in.hasRemaining()) {
			int type = in.readByte();
			byte[] key = in.readLengthPrefixed();
			long seq = sequence + found;
			switch (type) {
				case InternalKeys.TYPE_VALUE -> pending.put(new ByteKey(key), new Entry(seq, in.readLengthPrefixed()));
				case InternalKeys.TYPE_DELETION -> pending.put(new ByteKey(key), new Entry(seq, null));
				default -> throw new LevelDbException("Unknown WriteBatch record type " + type + " in " + source);
			}
			found++;
		}
		if (found != count) {
			throw new LevelDbException("WriteBatch count mismatch in " + source + ": header says " + count + ", found " + found);
		}
		for (Map.Entry<ByteKey, Entry> e : pending.entrySet()) {
			Entry existing = entries.get(e.getKey());
			if (existing == null || existing.sequence() <= e.getValue().sequence()) {
				entries.put(e.getKey(), e.getValue());
			}
		}
		if (count > 0) {
			maxSequence = Math.max(maxSequence, sequence + count - 1);
		}
	}

	InternalIterator iterator() {
		return new Iter();
	}

	private final class Iter implements InternalIterator {
		private Iterator<Map.Entry<ByteKey, Entry>> it;
		private Map.Entry<ByteKey, Entry> current;
		private byte[] key;

		@Override
		public boolean isValid() {
			return current != null;
		}

		@Override
		public void seekToFirst() {
			position(entries);
		}

		@Override
		public void seek(byte[] target) {
			ByteKey userKey = new ByteKey(InternalKeys.userKey(target));
			position(entries.tailMap(userKey, true));
			// An entry for the same user key with a newer sequence than the target sorts before it
			if (current != null && InternalKeys.compare(key, target) < 0) {
				next();
			}
		}

		private void position(NavigableMap<ByteKey, Entry> map) {
			it = map.entrySet().iterator();
			next();
		}

		@Override
		public void next() {
			if (it.hasNext()) {
				current = it.next();
				Entry e = current.getValue();
				int type = e.isDeletion() ? InternalKeys.TYPE_DELETION : InternalKeys.TYPE_VALUE;
				key = InternalKeys.make(current.getKey().bytes(), (e.sequence() << 8) | type);
			} else {
				current = null;
				key = null;
			}
		}

		@Override
		public byte[] key() {
			return key;
		}

		@Override
		public byte[] value() {
			byte[] v = current.getValue().value();
			return v == null ? new byte[0] : v.clone();
		}

		@Override
		public void close() {
		}
	}
}
