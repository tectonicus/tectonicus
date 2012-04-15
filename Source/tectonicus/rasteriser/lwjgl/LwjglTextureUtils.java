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
package tectonicus.rasteriser.lwjgl;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Hashtable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import tectonicus.rasteriser.TextureFilter;
import tectonicus.rasteriser.TextureFormat;

public class LwjglTextureUtils
{
	static ComponentColorModel glRGBAColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
             new int[] {8,8,8,8},
             true,
             false,
             ComponentColorModel.TRANSLUCENT,
             DataBuffer.TYPE_BYTE);
             
	static ComponentColorModel glRGBColorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
             new int[] {8,8,8,0},
             false,
             false,
             ComponentColorModel.OPAQUE,
             DataBuffer.TYPE_BYTE);
	
	public static BufferedImage createBufferedImage(final int width, final int height, TextureFormat format)
	{
		if (format == TextureFormat.RGB)
		{
			WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3, null);
			return new BufferedImage(LwjglTextureUtils.glRGBColorModel, raster, false, new Hashtable<String, Object>());
		}
		else if (format == TextureFormat.RGBA)
		{
			WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);
			return new BufferedImage(LwjglTextureUtils.glRGBAColorModel, raster, false, new Hashtable<String, Object>());
		}
		else
		{
			assert false;
			return null;
		}
	}
	
	public static TextureFormat findFormat(BufferedImage img)
	{
		final int numBands = img.getRaster().getNumBands();
		return (numBands == 3) ? TextureFormat.RGB : TextureFormat.RGBA;
	}
	
	public static BufferedImage convertToGlFormat(BufferedImage inImage)
	{
		// If already in a suitable colour model then just return the input unchanged
		if (inImage.getColorModel() == glRGBColorModel || inImage.getColorModel() == glRGBAColorModel)
			return inImage;
		
		TextureFormat format = LwjglTextureUtils.findFormat(inImage);
		BufferedImage outImage = LwjglTextureUtils.createBufferedImage(inImage.getWidth(), inImage.getHeight(), format);
		outImage.getGraphics().drawImage(inImage, 0, 0, null);
		
		return outImage;
	}
	
	public static int createTexture(BufferedImage imageData, TextureFilter filterMode)
	{
		imageData = convertToGlFormat(imageData);
		
		IntBuffer buff = BufferUtils.createIntBuffer(16);
		buff.limit(1);
		GL11.glGenTextures(buff);
		
		int textureId = buff.get();
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		if (filterMode == TextureFilter.NEAREST)
		{
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		}
		else
		{
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		}
		
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		
		ByteBuffer scratch = ByteBuffer.allocateDirect(4*imageData.getWidth()*imageData.getHeight());

		Raster raster = imageData.getRaster();
		byte data[] = (byte[])raster.getDataElements(0, 0, imageData.getWidth(), imageData.getHeight(), null);
		scratch.clear();
		scratch.put(data);
		scratch.rewind();
		
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,					// Mip level & Internal format
							imageData.getWidth(), imageData.getHeight(), 0,		// width, height, border
							GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,				// pixel data format
							scratch);											// pixel data
		
		GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
				0, 0,
				imageData.getWidth(), imageData.getHeight(),
				GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,			// format, type
				scratch);
		
		return textureId;
	}
	
	public static int createTexture(BufferedImage[] mips, TextureFilter filterMode)
	{
		IntBuffer buff = BufferUtils.createIntBuffer(16);
		buff.limit(1);
		GL11.glGenTextures(buff);
		
		int textureId = buff.get();
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		if (filterMode == TextureFilter.NEAREST)
		{
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
		}
		else
		{
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		}
		
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		
		for (int mip=0; mip<mips.length; mip++)
		{
			BufferedImage imageData = mips[mip];
			imageData = convertToGlFormat(imageData);
			
			ByteBuffer scratch = ByteBuffer.allocateDirect(4*imageData.getWidth()*imageData.getHeight());
	
			Raster raster = imageData.getRaster();
			byte data[] = (byte[])raster.getDataElements(0, 0, imageData.getWidth(), imageData.getHeight(), null);
			scratch.clear();
			scratch.put(data);
			scratch.rewind();
			
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, mip, GL11.GL_RGBA,				// Mip level & Internal format
								imageData.getWidth(), imageData.getHeight(), 0,		// width, height, border
								GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,				// pixel data format
								scratch);											// pixel data
			
		//	GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
		//			0, 0,
		//			imageData.getWidth(), imageData.getHeight(),
		//			GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,			// format, type
		//			scratch);
		}
		
		return textureId;
	}
}
