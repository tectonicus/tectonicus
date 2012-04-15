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