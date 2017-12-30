/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import tectonicus.cache.swap.HddTileList;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;

public class Downsampler
{
	private ChangeFile changedFileList;
	
	private ThreadPoolExecutor executor;
	
	public Downsampler(final int numThreads, ChangeFile changedFileList)
	{
		this.changedFileList = changedFileList;
		
		// 1 thread  = 1m 52s
		// 2 threads = 1m 9s
		// 3 threads = 1m 9s 
		
		executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(64), new ResubmitHandler());
	}
	
	public void downsample(File inputDir, File outputDir, HddTileList tiles, Layer layer, final int tileWidth, final int tileHeight, ProgressListener progressListener)
	{
		int count = 0;
		
		Shared state = new Shared(inputDir, outputDir, layer.getImageFormat(), layer.getImageCompressionLevel(), layer.getBackgroundColorRGB(), tileWidth, tileHeight);
		
		for (TileCoord tile : tiles)
		{
			DownsampleTask task = new DownsampleTask(tile, state, changedFileList);
			executor.submit(task);
			
			count++;
			if (count % 20 == 0)
			{
				final int percentage = (int)Math.floor((count / (float)tiles.size()) * 100);
				System.out.print(percentage+"%\r"); //prints a carraige return after line
			}
			progressListener.onTaskUpdate(count, tiles.size());
		}
		
		System.out.println("100%");
		
		try
		{
			System.out.println("Finalizing downsampling...");
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.DAYS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		System.out.println("Downsampling complete");
	}
	
	private static BufferedImage getTile(File file)
	{
		try
		{
			BufferedImage img = ImageIO.read(file);
			return img;
		}
		catch (Exception e)
		{
		//	e.printStackTrace();
		}
		return null;
	}
	
	private static class Shared
	{
		public final File inputDir;
		public final File outputDir;
		
		public final int tileWidth;
		public final int tileHeight;
		
		public final ImageFormat imageFormat;
		public final float imageCompressionLevel;
		public final Color backgroundColor;
		
		public Shared(File inputDir, File outputDir, ImageFormat format, float compressionLevel, Color backgroundColor, final int tileWidth, final int tileHeight)
		{
			this.inputDir = inputDir;
			this.outputDir = outputDir;
			
			this.tileWidth = tileWidth;
			this.tileHeight = tileHeight;
			
			this.imageFormat = format;
			this.imageCompressionLevel = compressionLevel;
			this.backgroundColor = backgroundColor;
		}
	}
	
	private static class DownsampleTask implements Callable<Void>
	{
		private final TileCoord tile;
		private final Shared state;
		private final ChangeFile changedFileList;
		
		public DownsampleTask(TileCoord tile, Shared state, ChangeFile changedFileList)
		{
			this.tile = tile;
			this.state = state;
			this.changedFileList = changedFileList;
		}
		
		@Override
		public Void call() throws Exception
		{
			// Find the four input files
			BufferedImage in00 = getTile( TileRenderer.getImageFile(state.inputDir, tile.x * 2, tile.y * 2, state.imageFormat) );
			BufferedImage in10 = getTile( TileRenderer.getImageFile(state.inputDir, tile.x * 2 + 1, tile.y * 2, state.imageFormat) );
			BufferedImage in01 = getTile( TileRenderer.getImageFile(state.inputDir, tile.x * 2, tile.y * 2 + 1, state.imageFormat) );
			BufferedImage in11 = getTile( TileRenderer.getImageFile(state.inputDir, tile.x * 2 + 1, tile.y * 2 + 1, state.imageFormat) );
			
			if (in00 == null && in10 == null && in01 == null && in11 == null)
				return null;
			
			Log.logDebug("\tDownsampling to create meta tile at "+tile.x+","+tile.y);
			
			final int pixelFormat = state.imageFormat.hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
			BufferedImage outImg = new BufferedImage(state.tileWidth, state.tileHeight, pixelFormat);
			Graphics2D g = (Graphics2D)outImg.getGraphics();
			
			g.setColor(state.backgroundColor);
			g.fillRect(0, 0, state.tileWidth, state.tileHeight);
			
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			final int halfWidth = state.tileWidth / 2;
			final int halfHeight = state.tileHeight / 2;
			
			if (in00 != null)
				g.drawImage(in00, 0, 0, halfWidth, halfHeight, null);
			if (in10 != null)
				g.drawImage(in10, halfWidth, 0, halfWidth, halfHeight, null);
			if (in01 != null)
				g.drawImage(in01, 0, halfHeight, halfWidth, halfHeight, null);
			if (in11 != null)
				g.drawImage(in11, halfWidth, halfHeight, halfWidth, halfHeight, null);
			
			try
			{
				File outputFile = TileRenderer.getImageFile(state.outputDir, tile.x, tile.y, state.imageFormat);
				
				Screenshot.write(outputFile, outImg, state.imageFormat, state.imageCompressionLevel);
				
				changedFileList.writeLine( outputFile.getAbsolutePath() );
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
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
