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

import java.nio.FloatBuffer;
import java.text.NumberFormat;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class MatrixUtil
{
	public static Matrix4f createOrthoMatrix(final float left, final float right, final float bottom, final float top, final float near, final float far)
	{
		Matrix4f ortho = new Matrix4f();
		ortho.setZero();
		
		// First the scale part
		ortho.m00 = 2.0f / (right - left);
		ortho.m11 = 2.0f / (top - bottom);
		ortho.m22 = (-2.0f) / (far - near);
		ortho.m33 = 1.0f;
		
		// Then the translation part
		ortho.m30 = -( (right+left) / (right-left) );
		ortho.m31 = -( (top+bottom) / (top-bottom) );
		ortho.m32 = -( (far+near) / (far-near) );
		
		return ortho;
	}
	
	private static float degToRad(final float deg)
	{
		return deg / 180.0f * (float)Math.PI;
	}
	
	public static Matrix4f createPerspectiveMatrix(final float fovYDeg, final float aspect, final float zNear, final float zFar)
	{
		Matrix4f persp = new Matrix4f();
		persp.setZero();
		
		final float fovYRad = degToRad(fovYDeg);
		
		final float f = 1.0f / (float)Math.tan( fovYRad/2 );
		
		persp.m00 = f / aspect;
		persp.m11 = f;
		persp.m22 = (zFar + zNear) / (zNear-zFar);
		
		persp.m23 = -1;
		
		persp.m32 = (2*zNear*zFar) / (zNear-zFar);
		
		return persp;
	}
	
	public static Matrix4f createLookAt(Vector3f eye, Vector3f lookAt, Vector3f up)
	{
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		
		// Create the basis vectors
		Vector3f forwards = Vector3f.sub(eye, lookAt, null);
		forwards.normalise();
		
		Vector3f right = Vector3f.cross(up, forwards, null);
		right.normalise();
		
		Vector3f actualUp = Vector3f.cross(forwards, right, null);
		actualUp.normalise();
		
		// Right vector across the top
		matrix.m00 = right.x;
		matrix.m10 = right.y;
		matrix.m20 = right.z;
		
		// Up vector across the middle row
		matrix.m01 = actualUp.x;
		matrix.m11 = actualUp.y;
		matrix.m21 = actualUp.z;
		
		// Forwards vector across the bottom row
		matrix.m02 = forwards.x;
		matrix.m12 = forwards.y;
		matrix.m22 = forwards.z;
		
		// Negative translation in the last column
		Matrix4f translation = new Matrix4f();
		translation.setIdentity();
		translation.translate(new Vector3f(-eye.x, -eye.y, -eye.z));
		
		return Matrix4f.mul(matrix, translation, null);
	}
	
	
	
	public static void testOrthoMatrix()
	{
	/*	testOrthoMatrix(0, 800, 600, 0, -1, 1);
		testOrthoMatrix(-256, 256, -256, 256, -10, 10);
		testOrthoMatrix(-512, 512, -512, 512, -1000, 1000);
		testOrthoMatrix(123, 79, 234, 209, 293, 943);
	*/	
		testOrthoMatrix(-20, 20, -20, 20, -1000, 1000);
	/*	
		testLookAtMatrix(new Vector3f(0, 0, 0), new Vector3f(1, 1, 1), new Vector3f(0, 1, 0));
		testLookAtMatrix(new Vector3f(0, 0, 0), new Vector3f(10, -20, 30), new Vector3f(0, 1, 0));
		testLookAtMatrix(new Vector3f(12, 34, 56), new Vector3f(21, 43, 54), new Vector3f(0, 1, 0));
		testLookAtMatrix(new Vector3f(1, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
	*/
	}

	public static void testLookAtMatrix(Vector3f eye, Vector3f center, Vector3f up)
	{
		// Make a lookat matrix in opengl and pull it out into a Matrix4f
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GLU.gluLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
		
		FloatBuffer fromGlBuffer = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW, fromGlBuffer);
		Matrix4f fromGl = new Matrix4f();
		fromGl.load(fromGlBuffer);
		
		Matrix4f manual = createLookAt(eye, center, up);
		
		compare(fromGl, manual);
	}
	
	public static void testOrthoMatrix(final int left, final int right, final int bottom, final int top, final int near, final int far)
	{
		// Make an ortho matrix in opengl and pull it out into a Matrix4f
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glOrtho(left, right, bottom, top, near, far);
		FloatBuffer fromGlBuffer = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW, fromGlBuffer);
		Matrix4f fromGl = new Matrix4f();
		fromGl.load(fromGlBuffer);
		
		Matrix4f manual = createOrthoMatrix(left, right, bottom, top, near, far);
		
		compare(fromGl, manual);
	}
	
	public static void compare(Matrix4f fromGl, Matrix4f manual)
	{
		// Now compare
		System.out.println("From Gl:\n"+fromGl);
		System.out.println("Manual:\n"+manual);
		
		System.out.println("delta: "+getDelta(fromGl, manual));
		
		System.out.println("------");
	}
	
	public static float getDelta(Matrix4f left, Matrix4f right)
	{
		float delta = 0;
		
		final float d00 = left.m00 - right.m00;
		final float d01 = left.m01 - right.m01;
		final float d02 = left.m02 - right.m02;
		final float d03 = left.m03 - right.m03;
		
		final float d10 = left.m10 - right.m10;
		final float d11 = left.m11 - right.m11;
		final float d12 = left.m12 - right.m12;
		final float d13 = left.m13 - right.m13;
		
		final float d20 = left.m20 - right.m20;
		final float d21 = left.m21 - right.m21;
		final float d22 = left.m22 - right.m22;
		final float d23 = left.m23 - right.m23;
		
		final float d30 = left.m30 - right.m30;
		final float d31 = left.m31 - right.m31;
		final float d32 = left.m32 - right.m32;
		final float d33 = left.m33 - right.m33;
		
		delta += d00 + d01 + d02 + d03;
		delta += d10 + d11 + d12 + d13;
		delta += d20 + d21 + d22 + d23;
		delta += d30 + d31 + d32 + d33;
		
		NumberFormat format = NumberFormat.getInstance();
		format.setMinimumFractionDigits(8);
		format.setMinimumIntegerDigits(3);
		
		System.out.println("[" + format.format(d00) + ", " + format.format(d10) + ", " + format.format(d20) + ", " + format.format(d30) + "]");
		System.out.println("[" + format.format(d01) + ", " + format.format(d11) + ", " + format.format(d21) + ", " + format.format(d31) + "]");
		System.out.println("[" + format.format(d02) + ", " + format.format(d12) + ", " + format.format(d22) + ", " + format.format(d32) + "]");
		System.out.println("[" + format.format(d03) + ", " + format.format(d13) + ", " + format.format(d23) + ", " + format.format(d33) + "]");
		System.out.println();
		
		return delta;
	}
	
}
