/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import com.jogamp.opengl.GL2;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;
import tectonicus.util.Colour4f;

import java.nio.IntBuffer;

/** A mesh that ignores its vertices, for parts that are hidden, such as the frame of an invisible item frame. */
final class DiscardingMesh implements Mesh {
	static final DiscardingMesh INSTANCE = new DiscardingMesh();

	private DiscardingMesh() {}

	@Override public void destroy() { }
	@Override public void finalise() { }
	@Override public Texture getTexture() { return null; }
	@Override public void bind() { }
	@Override public void bind(GL2 gl2) { }
	@Override public void draw(float xOffset, float yOffset, float zOffset) { }
	@Override public void draw(float xOffset, float yOffset, float zOffset, GL2 gl2) { }
	@Override public int getFaceCount() { return 0; }
	@Override public float getFaceDepth(int faceIndex, Vector3f eye, Vector3f forward, float xOffset, float yOffset, float zOffset) { return 0; }
	@Override public void drawFaces(int firstFace, int faceCount, float xOffset, float yOffset, float zOffset) { }
	@Override public void drawQuads(IntBuffer vertexIndices, float xOffset, float yOffset, float zOffset) { }
	@Override public int getMemorySize() { return 0; }
	@Override public int getTotalVertices() { return 0; }
	@Override public void addVertex(Vector3f position, Vector4f colour, float u, float v) { }
	@Override public void addVertex(Vector3f position, float u, float v) { }
	@Override public void addVertex(Vector3f position, Colour4f color, float u, float v) { }
}
