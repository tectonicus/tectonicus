/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

public enum ImageFormat
{
	Png("png", true, true),
	Jpg("jpg", false, false),
	Gif("gif", false, false);
	
	private String extension;
	private boolean hasAlpha;
	private boolean isPng;
	
	private ImageFormat(String ext, final boolean hasAlpha, final boolean isPng)
	{
		this.extension = ext;
		this.hasAlpha = hasAlpha;
		this.isPng = isPng;
	}
	
	public String getExtension() { return extension; }
	
	public boolean hasAlpha() { return hasAlpha; }
	
	public boolean isPng() { return isPng; }
	
	public ImageWriter createWriter() throws IOException
	{
		// New: return a new writer each time
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
		ImageWriterSpi spi = iter.next().getOriginatingProvider();
		return spi.createWriterInstance();
		
		// Original: one shared writer instance
	//	Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
	//	ImageWriter writer = iter.next();
	//	return writer;
	}
	
	public ImageWriteParam getWriterParam(ImageWriter writer, final float compressionLevel)
	{
		ImageWriteParam param = writer.getDefaultWriteParam();
		if (extension.equalsIgnoreCase("jpg"))
		{
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			// 0.95 gives high quality but still results in lower file size than png
		//	param.setCompressionQuality(0.95f);
			param.setCompressionQuality(compressionLevel);
		}
		return param;
	}
}
