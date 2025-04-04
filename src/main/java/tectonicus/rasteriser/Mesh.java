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

public interface Mesh {
	void destroy();
	
	void finalise();
	
	Texture getTexture();
	
	void bind();
	
	void bind(GL2 gl2);
	
	void draw(final float xOffset, final float yOffset, final float zOffset);
	
	void draw(final float xOffset, final float yOffset, final float zOffset, GL2 gl2);
	
	
	int getMemorySize();
	
	int getTotalVertices();
	
	
	void addVertex(Vector3f position, Vector4f colour, final float u, final float v);
	
	void addVertex(Vector3f position, final float u, final float v);
	
	void addVertex(org.joml.Vector3f position, Colour4f color, float u, float v);
	
}

