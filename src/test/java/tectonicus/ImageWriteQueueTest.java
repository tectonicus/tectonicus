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
import tectonicus.configuration.ImageFormat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageWriteQueueTest {
	@Test
	void runsCompletionOnlyForImagesThatReachDisk(@TempDir Path dir) throws Exception {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		File written = dir.resolve("0/0/tile_0_0.png").toFile();
		// A regular file where a directory is needed makes the second write fail.
		Path blocker = Files.writeString(dir.resolve("blocker"), "not a directory");
		File unwritable = new File(blocker.toFile(), "tile_1_0.png");
		List<File> completed = Collections.synchronizedList(new ArrayList<>());

		ImageWriteQueue queue = new ImageWriteQueue(2);
		queue.write(written, image, ImageFormat.PNG, 1.0f, () -> completed.add(written));
		queue.write(unwritable, image, ImageFormat.PNG, 1.0f, () -> completed.add(unwritable));

		assertEquals(1, queue.waitUntilFinished());
		assertTrue(written.isFile());
		assertFalse(unwritable.exists());
		assertEquals(List.of(written), completed);
	}
}
