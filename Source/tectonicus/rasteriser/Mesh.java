/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public interface Mesh
{

	public void destroy();
	
	public void finalise();

	public Texture getTexture();
	
	public void bind();

	public void draw(final float xOffset, final float yOffset, final float zOffset);

	
	public int getMemorySize();

	public int getTotalVertices();
	
	
	public void addVertex(Vector3f position, Vector4f colour, final float u, final float v);
	
	public void addVertex(Vector3f position, final float u, final float v);
	
}
