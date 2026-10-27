/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.configuration.ImageFormat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmptyTileTest {
	@TempDir Path directory;

	@Test
	void transparencyRequiresEveryPixelToHaveExactlyZeroAlpha() {
		for (int type : new int[] {BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_INT_ARGB_PRE}) {
			BufferedImage image = new BufferedImage(16, 16, type);
			assertTrue(ImageUtils.isFullyTransparent(image));
			image.setRGB(15, 15, 0x01010203);
			assertFalse(ImageUtils.isFullyTransparent(image));
			image.setRGB(15, 15, 0x00010203);
			assertTrue(ImageUtils.isFullyTransparent(image));
		}
		assertFalse(ImageUtils.isFullyTransparent(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)));
	}

	@Test
	void retiresOnlyObsoleteTileFilesAndReturnsTheirCoordinatesForDownsampling() throws Exception {
		Path tiles = directory.resolve("Zoom4");
		TileCoord keep = new TileCoord(-1, 2), retire = new TileCoord(3, -4);
		Path kept = TileRenderer.getImageFile(tiles.toFile(), keep.x, keep.y, ImageFormat.PNG).toPath();
		Path retired = TileRenderer.getImageFile(tiles.toFile(), retire.x, retire.y, ImageFormat.PNG).toPath();
		Files.createDirectories(kept.getParent());
		Files.createDirectories(retired.getParent());
		Files.createFile(kept);
		Files.createFile(retired);
		Path unrelated = Files.createFile(tiles.resolve("notes.txt"));
		HddTileList visible = new HddTileList(directory.resolve("visible").toFile());
		visible.add(keep);
		HddTileList removed = new HddTileList(directory.resolve("removed").toFile());
		TileOutput.removeObsolete(tiles, visible, ImageFormat.PNG, removed);
		assertTrue(Files.exists(kept));
		assertTrue(Files.exists(unrelated));
		assertFalse(Files.exists(retired));
		assertEquals(Set.of(retire), removed.toSet());
	}
}
