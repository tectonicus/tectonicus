/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tectonicus.configuration.ImageFormat;

public class ImageWriteQueue
{
	private ThreadPoolExecutor executor;
	
	public ImageWriteQueue(final int numThreads)
	{
		executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(16), new ResubmitHandler());
	}
	
	public void write(File outputFile, BufferedImage img, ImageFormat imageFormat, final float compressionLevel)
	{
		WriteTask task = new WriteTask(outputFile, img, imageFormat, compressionLevel);
		executor.submit(task);
	}
	
	public void waitUntilFinished()
	{
		try
		{
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.DAYS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	private static class WriteTask implements Callable<Void>
	{
		private final File outputFile;
		private final BufferedImage img;
		private final ImageFormat imageFormat;
		private final float compressionLevel;
		
		public WriteTask(File f, BufferedImage i, ImageFormat format, float compression)
		{
			this.outputFile = f;
			this.img = i;
			this.imageFormat = format;
			this.compressionLevel = compression;
		}
		
		@Override
		public Void call() throws Exception
		{
			Screenshot.write(outputFile, img, imageFormat, compressionLevel);
			return null;
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
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
