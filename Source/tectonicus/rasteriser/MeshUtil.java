/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.blockTypes.BlockUtil;
import tectonicus.texture.SubTexture;

public class MeshUtil
{

	public static void addCube(final float x, final float y, final float z, Vector4f colour, final boolean addTop,
								final boolean addNorth, final boolean addSouth, final boolean addEast, final boolean addWest,
								Mesh geometry)
	{
		final float inc = 0.2f;
		Vector4f lightColour = new Vector4f(colour.x + inc, colour.y + inc, colour.z + inc, colour.w);
		Vector4f darkColour = new Vector4f(colour.x - inc, colour.y - inc, colour.z - inc, colour.w);
		
		BlockUtil.clamp(lightColour);
		BlockUtil.clamp(darkColour);
		
		// Top
		if (addTop)
		{
			geometry.addVertex(new Vector3f(x, y+1, z), colour, 0, 0);
			geometry.addVertex(new Vector3f(x+1, y+1, z), colour, 1, 0);
			geometry.addVertex(new Vector3f(x+1, y+1, z+1), colour, 1, 1);
			geometry.addVertex(new Vector3f(x, y+1, z+1), colour, 0, 1);
		}
		
		// North face
		if (addNorth)
		{
			geometry.addVertex(new Vector3f(x, y, z), darkColour, 0, 0);
			geometry.addVertex(new Vector3f(x, y+1, z), darkColour, 1, 0);
			geometry.addVertex(new Vector3f(x, y+1, z+1), darkColour, 1, 1);
			geometry.addVertex(new Vector3f(x, y, z+1), darkColour, 0, 1);	
		}
		
		// South
		if (addSouth)
		{
			geometry.addVertex(new Vector3f(x+1, y, z), darkColour, 0, 0);
			geometry.addVertex(new Vector3f(x+1, y, z+1), darkColour, 0, 1);
			geometry.addVertex(new Vector3f(x+1, y+1, z+1), darkColour, 1, 1);
			geometry.addVertex(new Vector3f(x+1, y+1, z), darkColour, 1, 0);
		}
		
		// East
		if (addEast)
		{
			geometry.addVertex(new Vector3f(x+1, y+1, z), lightColour, 1, 1);
			geometry.addVertex(new Vector3f(x, y+1, z), lightColour, 0, 1);
			geometry.addVertex(new Vector3f(x, y, z), lightColour, 0, 0);
			geometry.addVertex(new Vector3f(x+1, y, z), lightColour, 1, 0);
		}
		
		// West
		if (addWest)
		{
			geometry.addVertex(new Vector3f(x, y, z+1), lightColour, 0, 0);
			geometry.addVertex(new Vector3f(x, y+1, z+1), lightColour, 0, 1);
			geometry.addVertex(new Vector3f(x+1, y+1, z+1), lightColour, 1, 1);
			geometry.addVertex(new Vector3f(x+1, y, z+1), lightColour, 1, 0);
		}
	}

	public static void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture)
	{
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
	}
	
	public static void addDoubleSidedQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture)
	{
		// Clockwise
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
		
		// Anticlockwise
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
	}
	
	public static void addDoubleSidedQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		// Clockwise
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
		
		// Anticlockwise
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
	}

	public static void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
	}
	
}
