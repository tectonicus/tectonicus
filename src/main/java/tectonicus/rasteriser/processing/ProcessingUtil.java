/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.processing;


import org.joml.Matrix4f;
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
		result.m00 = src.m00();
		result.m10 = src.m01();
		result.m20 = src.m02();
		result.m30 = src.m03();

		result.m01 = src.m10();
		result.m11 = src.m11();
		result.m21 = src.m12();
		result.m31 = src.m13();
		
		result.m02 = src.m20();
		result.m12 = src.m21();
		result.m22 = src.m22();
		result.m32 = src.m23();
		
		result.m03 = src.m30();
		result.m13 = src.m31();
		result.m23 = src.m32();
		result.m33 = src.m33();
		
		return result;
	}
}
