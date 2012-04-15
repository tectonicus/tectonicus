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
package tectonicus.util;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector3f;

import tectonicus.renderer.Camera;

public class BoundingBox
{
	private Vector3l origin;
	private long width, height, depth;
	
	public BoundingBox(Vector3l origin, final long width, final long height, final long depth)
	{
		this.origin = new Vector3l(origin);
		this.width = width;
		this.height = height;
		this.depth = depth;
	}
	
	public boolean isVisible(Camera camera)
	{
		final float maxX = origin.x + width;
		final float maxY = origin.y + height;
		final float maxZ = origin.z + depth;
		
		// Bottom corners
		final int p0 = camera.classify(origin.x, origin.y, origin.z);
		final int p1 = camera.classify(maxX, origin.y, origin.z);
		final int p2 = camera.classify(origin.x, origin.y, maxZ);
		final int p3 = camera.classify(maxX, origin.y, maxZ);
		
		// Top corners
		final int p4 = camera.classify(origin.x, maxY, origin.z);
		final int p5 = camera.classify(maxX, maxY, origin.z);
		final int p6 = camera.classify(origin.x, maxY, maxZ);
		final int p7 = camera.classify(maxX, maxY, maxZ);
		
		
		return (p0 & p1 & p2 & p3 & p4 & p5 & p6 & p7) == 0; // All points in same region
	}
	
	/*
	public ArrayList<Vector3f> getSamplePoints()
	{
		ArrayList<Vector3f> result = new ArrayList<Vector3f>();
		
		final int subdivs = 10;
		final float xInc = width / (float)subdivs;
		final float yInc = height / (float)subdivs;
		final float zInc = depth / (float)subdivs;
		
		Log.logDebug("creating samples points with "+subdivs+" subdivs and steps: "+xInc+","+yInc+","+zInc);
		
		for (float x=0; x<=width; x+=xInc)
		{
			for (int y=0; y<=height; y+=yInc)
			{
				for (int z=0; z<=depth; z+=zInc)
				{
					result.add( new Vector3f(origin.x + x, origin.y + y, origin.z + z) );
				}
			}
		}
		
		Log.logDebug("created "+result.size()+" sample points");
		
		return result;
	}
	*/
	
	public ArrayList<Vector3f> getCornerPoints()
	{
		ArrayList<Vector3f> result = new ArrayList<Vector3f>();
		
		result.add( new Vector3f(origin.x,			origin.y,			origin.z) );
		result.add( new Vector3f(origin.x + width,	origin.y,			origin.z) );
		result.add( new Vector3f(origin.x,			origin.y + height,	origin.z) );
		result.add( new Vector3f(origin.x + width,	origin.y + height,	origin.z) );
		
		result.add( new Vector3f(origin.x,			origin.y,			origin.z + depth) );
		result.add( new Vector3f(origin.x + width,	origin.y,			origin.z + depth) );
		result.add( new Vector3f(origin.x,			origin.y + height,	origin.z + depth) );
		result.add( new Vector3f(origin.x + width,	origin.y + height,	origin.z + depth) );
		
		return result;
	}

	public Vector3f getCenter()
	{
		return new Vector3f(origin.x + width/2, origin.y + height/2, origin.z + depth/2);
	}
	
	public float getCenterX()
	{
		return origin.x + width/2;
	}
	
	public float getCenterY()
	{
		return origin.y + height/2;
	}
	
	public float getCenterZ()
	{
		return origin.z + depth/2;
	}

	public boolean contains(BoundingBox other)
	{
		return other.origin.x >= this.origin.x
				&& other.origin.y >= this.origin.y
				&& other.origin.z >= this.origin.z
				&& other.origin.x + other.width <= this.origin.x + this.width
				&& other.origin.y + other.height <= this.origin.y + this.height
				&& other.origin.z + other.depth <= this.origin.z + this.depth;
	}

	public Vector3l getOrigin()
	{
		return new Vector3l(origin);
	}
	
	public long getWidth() { return width; }
	public long getHeight() { return height; }
	public long getDepth() { return depth; }
}
