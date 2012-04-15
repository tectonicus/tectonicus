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
package tectonicus.rasteriser.processing;

import java.util.Arrays;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import processing.core.PGraphics;
import processing.core.PGraphics3D;

import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;

public class ProcessingMesh implements Mesh
{
	private final PGraphics3D graphics;
	private final ProcessingTexture texture;
	
	private float[] xPositions, yPositions, zPositions;
	private float[] reds, greens, blues, alphas;
	private float[] uCoords, vCoords;
	
	private int numVerts;
	
	public ProcessingMesh(ProcessingTexture texture, PGraphics3D graphics)
	{
		this.graphics = graphics;
		this.texture = texture;
		
		final int initialCapacity = 64;
		
		xPositions = new float[initialCapacity];
		yPositions = new float[initialCapacity];
		zPositions = new float[initialCapacity];
		
		reds = new float[initialCapacity];
		greens = new float[initialCapacity];
		blues = new float[initialCapacity];
		alphas = new float[initialCapacity];
		
		uCoords = new float[initialCapacity];
		vCoords = new float[initialCapacity];
	}
	
	public void destroy() {}
	
	private void ensureCapacity(final int numVerts)
	{
		xPositions = Arrays.copyOf(xPositions, numVerts);
		yPositions = Arrays.copyOf(yPositions, numVerts);
		zPositions = Arrays.copyOf(zPositions, numVerts);
		
		reds = Arrays.copyOf(reds, numVerts);
		greens = Arrays.copyOf(greens, numVerts);
		blues = Arrays.copyOf(blues, numVerts);
		alphas = Arrays.copyOf(alphas, numVerts);
		
		uCoords = Arrays.copyOf(uCoords, numVerts);
		vCoords = Arrays.copyOf(vCoords, numVerts);
	}
	
	@Override
	public void finalise()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Texture getTexture()
	{
		return null;
	}

	@Override
	public void bind()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void draw(final float xOffset, final float yOffset, final float zOffset)
	{
		assert (texture.getPImage() != null);
		
		// TODO: Apply offset to transform matrix rather than doing addition manually
		
		graphics.beginShape(PGraphics.QUADS);
		
		graphics.texture(texture.getPImage());
		graphics.noSmooth();
		graphics.noStroke();
		graphics.noLights();
		
		for (int i=0; i<numVerts; i++)
		{
			graphics.tint(reds[i], greens[i], blues[i], alphas[i]);
		//	graphics.fill(reds[i], greens[i], blues[i], alphas[i]);			
		//	graphics.fill(reds[i]*255.0f, greens[i]*255.0f, blues[i]*255.0f, alphas[i]*255.0f);
			
		//	graphics.vertex(xPositions[i], yPositions[i], zPositions[i], uCoords[i], vCoords[i]);
			graphics.vertex(xPositions[i] + xOffset, yPositions[i] + yOffset, zPositions[i] + zOffset, uCoords[i], vCoords[i]);
		}
		
		graphics.endShape();
	}

	@Override
	public int getMemorySize()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTotalVertices()
	{
		return numVerts;
	}

	@Override
	public void addVertex(Vector3f position, Vector4f colour, float u, float v)
	{
		if (numVerts+1 == xPositions.length)
			ensureCapacity(numVerts + 1024);
		
		xPositions[numVerts] = position.x;
		yPositions[numVerts] = position.y;
		zPositions[numVerts] = position.z;
		
		reds[numVerts] = colour.x;
		greens[numVerts] = colour.y;
		blues[numVerts] = colour.z;
		alphas[numVerts] = colour.w;
		
		uCoords[numVerts] = u;
		vCoords[numVerts] = v;
		
		numVerts++;
	}

	@Override
	public void addVertex(Vector3f position, final float u, final float v)
	{
		addVertex(position, new Vector4f(1, 1, 1, 1), u, v);
	}
	
}
