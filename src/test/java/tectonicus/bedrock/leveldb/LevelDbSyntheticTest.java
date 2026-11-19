/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tectonicus.bedrock.leveldb.TestDbWriter.bytes;
import static tectonicus.bedrock.leveldb.TestDbWriter.del;
import static tectonicus.bedrock.leveldb.TestDbWriter.put;

/** Tests against databases built by {@link TestDbWriter}, covering formats real fixtures can't. */
class LevelDbSyntheticTest {
	@TempDir
	Path dir;

	private static String str(byte[] b) {
		return b == null ? null : new String(b, StandardCharsets.ISO_8859_1);
	}

	private static Map<ByteKey, byte[]> readAll(LevelDbReader db) throws IOException {
		Map<ByteKey, byte[]> all = new TreeMap<>();
		List<ByteKey> order = new ArrayList<>();
		db.forEach((k, v) -> {
			all.put(new ByteKey(k), v);
			order.add(new ByteKey(k));
		});
		List<ByteKey> sorted = new ArrayList<>(order);
		sorted.sort(null);
		assertEquals(sorted, order, "keys must be produced in ascending order");
		assertEquals(order.size(), all.size(), "each key must be produced once");
		return all;
	}

	@Test
	void rejectsSnappyBlocks() {
		LevelDbException e = assertThrows(LevelDbException.class, () -> Compression.decompress(Compression.SNAPPY, new byte[4], 0, 4));
		assertTrue(e.getMessage().contains("snappy"));
	}

	@ParameterizedTest
	@ValueSource(ints = {Compression.NONE, Compression.ZLIB, Compression.ZLIB_RAW})
	void readsEveryCompressionType(int compression) throws IOException {
		List<TestDbWriter.Entry> entries = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			entries.add(put(String.format("k%04d", i), i + 1, ("value-" + i + "-").repeat(1 + i % 9)));
		}
		TestDbWriter.TableFile table = TestDbWriter.writeTable(dir, 1, 7, entries, compression, 300);
		TestDbWriter.writeManifest(dir, 8, List.of(TestDbWriter.versionEdit(9, 1000, 10, List.of(table))));

		try (LevelDbReader db = LevelDbReader.open(dir)) {
			for (int i = 0; i < 1000; i++) {
				assertEquals(("value-" + i + "-").repeat(1 + i % 9), str(db.get(bytes(String.format("k%04d", i)))));
			}
			assertNull(db.get(bytes("k")));
			assertNull(db.get(bytes("k10000")));
			assertNull(db.get(bytes("k0500x")));
			assertEquals(1000, readAll(db).size());
			int[] count = {0};
			db.forEachKeyWithPrefix(bytes("k05"), k -> count[0]++);
			assertEquals(100, count[0]);
		}
	}

	@Test
	void mergesAllLevelsAndLogsNewestFirst() throws IOException {
		Random random = new Random(42);
		List<String> pool = new ArrayList<>();
		for (int i = 0; i < 400; i++) {
			pool.add("key-" + Integer.toString(i * 7919 % 4001, 36) + (i % 3 == 0 ? "ÿ" : ""));
		}
		pool.add("");
		pool.sort((a, b) -> Arrays.compareUnsigned(bytes(a), bytes(b)));
		Map<String, String> model = new TreeMap<>();
		long[] seq = {1};

		// Level 2: one table with two versions of some keys
		List<TestDbWriter.Entry> l2 = new ArrayList<>();
		for (String k : pool) {
			if (random.nextInt(4) == 0) {
				l2.add(put(k, seq[0]++, "l2-old-" + k));
			}
			String v = "l2-" + k;
			l2.add(put(k, seq[0]++, v));
			model.put(k, v);
		}
		// Level 1: two disjoint tables
		List<TestDbWriter.Entry> l1a = new ArrayList<>();
		List<TestDbWriter.Entry> l1b = new ArrayList<>();
		for (int i = 0; i < pool.size(); i++) {
			String k = pool.get(i);
			List<TestDbWriter.Entry> target = i < pool.size() / 2 ? l1a : l1b;
			int r = random.nextInt(3);
			if (r == 0) {
				target.add(del(k, seq[0]++));
				model.remove(k);
			} else if (r == 1) {
				target.add(put(k, seq[0]++, "l1-" + k));
				model.put(k, "l1-" + k);
			}
		}
		// Level 0: two overlapping tables (the higher-numbered one is newer)
		List<List<TestDbWriter.Entry>> l0 = List.of(new ArrayList<>(), new ArrayList<>());
		for (List<TestDbWriter.Entry> table : l0) {
			for (String k : pool) {
				int r = random.nextInt(5);
				if (r == 0) {
					table.add(del(k, seq[0]++));
					model.remove(k);
				} else if (r == 1) {
					String v = "l0-" + seq[0] + "-" + k;
					table.add(put(k, seq[0]++, v));
					model.put(k, v);
				}
			}
		}
		List<TestDbWriter.TableFile> files = List.of(
			TestDbWriter.writeTable(dir, 2, 3, l2, Compression.ZLIB_RAW, 200),
			TestDbWriter.writeTable(dir, 1, 5, l1a, Compression.ZLIB, 512),
			TestDbWriter.writeTable(dir, 1, 6, l1b, Compression.ZLIB_RAW, 128),
			TestDbWriter.writeTable(dir, 0, 8, l0.get(0), Compression.NONE, 1024),
			TestDbWriter.writeTable(dir, 0, 9, l0.get(1), Compression.ZLIB_RAW, 4096));

		// Log 11: several batches, including deletes and re-puts of the same key within one batch
		List<byte[]> batches = new ArrayList<>();
		for (int b = 0; b < 20; b++) {
			List<TestDbWriter.Entry> ops = new ArrayList<>();
			long start = seq[0];
			for (int i = 0; i < 10; i++) {
				String k = pool.get(random.nextInt(pool.size()));
				if (random.nextBoolean()) {
					ops.add(del(k, seq[0]++));
					model.remove(k);
				} else {
					String v = "log-" + seq[0];
					ops.add(put(k, seq[0]++, v));
					model.put(k, v);
				}
			}
			batches.add(TestDbWriter.writeBatch(start, ops));
		}
		Files.write(dir.resolve("000011.log"), TestDbWriter.logFile(batches));
		// A stale log older than the manifest's log number must be ignored
		Files.write(dir.resolve("000002.log"), TestDbWriter.logFile(List.of(
			TestDbWriter.writeBatch(1, List.of(put(pool.get(0), 1, "stale"), put("stale-key", 2, "stale"))))));

		// Manifest with several edits, including a file that was added and later deleted
		TestDbWriter.TableFile ghost = new TestDbWriter.TableFile(0, 4, 100, InternalKeys.make(bytes("a"), 1 << 8 | 1),
			InternalKeys.make(bytes("z"), 1 << 8 | 1));
		byte[] deleteGhost = deletedFileEdit(0, 4);
		TestDbWriter.writeManifest(dir, 10, List.of(
			TestDbWriter.versionEdit(1, 1, 5, List.of(ghost, files.get(0))),
			TestDbWriter.versionEdit(11, seq[0] - 1, 12, files.subList(1, files.size())),
			deleteGhost));

		try (LevelDbReader db = LevelDbReader.open(dir, LevelDbReader.Options.builder().blockCacheBytes(4096).maxOpenTables(2).build())) {
			assertEquals(2, db.version().files(0).size());
			assertEquals(9, db.version().files(0).getFirst().number(), "level 0 newest first");

			Map<ByteKey, byte[]> all = readAll(db);
			Map<ByteKey, byte[]> want = new TreeMap<>();
			model.forEach((k, v) -> want.put(new ByteKey(bytes(k)), bytes(v)));
			assertEquals(want.keySet(), all.keySet());
			for (Map.Entry<ByteKey, byte[]> e : want.entrySet()) {
				assertArrayEquals(e.getValue(), all.get(e.getKey()), e.getKey().toString());
			}
			for (String k : pool) {
				assertEquals(model.get(k), str(db.get(bytes(k))), k);
			}
			assertNull(db.get(bytes("stale-key")));

			// Prefix scans
			for (String prefix : List.of("key-1", "key-a", "key-", "x", "")) {
				List<String> got = new ArrayList<>();
				db.forEachKeyWithPrefix(bytes(prefix), k -> got.add(str(k)));
				List<String> exp = model.keySet().stream().filter(k -> k.startsWith(prefix)).toList();
				assertEquals(exp, got, prefix);
			}
		}
	}

	private static byte[] deletedFileEdit(int level, long number) {
		var out = new java.io.ByteArrayOutputStream();
		TestDbWriter.putVarint(out, 6);
		TestDbWriter.putVarint(out, level);
		TestDbWriter.putVarint(out, number);
		return out.toByteArray();
	}

	private void writeEmptyManifest(long logNumber, long lastSequence) throws IOException {
		TestDbWriter.writeManifest(dir, 1, List.of(TestDbWriter.versionEdit(logNumber, lastSequence, 100, List.of())));
	}

	private static List<byte[]> numberedBatches(int count, int valueSize) {
		List<byte[]> batches = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			batches.add(TestDbWriter.writeBatch(i + 1, List.of(put(String.format("b%03d", i), i + 1, String.valueOf((char) ('a' + i % 26)).repeat(valueSize)))));
		}
		return batches;
	}

	@Test
	void logRecordsSpanningBlocksAreReassembled() throws IOException {
		writeEmptyManifest(2, 0);
		List<byte[]> batches = new ArrayList<>(numberedBatches(30, 3000));
		byte[] huge = new byte[100_000];
		new Random(1).nextBytes(huge);
		batches.add(TestDbWriter.writeBatch(31, List.of(new TestDbWriter.Entry(bytes("huge"), 31, false, huge))));
		Files.write(dir.resolve("000002.log"), TestDbWriter.logFile(batches));
		try (LevelDbReader db = LevelDbReader.open(dir)) {
			assertArrayEquals(huge, db.get(bytes("huge")));
			assertEquals("c".repeat(3000), str(db.get(bytes("b002"))));
			assertEquals(31, readAll(db).size());
		}
	}

	@Test
	void truncatedLogTailIsIgnored() throws IOException {
		writeEmptyManifest(2, 0);
		byte[] log = TestDbWriter.logFile(numberedBatches(20, 5000));
		// Cut into the middle of the last record (which spans a block boundary)
		for (int cut : new int[]{log.length - 10, log.length - 4000, log.length - 5020}) {
			Files.write(dir.resolve("000002.log"), Arrays.copyOf(log, cut));
			try (LevelDbReader db = LevelDbReader.open(dir)) {
				assertEquals(19, readAll(db).size(), "cut at " + cut);
				assertNull(db.get(bytes("b019")));
				assertEquals("s".repeat(5000), str(db.get(bytes("b018"))));
			}
		}
	}

	@Test
	void corruptLogRecordStopsReplay() throws IOException {
		writeEmptyManifest(2, 0);
		byte[] log = TestDbWriter.logFile(numberedBatches(10, 100));
		// Each record is 7 + (12 + 1 + 1 + 4 + 1 + 100) bytes; corrupt a payload byte of the 4th record
		int recordSize = 7 + 12 + 1 + 1 + 4 + 1 + 100;
		log[3 * recordSize + 20] ^= 0x55;
		Files.write(dir.resolve("000002.log"), log);
		try (LevelDbReader db = LevelDbReader.open(dir)) {
			assertEquals(3, readAll(db).size());
			assertEquals("c".repeat(100), str(db.get(bytes("b002"))));
			assertNull(db.get(bytes("b003")));
		}
	}

	@Test
	void truncatedRealLogIsTolerated() throws Exception {
		Path source = LevelDbFixtureTest.fixture("memtable");
		try (Stream<Path> files = Files.list(source)) {
			for (Path f : files.toList()) {
				Files.copy(f, dir.resolve(f.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
			}
		}
		Path log = dir.resolve("000003.log");
		byte[] full = Files.readAllBytes(log);
		Map<String, byte[]> expected = LevelDbFixtureTest.expected("memtable");
		Files.write(log, Arrays.copyOf(full, full.length - 37));
		try (LevelDbReader db = LevelDbReader.open(dir)) {
			Map<ByteKey, byte[]> all = readAll(db);
			assertTrue(all.size() > 100, "most entries survive");
			// The last batch was cut off, so its puts are missing but earlier data is intact
			assertNull(db.get(bytes("batch000")));
			assertEquals("value1|value1|", str(db.get(bytes("mem0001"))));
		}
		assertTrue(expected.containsKey(java.util.HexFormat.of().formatHex(bytes("batch000"))));
	}

	@Test
	void unknownManifestTagFailsClearly() throws IOException {
		var edit = new java.io.ByteArrayOutputStream();
		edit.writeBytes(TestDbWriter.versionEdit(2, 0, 3, List.of()));
		TestDbWriter.putVarint(edit, 8);
		TestDbWriter.writeManifest(dir, 1, List.of(edit.toByteArray()));
		LevelDbException e = assertThrows(LevelDbException.class, () -> LevelDbReader.open(dir));
		assertTrue(e.getMessage().contains("Unknown VersionEdit tag 8"), e.getMessage());
	}

	@Test
	void missingCurrentFailsClearly() {
		LevelDbException e = assertThrows(LevelDbException.class, () -> LevelDbReader.open(dir));
		assertTrue(e.getMessage().contains("CURRENT"), e.getMessage());
		assertThrows(LevelDbException.class, () -> LevelDbReader.open(dir.resolve("does-not-exist")));
	}

	@Test
	void tableChecksumMismatchIsDetected() throws IOException {
		List<TestDbWriter.Entry> entries = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			entries.add(put(String.format("k%04d", i), i + 1, "v" + i));
		}
		TestDbWriter.TableFile table = TestDbWriter.writeTable(dir, 1, 7, entries, Compression.NONE, 4096);
		TestDbWriter.writeManifest(dir, 8, List.of(TestDbWriter.versionEdit(9, 50, 10, List.of(table))));
		Path file = dir.resolve("000007.ldb");
		byte[] data = Files.readAllBytes(file);
		data[30] ^= 0x01; // inside the first data block
		Files.write(file, data);
		try (LevelDbReader db = LevelDbReader.open(dir)) {
			LevelDbException e = assertThrows(LevelDbException.class, () -> db.get(bytes("k0001")));
			assertTrue(e.getMessage().contains("checksum"), e.getMessage());
		}
	}

	@Test
	void closedReaderRejectsReads() throws IOException {
		writeEmptyManifest(2, 0);
		LevelDbReader db = LevelDbReader.open(dir);
		db.close();
		db.close();
		assertThrows(IOException.class, () -> db.get(bytes("x")));
	}
}
