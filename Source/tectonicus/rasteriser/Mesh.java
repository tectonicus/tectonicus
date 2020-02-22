/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
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

public interface Mesh
{

	public void destroy();
	
	public void finalise();

	public Texture getTexture();
	
	public void bind();
	public void bind(GL2 gl2);

	public void draw(final float xOffset, final float yOffset, final float zOffset);
	public void draw(final float xOffset, final float yOffset, final float zOffset, GL2 gl2);

	
	public int getMemorySize();

	public int getTotalVertices();
	
	
	public void addVertex(Vector3f position, Vector4f colour, final float u, final float v);

	public void addVertex(Vector3f position, final float u, final float v);

	public void addVertex(org.joml.Vector3f position, Colour4f color, float u, float v);
	
}
