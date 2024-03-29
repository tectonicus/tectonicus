/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.view;

import lombok.extern.log4j.Log4j2;
import tectonicus.ChangeFile;
import tectonicus.ImageWriteQueue;
import tectonicus.TileRenderer;
import tectonicus.view.ViewUtil.Viewpoint;
import tectonicus.cache.FileViewCache;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LightStyle;
import tectonicus.configuration.ViewConfig;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.PerspectiveCamera;
import tectonicus.world.Sign;
import tectonicus.world.World;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

//	get image format out of map config node
//	extract view height offset and elevation angle from sign text

@Log4j2
public class ViewRenderer
{
        public static final byte SAMPLES = 4;
    
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
		ChangedViews changedViews = viewCache.findChangedViews(rasteriser, world, viewsFile, viewsDir, viewConfig);
		
		// Output changed views
		if (changedViews.getCount() > 0) {
			// TODO: Load custom blocks here
			log.info("Creating fallback block registry for views");
			world.loadBlockRegistry(null, true);

			world.flushChunkCache();
			world.flushGeometryCache();
			world.setLightStyle(LightStyle.Day);
			draw(viewCache, world, changedViews.getViewsFile(), viewsDir, viewConfig, LightStyle.Day, changedFiles);

			world.flushChunkCache();
			world.flushGeometryCache();
			world.setLightStyle(LightStyle.Night);
			draw(viewCache, world, changedViews.getViewsFile(), viewsDir, viewConfig, LightStyle.Night, changedFiles);

			log.info("View rendering done!");
		}
	}
	
	private void draw(FileViewCache viewCache, World world, File viewsFile, File viewsDir, ViewConfig viewConfig, LightStyle lightStyle, ChangeFile changedFiles)
	{
		log.info("Drawing {} views...", lightStyle);
		
		final ImageFormat imageFormat = viewConfig.getImageFormat();
		final float imageCompression = viewConfig.getImageCompressionLevel();
		
		HddObjectListReader<Sign> viewsIn = null;
		try
		{
			ImageWriteQueue imageWriteQueue = new ImageWriteQueue(numDownsampleThreads);
			
			viewsIn = new HddObjectListReader<>(viewsFile);
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
				
				if (view.getFov() < 30 || view.getFov() > 110) { //if FOV is not set on sign or is in invalid range use config file FOV
					view.setFov(viewConfig.getFOV());
				}
				
				// TODO: Store size of object list in file so we can see how far through the list we are?
				// May need to have separate day/night lists for this so it's accurate
				
				log.info("Drawing view at ({}, {}, {})", view.getLookAt().x, view.getLookAt().y, view.getLookAt().z);
				
				PerspectiveCamera perspectiveCamera = ViewUtil.createCamera(rasteriser, view, viewConfig);
				perspectiveCamera.apply();
				
                                final int supersampledWidth = viewConfig.getWidth()*SAMPLES;
                                final int supersampledHeight = viewConfig.getHeight()*SAMPLES;
                                final int displayWidth = rasteriser.getDisplayWidth();
                                final int displayHeight = rasteriser.getDisplayHeight();
                                
                                // Determine the number of sections to capture based on the window size and desired capture dimensions
                                int numHorizontalSections = (int) Math.ceil((double) supersampledWidth / displayWidth);
                                int numVerticalSections = (int) Math.ceil((double) supersampledHeight / displayHeight);

                                // Create an image to hold the entire screenshot
                                final int pixelFormat = viewConfig.getImageFormat().hasAlpha() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;

                                BufferedImage viewImage = new BufferedImage(viewConfig.getWidth(), viewConfig.getHeight(), pixelFormat);
                                Graphics2D viewGraphics = (Graphics2D)viewImage.getGraphics();
                                viewGraphics.setColor(TileRenderer.clearColour);
                                viewGraphics.fillRect(0, 0, viewImage.getWidth(), viewImage.getHeight());

                                viewGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                viewGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                                viewGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                                // Iterate over each section and capture it
                                for (int y = 0; y < numVerticalSections; y++) {
                                        for (int x = 0; x < numHorizontalSections; x++) {
                                                // Calculate the dimensions and starting coordinates of the section to capture
                                                int sectionWidth = Math.min(displayWidth, supersampledWidth - x * displayWidth);
                                                int sectionHeight = Math.min(displayHeight, supersampledHeight - y * displayHeight);
                                                int sectionStartX = x * displayWidth;
                                                int sectionStartY = y * displayHeight;

                                                // Capture the section
                                                rasteriser.resetState();
                                                rasteriser.clear(new Color(0, 0, 0));
                                                rasteriser.setViewport(-sectionStartX, -sectionStartY, supersampledWidth, supersampledHeight);

                                                world.draw(perspectiveCamera, true, false);

                                                BufferedImage viewSection = rasteriser.takeScreenshot(0, 0, sectionWidth, sectionHeight, imageFormat);
                                                if (viewSection == null) {
                                                        log.error("Error: Rasteriser.takeScreenshot gave us a null image (format: {})", imageFormat);
                                                        break;
                                                }

                                                // Draw the section onto the final image
                                                viewGraphics.drawImage(
                                                        viewSection,
                                                        sectionStartX/SAMPLES,
                                                        (supersampledHeight-sectionStartY-sectionHeight)/SAMPLES,
                                                        sectionWidth/SAMPLES,
                                                        sectionHeight/SAMPLES,
                                                        null);
                                        }
                                }

                                File outputFile = ViewUtil.createViewFile(viewsDir, sign, imageFormat);
                                imageWriteQueue.write(outputFile, viewImage, imageFormat, imageCompression);
                                
				viewCache.writeHash(sign, rasteriser, world, viewConfig);
			}
			
			imageWriteQueue.waitUntilFinished();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (viewsIn != null)
				viewsIn.close();
		}
	}
}
