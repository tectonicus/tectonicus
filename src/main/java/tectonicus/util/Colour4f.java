/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.awt.Color;

public class Colour4f
{
	public float r, g, b, a;
	
	public Colour4f()
	{
		r = g = b = a = 1.0f;
	}
	
	public Colour4f(Colour4f other)
	{
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
		this.a = other.a;
	}
	
	public Colour4f(float red, float green, float blue, float alpha)
	{
		this.r = red;
		this.g = green;
		this.b = blue;
		this.a = alpha;
	}
	
	public Colour4f(float red, float green, float blue)
	{
		this.r = red;
		this.g = green;
		this.b = blue;
		this.a = 1.0f;
	}
	
	public Colour4f(Color src)
	{
		this.r = (float)(src.getRed()) / 255.0f;
		this.g = (float)(src.getGreen()) / 255.0f;
		this.b = (float)(src.getBlue()) / 255.0f;
		this.a = (float)(src.getAlpha()) / 255.0f;
	}
	
	public void add(Colour4f other)
	{
		this.r += other.r;
		this.g += other.g;
		this.b += other.b;
		this.a += other.a;
	}
	
	public void divide(final float factor)
	{
		this.r /= factor;
		this.g /= factor;
		this.b /= factor;
		this.a /= factor;
	}

	public void multiply(Colour4f other) {
		this.r *= other.r;
		this.g *= other.g;
		this.b *= other.b;
		this.a *= other.a;
	}
	
	public void average(Colour4f other)
	{
		this.r = (this.r + other.r) / 2;
		this.g = (this.g + other.g) / 2;
		this.b = (this.b + other.b) / 2;
		this.a = (this.a + other.a) / 2;
	}
}
