/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
