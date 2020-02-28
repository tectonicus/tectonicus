/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

public class TextureRequest
{
	public final String path;
	
	public final boolean isTileRequest;
	public final int tileX, tileY;
	
	public TextureRequest(String path, String params)
	{
		this.path = path;
		
		if (params == null || params.equals(""))
		{
			// full texture
			isTileRequest = false;
			tileX = tileY = -1;
		}
		else if (params.startsWith("["))
		{
			isTileRequest = true;
			
			final int comma = params.indexOf(',');
			String first = params.substring(1, comma).trim();
			String second = params.substring(comma+1, params.length()-1).trim();
			
			tileX = Integer.parseInt(first);
			tileY = Integer.parseInt(second);
		}
		else
		{
			isTileRequest = false;
			tileX = tileY = -1;
		}
	}
	
	public TextureRequest(String path, final int tileX, final int tileY)
	{
		this.path = path;
		this.tileX = tileX;
		this.tileY = tileY;
		this.isTileRequest = true;
	}
	
	public boolean isFullTexture()
	{
		return !isTileRequest;
	}
	
	public boolean isTile()
	{
		return isTileRequest;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		TextureRequest other = (TextureRequest)obj;
		
		return this.path.equals(other.path)
				&& this.isTileRequest == other.isTileRequest
				&& this.tileX == other.tileX
				&& this.tileY == other.tileY;
	}
	
	@Override
	public int hashCode()
	{
		return this.path.hashCode() ^ tileX ^ tileY;
	}
}
