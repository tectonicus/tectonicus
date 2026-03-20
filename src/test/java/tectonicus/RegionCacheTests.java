/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class RegionCacheTests {
	@Test
	void detectsAlphaWhenRegionDirectoryIsMissing(@TempDir Path tempPath) {
		RegionCache cache = new RegionCache(tempPath.toFile());
		assertThat(cache.getFormat(), is(SaveFormat.ALPHA));
	}

	@Test
	void detectsAnvilWhenMcaFilesExist(@TempDir Path tempPath) throws IOException {
		File regionDir = new File(tempPath.toFile(), "region");
		regionDir.mkdirs();
		new File(regionDir, "r.0.0.mca").createNewFile();

		RegionCache cache = new RegionCache(tempPath.toFile());
		assertThat(cache.getFormat(), is(SaveFormat.ANVIL));
	}

	@Test
	void detectsMcRegionWhenOnlyMcrFilesExist(@TempDir Path tempPath) throws IOException {
		File regionDir = new File(tempPath.toFile(), "region");
		regionDir.mkdirs();
		new File(regionDir, "r.0.0.mcr").createNewFile();

		RegionCache cache = new RegionCache(tempPath.toFile());
		assertThat(cache.getFormat(), is(SaveFormat.MC_REGION));
	}
}
