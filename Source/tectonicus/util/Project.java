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
package tectonicus.util;

import java.awt.Rectangle;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Project
{
	private static FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
	private static FloatBuffer cameraBuffer = BufferUtils.createFloatBuffer(16);
	private static IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
	
	public static Vector2f project(org.lwjgl.util.vector.Vector3f worldPos, Matrix4f projectionMatrix, Matrix4f cameraMatrix, Rectangle viewport)
	{
		projectionBuffer.clear();
		projectionMatrix.store(projectionBuffer);
		projectionBuffer.flip();
		
		cameraBuffer.clear();
		cameraMatrix.store(cameraBuffer);
		cameraBuffer.flip();
		
		viewportBuffer.clear();
		viewportBuffer.put((int)viewport.getX()).put((int)viewport.getY()).put((int)viewport.getWidth()).put((int)viewport.getHeight());
		viewportBuffer.flip();
		
		FloatBuffer outBuffer = BufferUtils.createFloatBuffer(16);
		GLU.gluProject(worldPos.x, worldPos.y, worldPos.z, cameraBuffer, projectionBuffer, viewportBuffer, outBuffer);
		
		final float x = outBuffer.get(0);
		
		// Invert y so that origin is top-left
		final float y = (float)viewport.getHeight() - outBuffer.get(1);
		
		return new Vector2f(x, y);
	}
	
	public static Vector3f unproject(Vector3f screenPos, Matrix4f projectionMatrix, Matrix4f cameraMatrix, Rectangle viewport)
	{
		projectionBuffer.clear();
		projectionMatrix.store(projectionBuffer);
		projectionBuffer.flip();
		
		cameraBuffer.clear();
		cameraMatrix.store(cameraBuffer);
		cameraBuffer.flip();
		
		viewportBuffer.clear();
		viewportBuffer.put((int)viewport.getX()).put((int)viewport.getY()).put((int)viewport.getWidth()).put((int)viewport.getHeight());
		viewportBuffer.flip();
		
		final float projX = screenPos.x;
		
		// Invert y so that origin is top-left
		final float projY = (int)viewport.getHeight() - screenPos.y;
		
		final float projZ = screenPos.z;
		
		FloatBuffer outBuffer = BufferUtils.createFloatBuffer(16);
		GLU.gluUnProject(projX, projY, projZ, cameraBuffer, projectionBuffer, viewportBuffer, outBuffer);
		
		final float x = outBuffer.get(0);
		final float y = outBuffer.get(1);
		final float z = outBuffer.get(2);
		
		return new Vector3f(x, y, z);
	}
}
