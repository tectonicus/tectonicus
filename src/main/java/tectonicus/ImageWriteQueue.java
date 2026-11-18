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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ImageWriteQueue
{
	private final ThreadPoolExecutor executor;
	private final AtomicInteger failures = new AtomicInteger();
	
	public ImageWriteQueue(final int numThreads)
	{
		executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(16), new ResubmitHandler());
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
		executor.execute(new WriteTask(outputFile, img, imageFormat, compressionLevel, onWritten));
	}
	
	/** Waits for all queued writes and returns how many of them failed. */
	public int waitUntilFinished()
	{
		try
		{
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.DAYS);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			log.error("Exception: ", e);
		}
		final int failed = failures.get();
		if (failed > 0)
			log.warn("{} image(s) could not be written; they will be retried on the next render", failed);
		return failed;
	}
	
	private class WriteTask implements Runnable
	{
		private final File outputFile;
		private final BufferedImage img;
		private final ImageFormat imageFormat;
		private final float compressionLevel;
		private final Runnable onWritten;
		
		WriteTask(File f, BufferedImage i, ImageFormat format, float compression, Runnable onWritten)
		{
			this.outputFile = f;
			this.img = i;
			this.imageFormat = format;
			this.compressionLevel = compression;
			this.onWritten = onWritten;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (!Screenshot.write(outputFile, img, imageFormat, compressionLevel))
				{
					failures.incrementAndGet();
					return;
				}
				if (onWritten != null)
					onWritten.run();
			}
			catch (RuntimeException e)
			{
				failures.incrementAndGet();
				log.error("Failed to finish writing {}", outputFile.getAbsolutePath(), e);
			}
		}
	}
	
	private static class ResubmitHandler implements RejectedExecutionHandler
	{
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
		{
			try
			{
				executor.getQueue().put(r);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				log.error("Exception: ", e);
			}
		}
	}
}
