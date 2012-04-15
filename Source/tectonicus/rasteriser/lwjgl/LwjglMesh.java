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
package tectonicus.rasteriser.lwjgl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;

public class LwjglMesh implements Mesh
{
	private LwjglTexture texture;
	
	private FloatBuffer vertices;
	private ByteBuffer colours;
	private FloatBuffer texCoords;
	
	private int numVertices;
	private int maxVertices;
	
	private boolean hasDisplayList;
	private int displayList;
	
	private boolean isFinalised;
	
	public LwjglMesh(LwjglTexture texture)
	{
		this.texture = texture;
	}
	
	@Override
	public void destroy()
	{
		if (hasDisplayList)
		{
			GL11.glDeleteLists(displayList, 1);
			displayList = 0;
			hasDisplayList = false;
		}
	}
	
	@Override
	public void addVertex(Vector3f position, Vector4f colour, final float u, final float v)
	{
		assert(!isFinalised);
		
		if (numVertices + 3 > maxVertices)
		{
			final int newMax = maxVertices + 2000;
			
			vertices = realloc(vertices, maxVertices * 3, newMax * 3);
			colours = realloc(colours, maxVertices * 4, newMax * 4);
			texCoords = realloc(texCoords, maxVertices * 2, newMax * 2);
			
			maxVertices = newMax;
		}
		
		vertices.put(position.x);
		vertices.put(position.y);
		vertices.put(position.z);
		
		colours.put( (byte)(colour.x * 255) );
		colours.put( (byte)(colour.y * 255) );
		colours.put( (byte)(colour.z * 255) );
		colours.put( (byte)(colour.w * 255) );
		
		texCoords.put(u);
		texCoords.put(v);
		
		numVertices++;
		assert(numVertices < maxVertices);
	}
	
	@Override
	public void addVertex(Vector3f position, final float u, final float v)
	{
		assert(!isFinalised);
		
		vertices.put(position.x);
		vertices.put(position.y);
		vertices.put(position.z);
		
		colours.put((byte)255);
		colours.put((byte)255);
		colours.put((byte)255);
		colours.put((byte)255);
		
		texCoords.put(u);
		texCoords.put(v);
		
		numVertices++;
		assert(numVertices < maxVertices);
	}
	
	@Override
	public void finalise()
	{
		assert (!isFinalised);
		
		if (vertices != null)
			vertices.flip();
		
		if (colours != null)
			colours.flip();
		
		if (texCoords != null)
			texCoords.flip();
		
		isFinalised = true;
	}
	
	@Override
	public Texture getTexture()
	{
		return texture;
	}
	
	@Override
	public void bind()
	{
		if (vertices == null)
			return;
		
		assert (isFinalised);
		assert (numVertices > 0);
		
		org.lwjgl.opengl.Util.checkGLError();
		
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glVertexPointer(3, 0, vertices);
		
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glColorPointer(4, true, 0, colours);
		
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glTexCoordPointer(2, 0, texCoords);
		
		if (texture != null)
		{
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
		}
		else
		{
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}
		
		org.lwjgl.opengl.Util.checkGLError();
	}
	
	@Override
	public void draw(final float xOffset, final float yOffset, final float zOffset)
	{
		if (vertices == null)
			return;
		
		assert (numVertices > 0);
		
	//	if (hasDisplayList)
		{
	//		GL11.glCallList(displayList);
		}
	//	else
		{
	//		org.lwjgl.opengl.Util.checkGLError();
			
		//	displayList = GL11.glGenLists(1);
			
		//	GL11.glNewList(displayList, GL11.GL_COMPILE);
			{
				GL11.glPushMatrix();
				GL11.glTranslatef(xOffset, yOffset, zOffset);
				
				GL11.glDrawArrays(GL11.GL_QUADS, 0, numVertices);
				
				GL11.glPopMatrix();
			}
		//	GL11.glEndList();
			
		//	GL11.glCallList(displayList);
			
		//	hasDisplayList = true;
			
		//	org.lwjgl.opengl.Util.checkGLError();
		}
	}
	
	@Override
	public int getMemorySize()
	{
		if (vertices == null)
			return 0;
		
		final int vertexMem = vertices.capacity() * 4;
		final int coloursMem = colours.capacity();
		final int texCoordMem = texCoords.capacity() * 4;
		
		return vertexMem + coloursMem + texCoordMem;
	}

	@Override
	public int getTotalVertices()
	{
		return numVertices;
	}
	
	private static FloatBuffer realloc(FloatBuffer existing, final int existingSize, final int newSize)
	{
		FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newSize);
		
		if (existing != null)
		{
			existing.flip();
			newBuffer.put(existing);
		}
		
		return newBuffer;
	}

	private static ByteBuffer realloc(ByteBuffer existing, final int existingSize, final int newSize)
	{
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newSize);
		
		if (existing != null)
		{
			existing.flip();
			newBuffer.put(existing);
		}
		
		return newBuffer;
	}
}
