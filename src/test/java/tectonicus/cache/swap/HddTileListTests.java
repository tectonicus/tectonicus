/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.TileCoord;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HddTileListTests {
	@TempDir Path directory;

	@Test
	void deduplicatesAndTracksMembershipAndIndependentBounds() {
		try (HddTileListFactory factory = new HddTileListFactory(directory.toFile())) {
			HddTileList list = factory.createList();
			assertEquals(0, list.size());
			assertFalse(list.iterator().hasNext());
			assertTrue(list.add(new TileCoord(-10, 40)));
			assertTrue(list.add(new TileCoord(20, -30)));
			assertFalse(list.add(new TileCoord(-10, 40)));
			assertEquals(2, list.size());
			assertTrue(list.contains(new TileCoord(-10, 40)));
			assertFalse(list.contains(new TileCoord(40, -10)));
			assertEquals(new TileCoord(-10, -30), list.getAbsoluteMinCoord());
			assertEquals(new TileCoord(20, 40), list.getAbsoluteMaxCoord());
			assertEquals(Set.of(new TileCoord(-10, 40), new TileCoord(20, -30)), list.toSet());
		}
	}

	@Test
	void roundTripsSignedCoordinateExtremesAndRandomValues() {
		int[] edges = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -16777219, -65536, -1, 0, 1,
				65536, 16777219, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
		for (int x : edges) {
			for (int y : edges) {
				TileCoord tile = new TileCoord(x, y);
				assertEquals(tile, HddTileList.decode(HddTileList.encode(tile)));
			}
		}
		Random random = new Random(42);
		for (int i = 0; i < 1000; i++) {
			TileCoord tile = new TileCoord(random.nextInt(), random.nextInt());
			assertEquals(tile, HddTileList.decode(HddTileList.encode(tile)));
		}
	}

	@Test
	void iteratesInSpatialOrderRegardlessOfInsertionOrder() {
		int[][] offsets = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 0}, {3, 0}, {2, 1}, {3, 1},
				{0, 2}, {1, 2}, {0, 3}, {1, 3}, {2, 2}, {3, 2}, {2, 3}, {3, 3}};
		List<TileCoord> expected = new ArrayList<>();
		for (int[] origin : new int[][] {{-4, -4}, {0, -4}, {-4, 0}, {0, 0}})
			for (int[] offset : offsets)
				expected.add(new TileCoord(origin[0] + offset[0], origin[1] + offset[1]));
		try (HddTileListFactory factory = new HddTileListFactory(directory.toFile())) {
			HddTileList first = factory.createList();
			HddTileList second = factory.createList();
			expected.forEach(first::add);
			List<TileCoord> shuffled = new ArrayList<>(expected);
			Collections.shuffle(shuffled, new Random(7));
			shuffled.forEach(second::add);
			for (HddTileList list : List.of(first, second, first)) {
				List<TileCoord> actual = new ArrayList<>();
				list.forEach(actual::add);
				assertEquals(expected, actual);
			}
		}
	}

	@Test
	void sharesOneFileAndClosesListsIndependently() throws Exception {
		try (HddTileListFactory factory = new HddTileListFactory(directory.toFile())) {
			HddTileList first = factory.createList();
			HddTileList second = factory.createList();
			for (int i = 0; i < 64; i++)
				first.add(new TileCoord(i, -i));
			TileCoord extreme = new TileCoord(Integer.MIN_VALUE, Integer.MAX_VALUE);
			first.add(extreme);
			second.add(new TileCoord(Integer.MAX_VALUE, Integer.MIN_VALUE));
			assertTrue(first.contains(extreme));
			assertEquals(65, first.size());
			assertEquals(1, second.size());
			first.close();
			first.close();
			assertThrows(IllegalStateException.class, first::size);
			assertEquals(1, second.size());
			try (var paths = Files.walk(directory)) {
				assertEquals(List.of(directory.resolve("tileLists.mv")), paths.filter(Files::isRegularFile).toList());
			}
		}
		assertFalse(Files.exists(directory.resolve("tileLists.mv")));
	}

	@Test
	void clearsStaleDataAndReleasesHandlesForImmediateReuse() throws Exception {
		Path legacy = directory.resolve("autoList0/0/0/t_0_0.tile");
		Files.createDirectories(legacy.getParent());
		Files.createFile(legacy);
		Files.writeString(directory.resolve("tileLists.mv"), "interrupted render");
		HddTileListFactory factory = new HddTileListFactory(directory.toFile());
		HddTileList list;
		Iterator<TileCoord> iterator;
		try (factory) {
			assertFalse(Files.exists(legacy));
			assertFalse(Files.exists(directory.resolve("tileLists.mv")));
			list = factory.createList();
			iterator = list.iterator();
		}
		factory.close();
		list.close();
		assertThrows(IllegalStateException.class, factory::createList);
		assertThrows(IllegalStateException.class, iterator::hasNext);
		Files.delete(directory);
		try (HddTileListFactory retry = new HddTileListFactory(directory.toFile())) {
			assertEquals(0, retry.createList().size());
		}
	}

	@Test
	void reportsStoragePreparationErrors() throws Exception {
		Path file = directory.resolve("file");
		Files.createFile(file);
		assertThrows(UncheckedIOException.class, () -> new HddTileListFactory(file.resolve("lists").toFile()));
	}
}
