/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser.lwjgl;

import com.jogamp.opengl.GL2;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;
import tectonicus.util.Colour4f;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class LwjglMesh implements Mesh {
	private static final int COLOUR_OFFSET = 3 * Float.BYTES;
	private static final int TEX_COORD_OFFSET = COLOUR_OFFSET + 4;
	private static final int STRIDE = TEX_COORD_OFFSET + 2 * Float.BYTES;

	// Vertex buffers currently allocated on the GPU, across all meshes, for diagnosing video memory use.
	private static final AtomicLong liveBuffers = new AtomicLong();
	private static final AtomicLong liveBufferBytes = new AtomicLong();

	private final LwjglTexture texture;
	// Shared vertex buffers to place this mesh in, or null to give the mesh a buffer of its own.
	private final VertexBufferPool pool;
	private VertexBufferPool.Allocation allocation;
	private ByteBuffer vertices;
	private int numVertices;
	private float[] faceCentres;
	private boolean finalised;
	private boolean boundOnce;
	private boolean uploadAttempted;
	private int vertexBuffer;
	private long uploadedBytes;

	/** Creates a mesh that gets its own vertex buffer once it is drawn more than once. */
	public LwjglMesh(LwjglTexture texture) {
		this(null, texture);
	}

	/** Creates a mesh whose vertices are placed in {@code pool}'s shared buffers, if there is room. */
	LwjglMesh(VertexBufferPool pool, LwjglTexture texture) {
		this.pool = pool;
		this.texture = texture;
	}

	public static long getLiveBufferCount() {
		return liveBuffers.get();
	}

	public static long getLiveBufferBytes() {
		return liveBufferBytes.get();
	}

	@Override
	public void destroy() {
		if (vertexBuffer != 0) {
			GL15.glDeleteBuffers(vertexBuffer);
			vertexBuffer = 0;
		}
		if (allocation != null) {
			pool.free(allocation);
			allocation = null;
		}
		if (uploadedBytes > 0) {
			liveBuffers.decrementAndGet();
			liveBufferBytes.addAndGet(-uploadedBytes);
			uploadedBytes = 0;
		}
		vertices = null;
		faceCentres = null;
		numVertices = 0;
	}

	@Override
	public void addVertex(Vector3f position, Vector4f colour, float u, float v) {
		addVertex(position, (byte) (colour.x * 255), (byte) (colour.y * 255),
				(byte) (colour.z * 255), (byte) (colour.w * 255), u, v);
	}

	@Override
	public void addVertex(Vector3f position, float u, float v) {
		addVertex(position, (byte) 255, (byte) 255, (byte) 255, (byte) 255, u, v);
	}

	@Override
	public void addVertex(Vector3f position, Colour4f colour, float u, float v) {
		addVertex(position, (byte) (colour.r * 255), (byte) (colour.g * 255),
				(byte) (colour.b * 255), (byte) (colour.a * 255), u, v);
	}

	private void addVertex(Vector3f position, byte red, byte green, byte blue, byte alpha, float u, float v) {
		assert !finalised;
		if (vertices == null) {
			vertices = BufferUtils.createByteBuffer(256 * STRIDE);
		} else if (!vertices.hasRemaining()) {
			ByteBuffer grown = BufferUtils.createByteBuffer(vertices.capacity() * 2);
			grown.put(vertices.flip());
			vertices = grown;
		}

		vertices.putFloat(position.x).putFloat(position.y).putFloat(position.z);
		vertices.put(red).put(green).put(blue).put(alpha);
		vertices.putFloat(u).putFloat(v);
		numVertices++;
	}

	@Override
	public void finalise() {
		assert !finalised;
		if (vertices != null) {
			vertices.flip();
			faceCentres = new float[getFaceCount() * 3];
			for (int face = 0; face < getFaceCount(); face++) {
				int first = face * 4 * STRIDE;
				for (int axis = 0; axis < 3; axis++) {
					int offset = first + axis * Float.BYTES;
					faceCentres[face * 3 + axis] = (vertices.getFloat(offset) + vertices.getFloat(offset + STRIDE)
							+ vertices.getFloat(offset + 2 * STRIDE) + vertices.getFloat(offset + 3 * STRIDE)) * 0.25f;
				}
			}
		}
		finalised = true;
	}

	@Override
	public Texture getTexture() {
		return texture;
	}

	@Override
	public void bind() {
		if (numVertices == 0)
			return;
		assert finalised;

		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		if (!boundOnce || !bindVertexBuffer()) {
			// Inventory icons usually need one draw. Upload only once the mesh is reused.
			GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, vertices);
			GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, vertices.slice(COLOUR_OFFSET, vertices.limit() - COLOUR_OFFSET));
			GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, vertices.slice(TEX_COORD_OFFSET, vertices.limit() - TEX_COORD_OFFSET));
			boundOnce = true;
		}

		if (texture != null) {
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
		} else {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}
	}

	private boolean bindVertexBuffer() {
		if (pool != null)
			return bindPooledVertexBuffer();
		if (vertexBuffer == 0) {
			if (uploadAttempted)
				return false;
			uploadAttempted = true;
			vertexBuffer = GL15.glGenBuffers();
			if (vertexBuffer == 0)
				return false;
		}
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
		try {
			if (vertices != null) {
				GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
				if (GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE) != vertices.remaining()) {
					// An allocation failure leaves the mesh usable through its client arrays.
					GL15.glDeleteBuffers(vertexBuffer);
					vertexBuffer = 0;
					return false;
				}
				uploadedBytes = vertices.remaining();
				liveBuffers.incrementAndGet();
				liveBufferBytes.addAndGet(uploadedBytes);
				vertices = null;
			}
			GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, 0L);
			GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, (long) COLOUR_OFFSET);
			GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, (long) TEX_COORD_OFFSET);
			return true;
		} finally {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}
	}

	private boolean bindPooledVertexBuffer() {
		if (allocation == null) {
			if (uploadAttempted || vertices == null)
				return false;
			uploadAttempted = true;
			allocation = pool.allocate(vertices);
			if (allocation == null)
				return false; // No room: keep drawing from the client-side copy.
			uploadedBytes = allocation.bytes();
			liveBuffers.incrementAndGet();
			liveBufferBytes.addAndGet(uploadedBytes);
			vertices = null;
		}
		final long base = allocation.offset();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, allocation.buffer());
		try {
			GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, base);
			GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, base + COLOUR_OFFSET);
			GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, base + TEX_COORD_OFFSET);
		} finally {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}
		return true;
	}

	@Override
	public void bind(GL2 gl2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void draw(float xOffset, float yOffset, float zOffset) {
		if (numVertices == 0)
			return;
		drawVertices(0, numVertices, xOffset, yOffset, zOffset);
	}

	@Override
	public void draw(float xOffset, float yOffset, float zOffset, GL2 gl2) {
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
		drawVertices(firstFace * 4, faceCount * 4, xOffset, yOffset, zOffset);
	}

	private void drawVertices(int firstVertex, int vertexCount, float xOffset, float yOffset, float zOffset) {
		GL11.glPushMatrix();
		GL11.glTranslatef(xOffset, yOffset, zOffset);
		GL11.glDrawArrays(GL11.GL_QUADS, firstVertex, vertexCount);
		GL11.glPopMatrix();
	}

	@Override
	public void drawQuads(IntBuffer vertexIndices, float xOffset, float yOffset, float zOffset) {
		if (!vertexIndices.hasRemaining())
			return;
		GL11.glPushMatrix();
		GL11.glTranslatef(xOffset, yOffset, zOffset);
		GL11.glDrawElements(GL11.GL_QUADS, vertexIndices);
		GL11.glPopMatrix();
	}

	@Override
	public int getMemorySize() {
		int size = vertices != null ? vertices.capacity() : (vertexBuffer != 0 || allocation != null) ? numVertices * STRIDE : 0;
		return size + (faceCentres == null ? 0 : faceCentres.length * Float.BYTES);
	}

	@Override
	public long getVertexBytes() {
		return (long) numVertices * STRIDE;
	}

	@Override
	public int getTotalVertices() {
		return numVertices;
	}
}
