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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnappyTest {
	/** Inputs matching those used to generate snappy-vectors.txt (compressed with the snappyjs library). */
	private static Map<String, byte[]> inputs() {
		byte[] pattern = new byte[3000];
		for (int i = 0; i < pattern.length; i++) {
			pattern[i] = (byte) ((i % 251) ^ ((i >> 7) & 0x0f));
		}
		byte[] run = new byte[70000];
		Arrays.fill(run, (byte) 'a');
		return Map.of(
			"empty", new byte[0],
			"hello", "hello".getBytes(StandardCharsets.US_ASCII),
			"repeat", "abc".repeat(24).getBytes(StandardCharsets.US_ASCII),
			"pattern", pattern,
			"text", ("The quick brown fox jumps over the lazy dog. ".repeat(40) + "Tectonicus bedrock leveldb reader!".repeat(3)).getBytes(StandardCharsets.US_ASCII),
			"run", run
		);
	}

	@Test
	void decompressesReferenceVectors() throws IOException {
		Map<String, byte[]> inputs = inputs();
		int count = 0;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
			SnappyTest.class.getResourceAsStream("/bedrock/leveldb/snappy-vectors.txt"), StandardCharsets.US_ASCII))) {
			String line;
			while ((line = r.readLine()) != null) {
				String[] parts = line.trim().split(" ");
				byte[] expected = inputs.get(parts[0]);
				assertEquals(Integer.parseInt(parts[1]), expected.length, parts[0]);
				byte[] compressed = HexFormat.of().parseHex(parts[2]);
				assertArrayEquals(expected, Snappy.decompress(compressed, 0, compressed.length), parts[0]);
				count++;
			}
		}
		assertEquals(inputs.size(), count);
	}

	@Test
	void decompressesAllCopyKinds() throws IOException {
		HexFormat hex = HexFormat.of();
		// length 12: literal "abcd", copy1 (len 4, off 4), copy2 (len 2, off 3), copy4 (len 2, off 10)
		byte[] compressed = hex.parseHex("0c" + "0c61626364" + "0104" + "060300" + "070a000000");
		assertArrayEquals("abcdabcdbcab".getBytes(StandardCharsets.US_ASCII), Snappy.decompress(compressed, 0, compressed.length));
	}

	@Test
	void handlesOverlappingCopiesAndOffsets() throws IOException {
		// Embedded in a larger array at an offset; overlapping copy (offset 1, length 9) repeats 'x'
		byte[] inner = HexFormat.of().parseHex("0a" + "0078" + "1501");
		byte[] padded = new byte[inner.length + 6];
		System.arraycopy(inner, 0, padded, 3, inner.length);
		assertArrayEquals("xxxxxxxxxx".getBytes(StandardCharsets.US_ASCII), Snappy.decompress(padded, 3, inner.length));
	}

	@Test
	void rejectsCorruptInput() {
		HexFormat hex = HexFormat.of();
		// copy before any output
		assertThrows(LevelDbException.class, () -> decompress(hex.parseHex("040104")));
		// literal longer than input
		assertThrows(LevelDbException.class, () -> decompress(hex.parseHex("050c6162")));
		// output shorter than declared length
		assertThrows(LevelDbException.class, () -> decompress(hex.parseHex("0a0061")));
		// output longer than declared length
		assertThrows(LevelDbException.class, () -> decompress(hex.parseHex("01046162")));
		// truncated length varint
		assertThrows(LevelDbException.class, () -> decompress(hex.parseHex("80")));
	}

	private static byte[] decompress(byte[] b) throws LevelDbException {
		return Snappy.decompress(b, 0, b.length);
	}
}
