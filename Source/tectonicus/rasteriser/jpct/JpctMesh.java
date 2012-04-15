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
package tectonicus.rasteriser.jpct;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;

public class JpctMesh implements Mesh
{
	private World world;

	private Object3D object3D;
	
	private ArrayList<Vector3f> verts;
	private ArrayList<Vector4f> colours;
	private ArrayList<Vector2f> texCoords;
	
	public JpctMesh(World world, JpctTexture texture)
	{
		this.world = world;
		
		verts = new ArrayList<Vector3f>();
		colours = new ArrayList<Vector4f>();
		texCoords = new ArrayList<Vector2f>();
		
		object3D = new Object3D(1024);

	//	obj.setBaseTexture("box");
		object3D.setCulling(false);
	}
	
	@Override
	public void destroy()
	{
		world.removeObject(object3D);
	}
	
	@Override
	public void finalise()
	{
		object3D.build();
	}
	
	@Override
	public Texture getTexture()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void bind()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void draw(float xOffset, float yOffset, float zOffset)
	{
		world.addObject(object3D);
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
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void addVertex(Vector3f position, Vector4f colour, float u, float v)
	{
		verts.add(position);
		colours.add(colour);
		texCoords.add(new Vector2f(u, v));
		
		if (verts.size() == 4)
		{
			final int textureId = 0; // TODO!
			
			Vector3f v0 = verts.get(0);
			Vector3f v1 = verts.get(1);
			Vector3f v2 = verts.get(2);
			Vector3f v3 = verts.get(3);
			
		//	Vector4f c0 = colours.get(0);
		//	Vector4f c1 = colours.get(1);
		//	Vector4f c2 = colours.get(2);
		//	Vector4f c3 = colours.get(3);
			
			Vector2f uv0 = texCoords.get(0);
			Vector2f uv1 = texCoords.get(1);
			Vector2f uv2 = texCoords.get(2);
			Vector2f uv3 = texCoords.get(3);
			
			// TODO: Figure out how to add vertex colours
			
			object3D.addTriangle(	new SimpleVector(v0.x, v0.y, v0.z), uv0.x, uv0.y,
									new SimpleVector(v1.x, v1.y, v1.z), uv1.x, uv1.y,
									new SimpleVector(v2.x, v2.y, v2.z), uv2.x, uv2.y,
									textureId);
			
			object3D.addTriangle(	new SimpleVector(v2.x, v2.y, v2.z), uv2.x, uv2.y,
									new SimpleVector(v3.x, v3.y, v3.z), uv3.x, uv3.y,
									new SimpleVector(v0.x, v0.y, v0.z), uv0.x, uv0.y,
									textureId);
			
			verts.clear();
			colours.clear();
			texCoords.clear();
		}
	}
	
	@Override
	public void addVertex(Vector3f position, float u, float v)
	{
		addVertex(position, new Vector4f(1, 1, 1, 1), u, v);
	}
	
	public Object3D getObject3D()
	{
		return object3D;
	}
}
