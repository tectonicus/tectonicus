/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import com.luciad.imageio.webp.WebPWriteParam;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

@RequiredArgsConstructor
public enum ImageFormat {
	PNG("png", true, true),
	JPG("jpg", false, false),
	GIF("gif", false, false),
	WEBP("webp", true, false);
	
	@Getter
	private final String extension;
	private final boolean hasAlpha;
	@Getter
	private final boolean png;
	
	public boolean hasAlpha() {
		return hasAlpha;
	}
	
	public ImageWriter createWriter() throws IOException {
		// New: return a new writer each time
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
		ImageWriterSpi spi = iter.next().getOriginatingProvider();
		return spi.createWriterInstance();
		
		// Original: one shared writer instance
		//	Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
		//	ImageWriter writer = iter.next();
		//	return writer;
	}
	
	/**
	 * ImageIO's PNG writer maps quality to a deflate level. 0.75 encodes map tiles about 1.3x faster than the
	 * default for about 5% larger files (about 1% smaller than the default once opaque tiles are written as RGB).
	 */
	static final float PNG_COMPRESSION_QUALITY = 0.75f;
	
	public ImageWriteParam getWriterParam(ImageWriter writer, final float compressionLevel) {
		ImageWriteParam param = writer.getDefaultWriteParam();
		if (png) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(PNG_COMPRESSION_QUALITY);
		} else if (extension.equals("jpg") || extension.equals("webp")) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			if (extension.equals("webp"))
				param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
			param.setCompressionQuality(compressionLevel);
		}
		return param;
	}
}
