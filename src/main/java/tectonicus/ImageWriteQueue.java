/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.extern.slf4j.Slf4j;
import tectonicus.configuration.ImageFormat;
import tectonicus.util.BoundedExecutor;

import java.awt.image.BufferedImage;
import java.io.File;

@Slf4j
public class ImageWriteQueue
{
	private final BoundedExecutor executor;
	
	public ImageWriteQueue(final int numThreads)
	{
		executor = new BoundedExecutor("image-writer", numThreads, 16);
	}
	
	public void write(File outputFile, BufferedImage img, ImageFormat imageFormat, final float compressionLevel)
	{
		write(outputFile, img, imageFormat, compressionLevel, null);
	}

	/**
	 * Queues an image write. {@code onWritten} runs on the writer thread only after the image is on disk,
	 * so callers can record cache state that must not claim a file that was never written.
	 */
	public void write(File outputFile, BufferedImage img, ImageFormat imageFormat, final float compressionLevel, Runnable onWritten)
	{
		executor.execute("writing " + outputFile.getAbsolutePath(), () -> {
			// Screenshot.write logs its own errors and reports them by returning false.
			if (!Screenshot.write(outputFile, img, imageFormat, compressionLevel))
			{
				executor.recordFailure();
				return;
			}
			if (onWritten != null)
				onWritten.run();
		});
	}
	
	/** Waits for all queued writes and returns how many of them failed. */
	public int waitUntilFinished()
	{
		final int failed = executor.awaitCompletion();
		if (failed > 0)
			log.warn("{} image(s) could not be written; they will be retried on the next render", failed);
		return failed;
	}
}
