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
package tectonicus.world;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.Geometry;

public class SkyboxUtil
{
	public static Geometry generateDaySkybox(Rasteriser rasteriser)
	{
		Vector4f topColour = new Vector4f(0.3f, 0.3f, 1.0f, 1);
		Vector4f bottomColour = new Vector4f(1, 1, 1, 1);
		
		return generateSkybox(rasteriser, topColour, bottomColour);
	}
	
	public static Geometry generateNightSkybox(Rasteriser rasteriser)
	{
		Vector4f topColour = new Vector4f(0.0f, 0.0f, 0.0f, 1);
		Vector4f bottomColour = new Vector4f(0, 0, 0.4f, 1);
		
		return generateSkybox(rasteriser, topColour, bottomColour);
	}
	
	public static Geometry generateSkybox(Rasteriser rasteriser, Vector4f topColour, Vector4f bottomColour)
	{
		Geometry skybox = new Geometry(rasteriser, null);
		Mesh mesh = skybox.getBaseMesh();
		
		final float size = 10.0f;
		
		ArrayList<Vector3f> circle = new ArrayList<Vector3f>();
		final int numSubdivisions = 32;
		for (int i=0; i<=numSubdivisions; i++)
		{
			final float progress = (float)i / (float)numSubdivisions;
			final float angle = 2 * (float)Math.PI * progress;
			
			final float x = (float)Math.cos(angle) * size;
			final float z = (float)Math.sin(angle) * size;
			
			circle.add( new Vector3f(x, 0, z) );
		}
		
		// Sky tube sides
		for (int i=0; i<circle.size()-1; i++)
		{
			Vector3f p0 = circle.get(i);
			Vector3f p1 = circle.get(i+1);
			
			mesh.addVertex(new Vector3f(p0.x, -size, p0.z), bottomColour, 0, 0);
			mesh.addVertex(new Vector3f(p0.x, +size, p0.z), topColour, 0, 0);
			
			mesh.addVertex(new Vector3f(p1.x, +size, p1.z), topColour, 0, 0);
			mesh.addVertex(new Vector3f(p1.x, -size, p1.z), bottomColour, 0, 0);
		}
		
		for (int i=0; i<circle.size()-1; i++)
		{
			Vector3f p0 = circle.get(i);
			Vector3f p1 = circle.get(i+1);
			
			// Top circle
			mesh.addVertex(new Vector3f(0, size, 0), topColour, 0, 0);
			mesh.addVertex(new Vector3f(p1.x, size, p1.z), topColour, 0, 0);
			mesh.addVertex(new Vector3f(p0.x, size, p0.z), topColour, 0, 0);
			mesh.addVertex(new Vector3f(0, size, 0), topColour, 0, 0);

			// Bottom circle
			mesh.addVertex(new Vector3f(0, -size, 0),		bottomColour, 0, 0);
			mesh.addVertex(new Vector3f(p0.x, -size, p0.z), bottomColour, 0, 0);
			mesh.addVertex(new Vector3f(p1.x, -size, p1.z), bottomColour, 0, 0);
			mesh.addVertex(new Vector3f(0, -size, 0),		bottomColour, 0, 0);
		}
		
		mesh.finalise();
		
		return skybox;
	}
	
	public static Geometry generateSkyboxOld(Rasteriser rasteriser)
	{
		Geometry skybox = new Geometry(rasteriser, null);
		Mesh mesh = skybox.getBaseMesh();
		
		final float size = 10.0f;
		
		Vector4f topColour = new Vector4f(0.3f, 0.3f, 1.0f, 1);
		Vector4f bottomColour = new Vector4f(1, 1, 1, 1);
		
		// Floor
		mesh.addVertex(new Vector3f(-size, -size, -size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(size, -size, -size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(size, -size, size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, -size, size), bottomColour, 0, 0);
		
		// Ceiling
		mesh.addVertex(new Vector3f(-size, size, size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(size, size, size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(size, size, -size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, size, -size), topColour, 0, 0);
		
		// North
		mesh.addVertex(new Vector3f(-size, size, size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, size, -size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, -size, -size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, -size, size), bottomColour, 0, 0);
		
		// South
		mesh.addVertex(new Vector3f(size, size, -size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(size, size, size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(size, -size, size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(size, -size, -size), bottomColour, 0, 0);
		
		// East
		mesh.addVertex(new Vector3f(-size, size, -size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(size, size, -size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(size, -size, -size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, -size, -size), bottomColour, 0, 0);
		
		// West
		mesh.addVertex(new Vector3f(size, size, size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, size, size), topColour, 0, 0);
		mesh.addVertex(new Vector3f(-size, -size, size), bottomColour, 0, 0);
		mesh.addVertex(new Vector3f(size, -size, size), bottomColour, 0, 0);
		
		mesh.finalise();
		
		return skybox;
	}
}
