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
