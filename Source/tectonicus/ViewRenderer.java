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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import tectonicus.ViewUtil.Viewpoint;
import tectonicus.cache.FileViewCache;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LightStyle;
import tectonicus.configuration.ViewConfig;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.PerspectiveCamera;
import tectonicus.world.Sign;
import tectonicus.world.World;

//	get image format out of map config node
//	extract view height offset and elevation angle from sign text

public class ViewRenderer
{
	private final Rasteriser rasteriser;
	private final FileViewCache viewCache;	
	private final int numDownsampleThreads;
	private final ViewConfig viewConfig;
	
	public ViewRenderer(Rasteriser rasteriser, FileViewCache viewCache, final int numDownsampleThreads, ViewConfig viewConfig)
	{
		this.rasteriser = rasteriser;
		this.viewCache = viewCache;
		this.numDownsampleThreads = numDownsampleThreads;
		this.viewConfig = viewConfig;
	}
	
	public void output(World world, File mapDir, File viewsFile, ChangeFile changedFiles)
	{
		File viewsDir = new File(mapDir, "Views");
		viewsDir.mkdirs();
		
		// Find changed views
		File changedViews = viewCache.findChangedViews(rasteriser, world, viewsFile, viewsDir, viewConfig.getImageFormat(), viewConfig.getViewDistance());
		
		// Output changed views
		
		// TODO: Load custom blocks here
		
		world.flushChunkCache();
		world.flushGeometryCache();
		world.setLightStyle(LightStyle.Day);
		draw(viewCache, world, mapDir, changedViews, viewsDir, viewConfig, LightStyle.Day, changedFiles);
		
		world.flushChunkCache();
		world.flushGeometryCache();
		world.setLightStyle(LightStyle.Night);
		draw(viewCache, world, mapDir, changedViews, viewsDir, viewConfig, LightStyle.Night, changedFiles);
		
		System.out.println("View rendering done!");
	}
	
	private void draw(FileViewCache viewCache, World world, File mapDir, File viewsFile, File viewsDir, ViewConfig viewConfig, LightStyle lightStyle, ChangeFile changedFiles)
	{
		System.out.println("Drawing "+lightStyle+" views...");
		
		final ImageFormat imageFormat = viewConfig.getImageFormat();
		final float imageCompression = viewConfig.getImageCompressionLevel();
		
		HddObjectListReader<Sign> viewsIn = null;
		try
		{
			ImageWriteQueue imageWriteQueue = new ImageWriteQueue(numDownsampleThreads);
			
			viewsIn = new HddObjectListReader<Sign>(viewsFile);
			Sign sign = new Sign();
			while (viewsIn.hasNext())
			{
				viewsIn.read(sign);
				
				if (sign.getText(3).contains("home"))
				{
					System.out.println("problem sign");
				}
				
				if (ViewUtil.parseLightStyle(sign) != lightStyle)
					continue;
				
				Viewpoint view = ViewUtil.findView(sign);
				
				// TODO: Store size of object list in file so we can see how far through the list we are?
				// May need to have separate day/night lists for this so it's accurate
				
				System.out.println("Drawing view at ("+view.lookAt.x+", "+view.lookAt.y+", "+view.lookAt.x+")");
				
				PerspectiveCamera perspectiveCamera = ViewUtil.createCamera(rasteriser, view, viewConfig.getViewDistance());
				perspectiveCamera.apply();
				
				rasteriser.resetState();
				rasteriser.clear(new Color(0, 0, 0));
				rasteriser.setViewport(0, 0, ViewUtil.viewWidth, ViewUtil.viewHeight);
				
				world.draw(perspectiveCamera, true, false);
				
				
				BufferedImage tileImage = rasteriser.takeScreenshot(0, 0, ViewUtil.viewWidth, ViewUtil.viewHeight, imageFormat);
				if (tileImage != null)
				{
					File outputFile = ViewUtil.createViewFile(viewsDir, sign, imageFormat);
					
					writeScaled(tileImage, ViewUtil.viewWidth/2, ViewUtil.viewHeight/2, outputFile, imageFormat, imageCompression, imageWriteQueue);
					
					changedFiles.writeLine( outputFile.getAbsolutePath() );
				}
				else
				{
					System.err.println("Error: Rasteriser.takeScreenshot gave us a null image (format:"+imageFormat+")");
				}
				
				viewCache.writeHash(sign, rasteriser, world, viewConfig.getViewDistance());
			}
			
			imageWriteQueue.waitUntilFinished();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (viewsIn != null)
				viewsIn.close();
		}
	}
	
	private void writeScaled(BufferedImage originalImage, final int newWidth, final int newHeight, File outputFile, final ImageFormat imageFormat, final float imageCompression, ImageWriteQueue imageWriteQueue)
	{
		final int pixelFormat = imageFormat.hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
		
		BufferedImage outImg = new BufferedImage(ViewUtil.viewWidth/2, ViewUtil.viewHeight/2, pixelFormat);
		Graphics2D g = (Graphics2D)outImg.getGraphics();
		g.setColor(TileRenderer.clearColour);
		g.fillRect(0, 0, outImg.getWidth(), outImg.getHeight());
		
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.drawImage(originalImage, 0, 0, ViewUtil.viewWidth/2, ViewUtil.viewHeight/2, null);
		
		imageWriteQueue.write(outputFile, outImg, imageFormat, imageCompression);
	}
}
