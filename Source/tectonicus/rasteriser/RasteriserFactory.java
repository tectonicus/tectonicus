/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.rasteriser.jpct.JpctRasteriser;
import tectonicus.rasteriser.lwjgl.LwjglRasteriser;
import tectonicus.rasteriser.processing.ProcessingRasteriser;

public class RasteriserFactory
{
	
	public enum DisplayType
	{
		Window,
		Offscreen
	};
	
	public static Rasteriser createRasteriser(RasteriserType type, DisplayType displayType, final int displayWidth, final int displayHeight, final int colourDepth, final int alphaBits, final int depthBits, final int numSamples)
	{
		Rasteriser result = null;
		
		if (type == RasteriserType.Lwjgl)
		{
			try
			{
				result = new LwjglRasteriser(displayType, displayWidth, displayHeight, colourDepth, alphaBits, depthBits, numSamples);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (type == RasteriserType.Processing)
		{
			result = new ProcessingRasteriser(displayType, displayWidth, displayHeight);
		}
		else if (type == RasteriserType.Jpct)
		{
			result = new JpctRasteriser(displayType, displayWidth, displayHeight);
		}
		
		// Still null? Try the fallback option
		/*
		if (result == null)
		{
			result = new ProcessingRasteriser(displayType, displayWidth, displayHeight);
		}
		*/
		
		return result;
	}
	
}
