/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.Region;
import tectonicus.SaveFormat;
import tectonicus.raw.RawChunk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

class RectangularRegionIteratorTests {

	private RectangularRegionIterator iterator;
	private File tempDir;
	private File regionDir;
	
	@BeforeEach
	void setUp(@TempDir Path tempPath) {
		tempDir = tempPath.toFile();
		regionDir = new File(tempDir, "region");
		regionDir.mkdirs();
	}

	private void createRegionFile(int regionX, int regionZ) throws IOException {
		File regionFile = new File(regionDir, "r." + regionX + "." + regionZ + ".mca");
		regionFile.createNewFile();
	}

	@Test
	void testConstructorInitializesBaseDirCorrectly() {
		// Create iterator with simple bounds
		iterator = new RectangularRegionIterator(
			tempDir, SaveFormat.ANVIL,
				-150, 150, -150, 150
		);
		
		assertThat(iterator.getBaseDir(), is(equalTo(tempDir)));
	}

	@Test
	void testNextReturnsNullWhenNoRegionsExist() {
		// Create iterator but don't create any region files
		iterator = new RectangularRegionIterator(
			tempDir, SaveFormat.ANVIL,
				0, 100, 0, 100
		);
		
		// Should return null since no region files exist
		File next;
		int attempts = 0;
		do {
			next = iterator.next();
			attempts++;
		} while (next != null && attempts < 100);
		
		assertThat(next, is(nullValue()));
	}

	@Test
	void testIterationSkipsNonexistentRegions() throws IOException {
		createRegionFile(0, 0);
		
		// Create iterator that spans multiple regions
		final long regionEdgeLength = Region.WIDTH_IN_CHUNKS * RawChunk.WIDTH;
		iterator = new RectangularRegionIterator(
			tempDir, SaveFormat.ANVIL,
				0, regionEdgeLength * 5, 0, regionEdgeLength * 5
		);
		
		// Collect files - should only get the one that exists
		int fileCount = 0;
		File currentFile;
		while ((currentFile = iterator.next()) != null && fileCount < 10) {
			assertThat(currentFile.getName(), is(equalTo("r.0.0.mca")));
			fileCount++;
		}
		
		assertThat(fileCount, is(equalTo(1)));
	}

	@Test
	void testIteratorWithSmallRectangle() throws IOException {
		createRegionFile(0, 0);
		createRegionFile(1, 0);
		createRegionFile(-1, 1);
		
		// Small rectangle within first region
		iterator = new RectangularRegionIterator(
			tempDir, SaveFormat.ANVIL,
				50, 250, 50, 250
		);
		
		// Should find region (0, 0)
		File foundFile = iterator.next();
		assertThat(foundFile, is(notNullValue()));
		assertThat(foundFile.getName(), is(equalTo("r.0.0.mca")));
	}

	@Test
	void testIteratorWithLargeRectangle() throws IOException {
		createRegionFile(-1, -1);
		createRegionFile(0, -1);
		createRegionFile(1, -1);
		createRegionFile(-1, 0);
		createRegionFile(0, 0);
		createRegionFile(1, 0);
		createRegionFile(-1, 1);
		createRegionFile(0, 1);
		createRegionFile(1, 1);
		
		final long regionEdgeLength = Region.WIDTH_IN_CHUNKS * RawChunk.WIDTH;
		
		// Large rectangle spanning multiple regions
		iterator = new RectangularRegionIterator(
			tempDir, SaveFormat.ANVIL,
				-regionEdgeLength, regionEdgeLength, -regionEdgeLength,
				regionEdgeLength
		);
		
		// Count found files
		int fileCount = 0;
		File currentFile;
		while ((currentFile = iterator.next()) != null && fileCount < 20) {
			assertThat(currentFile.exists(), is(true));
			fileCount++;
		}
		
		// Should find all 9 regions
		assertThat(fileCount, is(equalTo(9)));
	}
}
