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
package tectonicus.rasteriser;

import java.util.ArrayList;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.texture.SubTexture;

public class SubMesh
{
	public enum Rotation
	{
		None,
		Clockwise,
		AntiClockwise
	};
	
	private ArrayList<Vector3f> positions;
	private ArrayList<Vector2f> texCoords;
	private ArrayList<Vector4f> colours;
	
	public SubMesh()
	{
		positions = new ArrayList<Vector3f>();
		texCoords = new ArrayList<Vector2f>();
		colours = new ArrayList<Vector4f>();
	}
	
	public void addVertex(Vector3f position, Vector4f colour, final float u, final float v)
	{
		positions.add(new Vector3f(position));
		texCoords.add(new Vector2f(u, v));
		colours.add(new Vector4f(colour));
	}
	
	public void addQuad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture)
	{
		addVertex(p0, colour, texture.u0, texture.v0);
		addVertex(p1, colour, texture.u1, texture.v0);
		addVertex(p2, colour, texture.u1, texture.v1);
		addVertex(p3, colour, texture.u0, texture.v1);
	}
	
	public void addQuad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		addVertex(p0, colour, uv0.x, uv0.y);
		addVertex(p1, colour, uv1.x, uv1.y);
		addVertex(p2, colour, uv2.x, uv2.y);
		addVertex(p3, colour, uv3.x, uv3.y);
	}
	
	public void pushTo(Mesh mesh, final float xOffset, final float yOffset, final float zOffset, Rotation rotation, final float angleDeg)
	{
		pushTo(mesh, xOffset, yOffset, zOffset, rotation, angleDeg, Rotation.None, 0);
	}
	public void pushTo(Mesh mesh, final float xOffset, final float yOffset, final float zOffset, Rotation horizRotation, final float horizAngleDeg, Rotation vertRotation, final float vertAngleDeg)
	{
		Matrix4f transform = createTransform(horizRotation, horizAngleDeg, vertRotation, vertAngleDeg);
		
		for (int i=0; i<positions.size(); i++)
		{
			Vector3f pos = new Vector3f( positions.get(i) );
			Vector2f tex = texCoords.get(i);
			Vector4f col = colours.get(i);
			
			if (transform != null)
			{
				Vector4f dest = new Vector4f();
				Matrix4f.transform(transform, new Vector4f(pos.x, pos.y, pos.z, 1.0f), dest);
				
				pos.x = dest.x / dest.w;
				pos.y = dest.y / dest.w;
				pos.z = dest.z / dest.w;
			}
			
			pos.x += xOffset;
			pos.y += yOffset;
			pos.z += zOffset;
			
			mesh.addVertex(pos, col, tex.x, tex.y);
		}
	}
	
	public void pushTo(SubMesh destMesh, final float xOffset, final float yOffset, final float zOffset, Rotation horizRotation, final float horizAngleDeg, Rotation vertRotation, final float vertAngleDeg)
	{
		Matrix4f transform = createTransform(horizRotation, horizAngleDeg, vertRotation, vertAngleDeg);
		
		for (int i=0; i<positions.size(); i++)
		{
			Vector3f pos = new Vector3f( positions.get(i) );
			
			if (transform != null)
			{
				Vector4f dest = new Vector4f();
				Matrix4f.transform(transform, new Vector4f(pos.x, pos.y, pos.z, 1.0f), dest);
				
				pos.x = dest.x / dest.w;
				pos.y = dest.y / dest.w;
				pos.z = dest.z / dest.w;
			}
			
			destMesh.positions.add( new Vector3f(positions.get(i) ));
			destMesh.colours.add( new Vector4f(colours.get(i) ));
			destMesh.texCoords.add( new Vector2f(texCoords.get(i) ));
		}
	}
	
	public void pushTo(SubMesh dest, final float xOffset, final float yOffset, final float zOffset)
	{
		for (int i=0; i<positions.size(); i++)
		{
			dest.positions.add( new Vector3f(positions.get(i) ));
			dest.colours.add( new Vector4f(colours.get(i) ));
			dest.texCoords.add( new Vector2f(texCoords.get(i) ));
		}
	}
	
	private static Matrix4f createTransform(Rotation horizontalRotation, final float horizontalAngleDeg,
											Rotation verticalRotation, final float verticalAngleDeg)
	{
		if (horizontalRotation == Rotation.None && verticalRotation == Rotation.None)
			return null;
		
		float horizontalAngleInRads = horizontalAngleDeg / 360.0f * 2.0f * (float)Math.PI;
		if (horizontalRotation == Rotation.AntiClockwise)
			horizontalAngleInRads *= -1.0f;
		
		float verticalAngleInRads = verticalAngleDeg / 360.0f * 2.0f * (float)Math.PI;
		if (verticalRotation == Rotation.AntiClockwise)
			verticalAngleInRads *= -1.0f;
		
		Matrix4f trans0 = new Matrix4f();
		trans0.translate(new Vector3f(+0.5f, +0.5f, +0.5f));
		
		Matrix4f horizontalRotate = new Matrix4f();
		horizontalRotate.rotate(horizontalAngleInRads, new Vector3f(0, 1, 0));
		
		Matrix4f verticalRotate = new Matrix4f();
		verticalRotate.rotate(verticalAngleInRads, new Vector3f(0, 0, 1));
		
		Matrix4f trans1 = new Matrix4f();
		trans1.translate(new Vector3f(-0.5f, -0.5f, -0.5f));
		
		Matrix4f combinedRotate = new Matrix4f();
		Matrix4f.mul(horizontalRotate, verticalRotate, combinedRotate);
		
		Matrix4f working = new Matrix4f();
		Matrix4f.mul(trans0, combinedRotate, working);
		
		Matrix4f actual = new Matrix4f();
		Matrix4f.mul(working, trans1, actual);
		
		
		
	/*	Matrix4f working = new Matrix4f();
		Matrix4f.mul(trans0, rotate, working);
		
		Matrix4f actual = new Matrix4f();
		Matrix4f.mul(working, trans1, actual);
	*/
		
		return actual;
	}
	
	
	public static void addBlock(SubMesh subMesh, final float x, final float y, final float z,
								final float width, final float height, final float depth,
								Vector4f colour, SubTexture sideTex, SubTexture topTex, SubTexture bottomTex)
	{
		// North
		subMesh.addQuad( new Vector3f(x, y+height, z), new Vector3f(x, y+height, z+depth),
						new Vector3f(x, y, z+depth),  new Vector3f(x, y, z), 
						colour, sideTex);
		
		// East
		subMesh.addQuad( new Vector3f(x+width, y+height, z), new Vector3f(x, y+height, z),
						new Vector3f(x, y, z),  new Vector3f(x+width, y, z), 
						colour, sideTex);
		
		// West
		subMesh.addQuad( new Vector3f(x, y+height, z+depth), new Vector3f(x+width, y+height, z+depth),
						new Vector3f(x+width, y, z+depth),  new Vector3f(x, y, z+depth), 
						colour, sideTex);
		
		// South
		subMesh.addQuad(new Vector3f(x+width, y+height, z+depth), new Vector3f(x+width, y+height, z),
						new Vector3f(x+width, y, z), new Vector3f(x+width, y, z+depth), 
						colour, sideTex);
		
		// Top
		subMesh.addQuad(new Vector3f(x, y+height, z), new Vector3f(x+width, y+height, z),
						new Vector3f(x+width, y+height, z+depth), new Vector3f(x, y+height, z+depth), 
						colour, topTex);
		
		// Bottom
		subMesh.addQuad(new Vector3f(x, y, z), new Vector3f(x, y, z+depth),
									new Vector3f(x+width, y, z+depth), new Vector3f(x+width, y, z), 
									colour, bottomTex);
	}
}
