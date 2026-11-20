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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads databases written by real LevelDB (Google LevelDB via the classic-level npm package, with a
 * small generator script kept outside the repository) and compares them with the generator's record of
 * the expected live contents ({@code <name>.expected.tsv}: hex key, tab, hex value, in key order).
 * <p>
 * {@code memtable} holds only a write-ahead log; the {@code leveled-*} databases have a compacted
 * deeper level, two overlapping level-0 tables and a log, with overwrites and deletions at every stage,
 * written without compression.
 */
class LevelDbFixtureTest {
	private static final HexFormat HEX = HexFormat.of();

	static Path fixture(String name) throws URISyntaxException {
		return Path.of(LevelDbFixtureTest.class.getResource("/bedrock/leveldb/" + name).toURI());
	}

	/** Expected live entries as hex(key) -> value, in key order. */
	static LinkedHashMap<String, byte[]> expected(String name) throws IOException, URISyntaxException {
		LinkedHashMap<String, byte[]> map = new LinkedHashMap<>();
		for (String line : Files.readAllLines(fixture(name + ".expected.tsv"), StandardCharsets.US_ASCII)) {
			if (line.isEmpty()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			map.put(parts[0], HEX.parseHex(parts[1]));
		}
		return map;
	}

	private static byte[] key(String s) {
		return s.getBytes(StandardCharsets.ISO_8859_1);
	}

	@ParameterizedTest
	@ValueSource(strings = {"memtable", "leveled-none"})
	void iteratesExactlyTheLiveEntriesInOrder(String name) throws Exception {
		LinkedHashMap<String, byte[]> expected = expected(name);
		LinkedHashMap<String, byte[]> actual = new LinkedHashMap<>();
		try (LevelDbReader db = LevelDbReader.open(fixture(name))) {
			db.forEach((k, v) -> assertNull(actual.put(HEX.formatHex(k), v), "duplicate key"));
		}
		assertEquals(List.copyOf(expected.keySet()), List.copyOf(actual.keySet()));
		for (Map.Entry<String, byte[]> e : expected.entrySet()) {
			assertArrayEquals(e.getValue(), actual.get(e.getKey()), e.getKey());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"memtable", "leveled-none"})
	void getReturnsNewestValueForEveryKey(String name) throws Exception {
		LinkedHashMap<String, byte[]> expected = expected(name);
		try (LevelDbReader db = LevelDbReader.open(fixture(name))) {
			for (Map.Entry<String, byte[]> e : expected.entrySet()) {
				assertArrayEquals(e.getValue(), db.get(HEX.parseHex(e.getKey())), e.getKey());
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"leveled-none"})
	void deletedAndAbsentKeysAreNotFound(String name) throws Exception {
		LinkedHashMap<String, byte[]> expected = expected(name);
		List<String> missing = List.of(
			"key00020", "key00029",           // deleted in the second level-0 table
			"key00110", "key00159",           // deleted in the first level-0 table
			"key00500", "key00519",           // deleted in the log
			"key00600", "key", "kez", "a", "ÿÿ" // never written
		);
		try (LevelDbReader db = LevelDbReader.open(fixture(name))) {
			for (String k : missing) {
				assertFalse(expected.containsKey(HEX.formatHex(key(k))), "fixture sanity: " + k);
				assertNull(db.get(key(k)), k);
				assertFalse(db.contains(key(k)), k);
			}
			// resurrected after deletion, overwritten in three generations
			assertEquals("resurrected-100|", new String(db.get(key("key00100")), StandardCharsets.ISO_8859_1));
			assertEquals("v2b-5|v2b-5|", new String(db.get(key("key00005")), StandardCharsets.ISO_8859_1));
			assertEquals("v3-55|", new String(db.get(key("key00055")), StandardCharsets.ISO_8859_1));
			assertEquals("v2-75|v2-75|", new String(db.get(key("key00075")), StandardCharsets.ISO_8859_1));
			assertEquals(0, db.get(key("empty-value")).length);
			assertEquals("empty-key|", new String(db.get(new byte[0]), StandardCharsets.ISO_8859_1));
		}
	}

	@Test
	void memtableFixtureHasDeletedKeys() throws Exception {
		try (LevelDbReader db = LevelDbReader.open(fixture("memtable"))) {
			assertNull(db.get(key("mem0000")));
			assertNull(db.get(key("mem0190")));
			assertNull(db.get(key("batch002")));
			assertEquals("overwritten5|overwritten5|", new String(db.get(key("mem0005")), StandardCharsets.ISO_8859_1));
			assertEquals(0, db.version().files(0).size());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"leveled-none"})
	void leveledFixtureSpansSeveralLevelsAndTheLog(String name) throws Exception {
		try (LevelDbReader db = LevelDbReader.open(fixture(name))) {
			Version v = db.version();
			assertEquals(2, v.files(0).size(), "overlapping level-0 tables");
			int deeper = 0;
			for (int level = 1; level < v.numLevels(); level++) {
				deeper += v.files(level).size();
			}
			assertTrue(deeper >= 1, "tables below level 0");
			assertTrue(db.memTable().size() > 0, "unflushed log entries");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"memtable", "leveled-none"})
	void prefixIterationMatchesFilteredExpectation(String name) throws Exception {
		LinkedHashMap<String, byte[]> expected = expected(name);
		List<byte[]> prefixes = new ArrayList<>(List.of(key("key000"), key("key001"), key("key0059"), key("mem01"),
			key("batch"), key("k"), key("zzz"), key("nope"), new byte[]{(byte) 0xee}, new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff}));
		// Bedrock-style binary prefixes (x, z as int32 LE) present in the leveled fixtures
		prefixes.add(HEX.parseHex("eeffffff"));
		prefixes.add(HEX.parseHex("00000000fbffffff"));
		try (LevelDbReader db = LevelDbReader.open(fixture(name))) {
			for (byte[] prefix : prefixes) {
				String hexPrefix = HEX.formatHex(prefix);
				List<String> want = expected.keySet().stream().filter(k -> k.startsWith(hexPrefix)).toList();
				List<String> got = new ArrayList<>();
				db.forEachKeyWithPrefix(prefix, k -> got.add(HEX.formatHex(k)));
				assertEquals(want, got, "prefix " + hexPrefix);

				List<String> gotWithValues = new ArrayList<>();
				db.forEachWithPrefix(prefix, (k, v) -> {
					assertArrayEquals(expected.get(HEX.formatHex(k)), v);
					gotWithValues.add(HEX.formatHex(k));
				});
				assertEquals(want, gotWithValues);
			}
			List<String> all = new ArrayList<>();
			db.forEachKey(k -> all.add(HEX.formatHex(k)));
			assertEquals(List.copyOf(expected.keySet()), all);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"leveled-none"})
	void concurrentGetsWithTinyCaches(String name) throws Exception {
		LinkedHashMap<String, byte[]> expected = expected(name);
		List<String> keys = List.copyOf(expected.keySet());
		LevelDbReader.Options options = LevelDbReader.Options.builder().blockCacheBytes(2048).maxOpenTables(1).build();
		ExecutorService pool = Executors.newFixedThreadPool(8);
		try (LevelDbReader db = LevelDbReader.open(fixture(name), options)) {
			List<Future<Integer>> futures = new ArrayList<>();
			for (int t = 0; t < 8; t++) {
				int seed = t;
				futures.add(pool.submit(() -> {
					Random random = new Random(seed);
					int checked = 0;
					for (int i = 0; i < 3000; i++) {
						String k = keys.get(random.nextInt(keys.size()));
						assertArrayEquals(expected.get(k), db.get(HEX.parseHex(k)), k);
						checked++;
						if (i % 1000 == 0) {
							// interleave a scan, which pins tables while gets evict them
							int[] n = {0};
							db.forEachKeyWithPrefix(key("key00"), k2 -> n[0]++);
							assertTrue(n[0] > 0);
						}
					}
					return checked;
				}));
			}
			for (Future<Integer> f : futures) {
				assertEquals(3000, f.get());
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"memtable", "leveled-none"})
	void readingLeavesTheDatabaseDirectoryUntouched(String name) throws Exception {
		Path dir = fixture(name);
		Map<String, String> before = snapshot(dir);
		try (LevelDbReader db = LevelDbReader.open(dir)) {
			db.forEach((k, v) -> {
			});
			db.get(key("key00001"));
			try (LevelDbCursor cursor = db.cursor()) {
				cursor.next();
			}
		}
		assertEquals(before, snapshot(dir));
	}

	static Map<String, String> snapshot(Path dir) throws IOException, NoSuchAlgorithmException {
		Map<String, String> result = new TreeMap<>();
		result.put(".", "mtime=" + Files.getLastModifiedTime(dir).toMillis());
		try (Stream<Path> files = Files.list(dir)) {
			for (Path f : files.toList()) {
				byte[] content = Files.readAllBytes(f);
				String sha = HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(content));
				result.put(f.getFileName().toString(), "size=" + content.length
					+ " mtime=" + Files.getLastModifiedTime(f).toMillis() + " sha256=" + sha);
			}
		}
		return result;
	}
}
