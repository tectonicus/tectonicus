/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.test;

import java.awt.Color;

import tectonicus.InteractiveRenderer;
import tectonicus.TectonicusApp;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.renderer.OrthoCamera;

public class RasteriserTest
{
	public static void main(String[] args)
	{
		TectonicusApp.unpackLwjgl(false, false);
		
		Rasteriser rasteriser = RasteriserFactory.createRasteriser(RasteriserType.JPCT, DisplayType.Window, 512, 512, 24, 0, 16, 0);
		
		OrthoCamera camera = new OrthoCamera(rasteriser, 512, 512);
		
		while (!rasteriser.isCloseRequested())
		{
			rasteriser.beginFrame();
			
			rasteriser.resetState();
			rasteriser.clear(new Color(100, 128, 255, 0));
			
			camera.lookAt(0, 0, 0, 8, (float)Math.PI/4, (float)Math.PI/4);
			camera.apply();
			
			InteractiveRenderer.drawAxies(rasteriser);
			
			InteractiveRenderer.drawChunkCheckerboard(rasteriser);
			
			rasteriser.sync();
		}
		
		rasteriser.destroy();
	}
}
