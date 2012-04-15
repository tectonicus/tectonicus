/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
