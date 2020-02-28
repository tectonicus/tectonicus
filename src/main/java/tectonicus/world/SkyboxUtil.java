/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.experimental.UtilityClass;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.Geometry;

import java.util.ArrayList;

@UtilityClass
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
		
		ArrayList<Vector3f> circle = new ArrayList<>();
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
