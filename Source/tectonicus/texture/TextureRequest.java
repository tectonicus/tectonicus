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