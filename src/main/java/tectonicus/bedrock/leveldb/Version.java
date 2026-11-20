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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The set of live table files per level, as recorded by the MANIFEST that {@code CURRENT} points at.
 * <p>
 * The MANIFEST is a log-format file whose records are VersionEdits: a sequence of tagged fields that,
 * applied in order, yield the current file set.
 */
@Slf4j
final class Version {
	static final int NUM_LEVELS = 7;
	static final String BYTEWISE_COMPARATOR = "leveldb.BytewiseComparator";

	// VersionEdit tags
	private static final int TAG_COMPARATOR = 1;
	private static final int TAG_LOG_NUMBER = 2;
	private static final int TAG_NEXT_FILE_NUMBER = 3;
	private static final int TAG_LAST_SEQUENCE = 4;
	private static final int TAG_COMPACT_POINTER = 5;
	private static final int TAG_DELETED_FILE = 6;
	private static final int TAG_NEW_FILE = 7;
	// 8 was kLargeValueRef in very old LevelDB versions and is no longer valid
	private static final int TAG_PREV_LOG_NUMBER = 9;

	/** Files per level; level 0 sorted newest (highest number) first, other levels by smallest key. */
	private final List<List<FileMetaData>> levels;
	private final String manifestName;
	private final long logNumber;
	private final long prevLogNumber;
	private final long lastSequence;
	private final long nextFileNumber;

	private Version(List<List<FileMetaData>> levels, String manifestName, long logNumber, long prevLogNumber, long lastSequence, long nextFileNumber) {
		this.levels = levels;
		this.manifestName = manifestName;
		this.logNumber = logNumber;
		this.prevLogNumber = prevLogNumber;
		this.lastSequence = lastSequence;
		this.nextFileNumber = nextFileNumber;
	}

	List<FileMetaData> files(int level) {
		return levels.get(level);
	}

	int numLevels() {
		return levels.size();
	}

	String manifestName() {
		return manifestName;
	}

	long logNumber() {
		return logNumber;
	}

	long prevLogNumber() {
		return prevLogNumber;
	}

	long lastSequence() {
		return lastSequence;
	}

	long nextFileNumber() {
		return nextFileNumber;
	}

	/** Reads {@code CURRENT} and replays the MANIFEST it names. */
	static Version load(Path dbDir) throws IOException {
		Path currentFile = dbDir.resolve("CURRENT");
		String current;
		try {
			current = new String(Files.readAllBytes(currentFile), StandardCharsets.UTF_8).trim();
		} catch (NoSuchFileException e) {
			throw new LevelDbException("Not a LevelDB database (no CURRENT file): " + dbDir, e);
		}
		if (!current.matches("MANIFEST-\\d+")) {
			throw new LevelDbException("CURRENT file does not name a MANIFEST: '" + current + "'");
		}
		Path manifest = dbDir.resolve(current);
		byte[] data;
		try {
			data = Files.readAllBytes(manifest);
		} catch (NoSuchFileException e) {
			throw new LevelDbException("MANIFEST named by CURRENT does not exist: " + manifest, e);
		}
		return parse(data, current);
	}

	static Version parse(byte[] manifestData, String manifestName) throws LevelDbException {
		List<Map<Long, FileMetaData>> files = new ArrayList<>();
		for (int i = 0; i < NUM_LEVELS; i++) {
			files.add(new TreeMap<>());
		}
		long logNumber = 0;
		long prevLogNumber = 0;
		long lastSequence = 0;
		long nextFileNumber = 0;
		boolean haveLogNumber = false;
		boolean haveLastSequence = false;

		LogReader reader = new LogReader(manifestData, manifestName);
		int records = 0;
		byte[] record;
		while ((record = reader.nextRecord()) != null) {
			records++;
			SliceInput in = new SliceInput(record);
			while (in.hasRemaining()) {
				int tag = in.readVarint32();
				switch (tag) {
					case TAG_COMPARATOR -> {
						String comparator = new String(in.readLengthPrefixed(), StandardCharsets.UTF_8);
						if (!BYTEWISE_COMPARATOR.equals(comparator)) {
							throw new LevelDbException("Unsupported comparator '" + comparator + "' in " + manifestName);
						}
					}
					case TAG_LOG_NUMBER -> {
						logNumber = in.readVarint64();
						haveLogNumber = true;
					}
					case TAG_NEXT_FILE_NUMBER -> nextFileNumber = in.readVarint64();
					case TAG_LAST_SEQUENCE -> {
						lastSequence = in.readVarint64();
						haveLastSequence = true;
					}
					case TAG_COMPACT_POINTER -> {
						readLevel(in, manifestName);
						in.readLengthPrefixed();
					}
					case TAG_DELETED_FILE -> {
						int level = readLevel(in, manifestName);
						files.get(level).remove(in.readVarint64());
					}
					case TAG_NEW_FILE -> {
						int level = readLevel(in, manifestName);
						long number = in.readVarint64();
						long size = in.readVarint64();
						byte[] smallest = in.readLengthPrefixed();
						byte[] largest = in.readLengthPrefixed();
						InternalKeys.checkValid(smallest);
						InternalKeys.checkValid(largest);
						files.get(level).put(number, new FileMetaData(number, size, smallest, largest));
					}
					case TAG_PREV_LOG_NUMBER -> prevLogNumber = in.readVarint64();
					default -> throw new LevelDbException("Unknown VersionEdit tag " + tag + " in " + manifestName
						+ " (record " + records + ")");
				}
			}
		}
		if (reader.hasTruncatedTail()) {
			log.warn("{} ends with an incomplete record; ignoring it", manifestName);
		}
		if (!haveLogNumber || !haveLastSequence) {
			throw new LevelDbException(manifestName + " is missing " + (haveLogNumber ? "last sequence" : "log number"));
		}

		List<List<FileMetaData>> levels = new ArrayList<>(NUM_LEVELS);
		for (int level = 0; level < NUM_LEVELS; level++) {
			List<FileMetaData> list = new ArrayList<>(files.get(level).values());
			if (level == 0) {
				list.sort(Comparator.comparingLong(FileMetaData::number).reversed());
			} else {
				list.sort((a, b) -> InternalKeys.compare(a.smallest(), b.smallest()));
			}
			levels.add(List.copyOf(list));
		}
		return new Version(List.copyOf(levels), manifestName, logNumber, prevLogNumber, lastSequence, nextFileNumber);
	}

	private static int readLevel(SliceInput in, String manifestName) throws LevelDbException {
		int level = in.readVarint32();
		if (level < 0 || level >= NUM_LEVELS) {
			throw new LevelDbException("Invalid level " + level + " in " + manifestName);
		}
		return level;
	}
}
