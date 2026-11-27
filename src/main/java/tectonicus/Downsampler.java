/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tectonicus.cache.TileCache;
import tectonicus.cache.swap.HddTileList;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LayerConfig;
import tectonicus.util.BoundedExecutor;
import tectonicus.util.ImageUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;

@Slf4j
public class Downsampler {
	private final ChangeFile changedFileList;
	private final BoundedExecutor executor;
	
	public Downsampler(final int numThreads, ChangeFile changedFileList) {
		this.changedFileList = changedFileList;
		executor = new BoundedExecutor("downsampler", numThreads, 64);
	}
	
	public void downsample(File inputDir, File outputDir, HddTileList tiles, LayerConfig layer, final int tileWidth, final int tileHeight, ProgressListener progressListener, TileCache tileCache, int zoomLevel) {
		int count = 0;
		
		Shared state = new Shared(inputDir, outputDir, layer.getImageFormat(), layer.getImageCompressionLevel(), layer.getBackgroundColorRGB(), tileWidth, tileHeight, zoomLevel);
		
		for (TileCoord tile : tiles) {
			DownsampleTask task = new DownsampleTask(tile, state, changedFileList, tileCache, executor);
			executor.execute("downsampling tile " + tile.x + "," + tile.y + " at zoom " + zoomLevel, task);
			
			count++;
			if (count % 128 == 0) {
				final int percentage = (int)Math.floor((count / (float)tiles.size()) * 100);
				System.out.print("\t" + percentage + "%\r"); //prints a carriage return after line
			}
			progressListener.onTaskUpdate(count, tiles.size());
		}
		
		System.out.println("\t100%");
		
		log.debug("\tFinalizing downsampling...");
		final int failed = executor.awaitCompletion();
		if (failed > 0)
			log.warn("{} tile(s) at zoom {} could not be downsampled; they will be retried on the next render", failed, zoomLevel);
		
		log.debug("\tDownsampling complete");
	}

	@RequiredArgsConstructor
	private static class Shared {
		public final File inputDir;
		public final File outputDir;

		public final ImageFormat imageFormat;
		public final float imageCompressionLevel;
		public final Color backgroundColor;

		public final int tileWidth;
		public final int tileHeight;
		public final int zoomLevel;
	}

	@RequiredArgsConstructor
	private static class DownsampleTask implements Runnable {
		// A task runs on one thread from start to finish and writes its tile before returning,
		// so each thread decodes into and draws on the same images from one tile to the next.
		private static final ThreadLocal<BufferedImage[]> INPUT_IMAGES = ThreadLocal.withInitial(() -> new BufferedImage[4]);
		private static final ThreadLocal<BufferedImage> OUTPUT_IMAGE = new ThreadLocal<>();

		private final TileCoord tile;
		private final Shared state;
		private final ChangeFile changedFileList;
		private final TileCache tileCache;
		private final BoundedExecutor executor;
		
		@Override
		public void run() {
			try {
				downsample();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private void downsample() throws IOException {
			// Find the four input files
			BufferedImage[] inputs = new BufferedImage[4];
			for (int i = 0; i < inputs.length; i++) {
				File file = TileRenderer.getImageFile(state.inputDir, tile.x * 2 + i % 2, tile.y * 2 + i / 2, state.imageFormat);
				inputs[i] = getTile(file, i);
				// Unreadable files must not be mistaken for deliberately omitted empty children.
				if (inputs[i] == null && file.exists()) {
					log.warn("Skipping tile {},{} at zoom {} because {} could not be read", tile.x, tile.y, state.zoomLevel, file.getAbsolutePath());
					executor.recordFailure();
					return;
				}
			}
			BufferedImage in00 = inputs[0], in10 = inputs[1], in01 = inputs[2], in11 = inputs[3];
			
			File outputFile = TileRenderer.getImageFile(state.outputDir, tile.x, tile.y, state.imageFormat);
			if (in00 == null && in10 == null && in01 == null && in11 == null) {
				Files.deleteIfExists(outputFile.toPath());
				tileCache.updateTileDownsampleStatus(tile, state.zoomLevel, true);
				return;
			}

			if (state.imageFormat.hasAlpha() && isEmpty(in00) && isEmpty(in10) && isEmpty(in01) && isEmpty(in11)) {
				Files.deleteIfExists(outputFile.toPath());
				tileCache.updateTileDownsampleStatus(tile, state.zoomLevel, true);
				return;
			}
			
			log.trace("\tDownsampling to create meta tile at "+tile.x+","+tile.y);
			
			final boolean hasAlpha = state.imageFormat.hasAlpha();
			final int pixelFormat = hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
			BufferedImage outImg = OUTPUT_IMAGE.get();
			if (outImg == null || outImg.getType() != pixelFormat
					|| outImg.getWidth() != state.tileWidth || outImg.getHeight() != state.tileHeight) {
				outImg = new BufferedImage(state.tileWidth, state.tileHeight, pixelFormat);
				OUTPUT_IMAGE.set(outImg);
			} else if (hasAlpha) {
				// Quadrants without an input tile must stay transparent rather than show the previous tile.
				Arrays.fill(((DataBufferByte) outImg.getRaster().getDataBuffer()).getData(), (byte) 0);
			}
			Graphics2D g = (Graphics2D)outImg.getGraphics();
			
			if (!hasAlpha) {
				g.setColor(state.backgroundColor);
				g.fillRect(0, 0, state.tileWidth, state.tileHeight);
			}
			
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
			g.dispose();
			
			boolean empty = state.imageFormat.hasAlpha() && ImageUtils.isFullyTransparent(outImg);
			if (empty) {
				Files.deleteIfExists(outputFile.toPath());
			} else {
				// Leave the tile pending so the next render retries a failed write.
				if (!Screenshot.write(outputFile, outImg, state.imageFormat, state.imageCompressionLevel)) {
					executor.recordFailure();
					return;
				}
				changedFileList.writeLine(outputFile.getAbsolutePath());
			}
			tileCache.updateTileDownsampleStatus(tile, state.zoomLevel, empty);
		}

		private static boolean isEmpty(BufferedImage image) {
			return image == null || ImageUtils.isFullyTransparent(image);
		}
		
		/** Reads a tile, decoding into this thread's image for the given quadrant when it has the same layout. */
		private static BufferedImage getTile(File file, int quadrant) {
			try {
				// Keep stream-based WebP decoding without ImageIO's temporary disk cache.
				try (var inputStream = new FileInputStream(file);
					 var imageInput = new MemoryCacheImageInputStream(inputStream)) {
					Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
					if (!readers.hasNext())
						return null;

					ImageReader reader = readers.next();
					try {
						var readParam = reader.getDefaultReadParam();
						reader.setInput(imageInput, true, true);
						BufferedImage[] reusable = INPUT_IMAGES.get();
						if (canDecodeInto(reader, reusable[quadrant]))
							readParam.setDestination(reusable[quadrant]);
						BufferedImage image = reader.read(0, readParam);
						reusable[quadrant] = image;
						return image;
					} finally {
						reader.dispose();
					}
				}
			} catch (FileNotFoundException e) {
				return null;
			} catch (Exception e) {
				log.error("Error getting tile {}", file.getAbsolutePath(), e);
			}
			return null;
		}

		/**
		 * Whether the reader would decode into an image laid out like {@code image} anyway, so decoding into it gives
		 * the same pixels. Indexed images are never reused, since each file has its own palette.
		 */
		private static boolean canDecodeInto(ImageReader reader, BufferedImage image) throws IOException {
			if (image == null || reader.getWidth(0) != image.getWidth() || reader.getHeight(0) != image.getHeight())
				return false;
			int type = image.getType();
			if (type != BufferedImage.TYPE_3BYTE_BGR && type != BufferedImage.TYPE_4BYTE_ABGR
					&& type != BufferedImage.TYPE_INT_RGB && type != BufferedImage.TYPE_INT_ARGB
					&& type != BufferedImage.TYPE_BYTE_GRAY)
				return false;
			Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
			return types.hasNext() && types.next().getBufferedImageType() == type;
		}
	}
}
