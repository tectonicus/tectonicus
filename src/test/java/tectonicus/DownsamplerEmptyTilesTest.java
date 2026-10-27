/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.cache.NullTileCache;
import tectonicus.cache.swap.HddTileList;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.MutableLayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DownsamplerEmptyTilesTest {
	@TempDir Path directory;
	private final TileCoord parent = new TileCoord(-2, 3);
	private final AtomicReference<Boolean> emptyStatus = new AtomicReference<>();

	@Test
	void missingChildrenRemoveAnOldParentAndFinishItsCacheEntry() throws Exception {
		Path output = output();
		Files.createDirectories(output.getParent());
		Files.writeString(output, "stale parent");
		run();
		assertFalse(Files.exists(output));
		assertEquals(Boolean.TRUE, emptyStatus.get());
		assertEquals("", Files.readString(directory.resolve("changed.txt")));
	}

	@Test
	void transparentChildrenAreNotEncodedOrReportedAsFilesToUpload() throws Exception {
		writeChild(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
		run();
		assertFalse(Files.exists(output()));
		assertEquals(Boolean.TRUE, emptyStatus.get());
		assertEquals("", Files.readString(directory.resolve("changed.txt")));
	}

	@Test
	void visiblePixelsCreateAnImageAndClearEmptyStatus() throws Exception {
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 64; x++)
			for (int y = 0; y < 64; y++)
				image.setRGB(x, y, 0x0200ff00);
		writeChild(image);
		run();
		assertTrue(Files.exists(output()));
		assertEquals(Boolean.FALSE, emptyStatus.get());
		assertFalse(Files.readString(directory.resolve("changed.txt")).isEmpty());
	}

	@Test
	void unreadableChildrenAreRetriedAndDoNotDeleteOldParents() throws Exception {
		Path child = child();
		Files.createDirectories(child.getParent());
		Files.writeString(child, "corrupt");
		Files.createDirectories(output().getParent());
		Files.writeString(output(), "old parent");
		run();
		assertNull(emptyStatus.get());
		assertEquals("old parent", Files.readString(output()));
	}

	private void writeChild(BufferedImage image) throws Exception {
		Files.createDirectories(child().getParent());
		ImageIO.write(image, "png", child().toFile());
	}

	private Path child() {
		return TileRenderer.getImageFile(directory.resolve("input").toFile(), parent.x * 2, parent.y * 2, ImageFormat.PNG).toPath();
	}

	private Path output() {
		return TileRenderer.getImageFile(directory.resolve("output").toFile(), parent.x, parent.y, ImageFormat.PNG).toPath();
	}

	private void run() {
		HddTileList tiles = new HddTileList(directory.resolve("tiles").toFile());
		tiles.add(parent);
		MutableLayer layer = new MutableLayer("day", "map");
		layer.setBackgroundColor("#123456");
		ChangeFile changes = new ChangeFile(directory.resolve("changed.txt").toFile());
		try {
			new Downsampler(1, changes).downsample(directory.resolve("input").toFile(), directory.resolve("output").toFile(),
					tiles, layer, 64, 64, new ProgressListener() {
						public void onTaskStarted(String task) {}
						public void onTaskUpdate(int count, int total) {}
					}, new NullTileCache() {
						@Override
						public void updateTileDownsampleStatus(TileCoord tile, int zoom, boolean empty) {
							emptyStatus.set(empty);
						}
					}, 0);
		} finally { changes.close(); }
	}
}
