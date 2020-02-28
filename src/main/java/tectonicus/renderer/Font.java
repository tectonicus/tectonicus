/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
