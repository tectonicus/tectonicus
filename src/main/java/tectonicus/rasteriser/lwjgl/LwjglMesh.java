/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import com.jogamp.opengl.GL2;
import lombok.RequiredArgsConstructor;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;
import tectonicus.util.Colour4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@RequiredArgsConstructor
public class LwjglMesh implements Mesh
{
	private final LwjglTexture texture;
	
	private FloatBuffer vertices;
	private ByteBuffer colours;
	private FloatBuffer texCoords;
	
	private int numVertices;
	private int maxVertices;
	private float[] faceCentres;
	
	private boolean hasDisplayList;
	private int displayList;
	
	private boolean isFinalised;
	
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
			final int newMax = maxVertices + 4000;
			
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
	public void addVertex(org.joml.Vector3f position, Colour4f color, final float u, final float v)
	{
		assert(!isFinalised);
		
		if (numVertices + 3 > maxVertices)
		{
			final int newMax = maxVertices + 4000;
			
			vertices = realloc(vertices, maxVertices * 3, newMax * 3);
			colours = realloc(colours, maxVertices * 4, newMax * 4);
			texCoords = realloc(texCoords, maxVertices * 2, newMax * 2);
			
			maxVertices = newMax;
		}
		
		vertices.put(position.x);
		vertices.put(position.y);
		vertices.put(position.z);
		
		colours.put( (byte)(color.r * 255) );
		colours.put( (byte)(color.g * 255) );
		colours.put( (byte)(color.b * 255) );
		colours.put( (byte)(color.a * 255) );
		
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

		faceCentres = new float[(numVertices / 4) * 3];
		for (int face = 0; face < numVertices / 4; face++) {
			int vertex = face * 4;
			faceCentres[face * 3] = (vertices.get(vertex * 3) + vertices.get((vertex + 1) * 3) + vertices.get((vertex + 2) * 3) + vertices.get((vertex + 3) * 3)) * 0.25f;
			faceCentres[face * 3 + 1] = (vertices.get(vertex * 3 + 1) + vertices.get((vertex + 1) * 3 + 1) + vertices.get((vertex + 2) * 3 + 1) + vertices.get((vertex + 3) * 3 + 1)) * 0.25f;
			faceCentres[face * 3 + 2] = (vertices.get(vertex * 3 + 2) + vertices.get((vertex + 1) * 3 + 2) + vertices.get((vertex + 2) * 3 + 2) + vertices.get((vertex + 3) * 3 + 2)) * 0.25f;
		}
		
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
		
		//org.lwjgl.opengl.Util.checkGLError();
		
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, vertices);
		
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, colours);
		
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, texCoords);
		
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
		
		//org.lwjgl.opengl.Util.checkGLError();
	}

	@Override
	public void bind(GL2 gl2) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void draw(final float xOffset, final float yOffset, final float zOffset) {
		if (vertices == null)
			return;
		
		assert (numVertices > 0);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(xOffset, yOffset, zOffset);
		
		GL11.glDrawArrays(GL11.GL_QUADS, 0, numVertices);
		
		GL11.glPopMatrix();
	}

	@Override
	public void draw(final float xOffset, final float yOffset, final float zOffset, GL2 gl2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getFaceCount() {
		return numVertices / 4;
	}

	@Override
	public float getFaceDepth(int faceIndex, Vector3f eye, Vector3f forward, float xOffset, float yOffset, float zOffset) {
		float x = faceCentres[faceIndex * 3] + xOffset;
		float y = faceCentres[faceIndex * 3 + 1] + yOffset;
		float z = faceCentres[faceIndex * 3 + 2] + zOffset;
		return (x - eye.x) * forward.x + (y - eye.y) * forward.y + (z - eye.z) * forward.z;
	}

	@Override
	public void drawFaces(int firstFace, int faceCount, float xOffset, float yOffset, float zOffset) {
		GL11.glPushMatrix();
		GL11.glTranslatef(xOffset, yOffset, zOffset);
		GL11.glDrawArrays(GL11.GL_QUADS, firstFace * 4, faceCount * 4);
		GL11.glPopMatrix();
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
