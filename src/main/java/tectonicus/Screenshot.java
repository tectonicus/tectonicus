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

import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Slf4j
public class Screenshot
{
	
	/** Encodes and writes the image, returning false (after logging) if it could not be written. */
	public static boolean write(File outputFile, BufferedImage img, ImageFormat imageFormat, final float compressionLevel)
	{
		ImageWriter writer = null;
		FileImageOutputStream output = null;
		try
		{
			if (imageFormat.isPng())
				img = dropAlphaIfOpaque(img);
			
			writer = imageFormat.createWriter();
			
			// First write to an in-memory stream
			ByteArrayOutputStream memOut = new ByteArrayOutputStream(img.getWidth() * img.getHeight() * 4);
			MemoryCacheImageOutputStream cacheOut = new MemoryCacheImageOutputStream(memOut);
			writer.setOutput(cacheOut);
			
			IIOImage image = new IIOImage(img, null, null);
			image.setMetadata(null);
			writer.write(null, image, imageFormat.getWriterParam(writer, compressionLevel));
			
			cacheOut.flush();
			
			outputFile.getParentFile().mkdirs();
			
			// Then write the encoded bytes to disk
			output = new FileImageOutputStream(outputFile);
			output.write(memOut.toByteArray(), 0, memOut.size());
                        output.flush();
			return true;
		}
		catch (Exception e)
		{
			log.error("Failed to write image {}", outputFile.getAbsolutePath(), e);
			return false;
		}
		finally
		{
			if (writer != null)
				writer.dispose();
			
			try
			{
				if (output != null)
					output.close();
			}
			catch (Exception e) {}
		}
	}
	
	/**
	 * Returns an RGB copy of an ABGR or ARGB image whose pixels are all fully opaque, or the image itself otherwise.
	 * PNGs without an alpha channel are smaller and encode no slower.
	 */
	static BufferedImage dropAlphaIfOpaque(BufferedImage img) {
		final int width = img.getWidth();
		final int height = img.getHeight();
		if (img.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
			byte[] abgr = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			for (int i = 0; i < abgr.length; i += 4) {
				if (abgr[i] != (byte) 0xff)
					return img;
			}
			BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			byte[] bgr = ((DataBufferByte) rgb.getRaster().getDataBuffer()).getData();
			for (int i = 0, j = 0; i < abgr.length; i += 4, j += 3) {
				bgr[j] = abgr[i + 1];
				bgr[j + 1] = abgr[i + 2];
				bgr[j + 2] = abgr[i + 3];
			}
			return rgb;
		}
		if (img.getType() == BufferedImage.TYPE_INT_ARGB) {
			int[] argb = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
			for (int pixel : argb) {
				if ((pixel >>> 24) != 0xff)
					return img;
			}
			BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			System.arraycopy(argb, 0, ((DataBufferInt) rgb.getRaster().getDataBuffer()).getData(), 0, argb.length);
			return rgb;
		}
		return img;
	}
}
