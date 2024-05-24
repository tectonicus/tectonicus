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
import java.io.ByteArrayOutputStream;
import java.io.File;

@Slf4j
public class Screenshot
{
	
	public static void write(File outputFile, BufferedImage img, ImageFormat imageFormat, final float compressionLevel)
	{
		ImageWriter writer = null;
		FileImageOutputStream output = null;
		try
		{
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
		}
		catch (Exception e)
		{
			log.error("Exception: ", e);
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
}
