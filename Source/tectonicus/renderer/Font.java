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
package tectonicus.renderer;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.texture.SubTexture;

public class Font
{
	private Texture texture;
	
	private Map<Character, SubTexture> characterMap;
	
	public Font(Rasteriser rasteriser, BufferedImage fontSheet, InputStream fontLookup)
	{
		this.texture = rasteriser.createTexture(fontSheet, TextureFilter.NEAREST);
		
		this.characterMap = new HashMap<Character, SubTexture>();
		
		int x = 0;
		int y = 2;
		
		final float texelU = 1.0f / 16.0f;
		final float texelV = 1.0f / 16.0f;
		
		final float epsilon = 0.001f;
		
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(fontLookup, Charset.forName("UTF-8")));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				if (line != null && line.length() > 0 && !line.startsWith("#"))
				{
					for (int c=0; c<line.length(); c++)
					{
						final char ch = line.charAt(c);
						SubTexture sub = new SubTexture(texture, x*texelU+epsilon, y*texelV+epsilon, (x+1)*texelU-epsilon, (y+1)*texelV-epsilon);
						characterMap.put(ch, sub);
						x++;
					}
					x = 0;
					y++;
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public Texture getTexture()
	{
		return texture;
	}
	
	public SubTexture getCharacter(final char ch)
	{
		return characterMap.get(ch);
	}
}
