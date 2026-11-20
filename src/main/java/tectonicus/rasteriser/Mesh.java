/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import com.jogamp.opengl.GL2;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.util.Colour4f;

import java.nio.IntBuffer;

public interface Mesh {
	void destroy();

	void finalise();

	Texture getTexture();

	void bind();
	
	void bind(GL2 gl2);
	
	void draw(final float xOffset, final float yOffset, final float zOffset);
	
	void draw(final float xOffset, final float yOffset, final float zOffset, GL2 gl2);

	int getFaceCount();

	float getFaceDepth(int faceIndex, Vector3f eye, Vector3f forward, float xOffset, float yOffset, float zOffset);

	void drawFaces(int firstFace, int faceCount, float xOffset, float yOffset, float zOffset);

	/**
	 * Draws quads in the order given by {@code vertexIndices}, four vertex indices per face, in one draw call.
	 * The buffer must be direct; its remaining indices are drawn and its position is left unchanged.
	 * The default draws one face at a time, for meshes that have no faster way.
	 */
	default void drawQuads(IntBuffer vertexIndices, float xOffset, float yOffset, float zOffset) {
		for (int i = vertexIndices.position(); i + 3 < vertexIndices.limit(); i += 4)
			drawFaces(vertexIndices.get(i) / 4, 1, xOffset, yOffset, zOffset);
	}


	int getMemorySize();
	
	int getTotalVertices();
	
	
	void addVertex(Vector3f position, Vector4f colour, final float u, final float v);
	
	void addVertex(Vector3f position, final float u, final float v);
	
	void addVertex(org.joml.Vector3f position, Colour4f color, float u, float v);
	
}

