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

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.renderer.Font;
import tectonicus.texture.SubTexture;

public class TextLayout
{
	private final Font font;
	
	private SubMesh subMesh;
	
	public TextLayout(Font font)
	{
		this.font = font;
		
		subMesh = new SubMesh();
	}
	
	public void setText(String text, final float baseX, final float baseY, final float baseZ, final boolean center)
	{
		// TODO: Kerning is slightly different from minecraft here, but it'll do for now.
		
		subMesh = new SubMesh();
		
		float x = baseX;
		float y = baseY;
		float z = baseZ;
		
		final float width = 1.0f / 10.0f;
		final float height = width;
		final float space = - 1.0f / 30.0f;
		
		if (center)
		{
			// Find width of text
			final float totalWidth = text.length() * (width + space);
			x -= (totalWidth / 2.0f);
		}
		
		for (int i=0; i<text.length(); i++)
		{
			final char ch = text.charAt(i);
			
			SubTexture subTex = font.getCharacter(ch);
			
			if (subTex != null)
			{
				subMesh.addQuad(new Vector3f(x, y+height, z), new Vector3f(x+width, y+height, z), new Vector3f(x+width, y, z), new Vector3f(x, y, z),
								new Vector4f(0, 0, 0, 1), subTex);
			}
			
			x += width + space;
		}
	}
	
	public void pushTo(Mesh mesh, final float x, final float y, final float z, Rotation rotation, final float angleInDegs)
	{
		subMesh.pushTo(mesh, x, y, z, rotation, angleInDegs);
	}
	
	public void pushTo(SubMesh mesh, final float x, final float y, final float z)
	{
		this.subMesh.pushTo(mesh, x, y, z);
	}
	
}
