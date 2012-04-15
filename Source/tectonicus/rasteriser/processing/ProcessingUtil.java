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
package tectonicus.rasteriser.processing;

import org.lwjgl.util.vector.Matrix4f;

import processing.core.PMatrix3D;

public class ProcessingUtil
{
	public static PMatrix3D toPMatrix(Matrix4f src)
	{
		PMatrix3D result = new PMatrix3D();
		
	/*
		result.m00 = src.m00;
		result.m01 = src.m01;
		result.m02 = src.m02;
		result.m03 = src.m03;
		
		result.m10 = src.m10;
		result.m11 = src.m11;
		result.m12 = src.m12;
		result.m13 = src.m13;
		
		result.m20 = src.m20;
		result.m21 = src.m21;
		result.m22 = src.m22;
		result.m23 = src.m23;
		
		result.m30 = src.m30;
		result.m31 = src.m31;
		result.m32 = src.m32;
		result.m33 = src.m33;
	*/
		result.m00 = src.m00;
		result.m10 = src.m01;
		result.m20 = src.m02;
		result.m30 = src.m03;
		
		result.m01 = src.m10;
		result.m11 = src.m11;
		result.m21 = src.m12;
		result.m31 = src.m13;
		
		result.m02 = src.m20;
		result.m12 = src.m21;
		result.m22 = src.m22;
		result.m32 = src.m23;
		
		result.m03 = src.m30;
		result.m13 = src.m31;
		result.m23 = src.m32;
		result.m33 = src.m33;
		
		return result;
	}
}
