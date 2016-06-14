/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class SignEntity extends BlockEntity
{
	private String text1, text2, text3, text4;
	private int data;
	
	public SignEntity(int x, int y, int z, int localX, int localY, int localZ, String text1, String text2, String text3, String text4, int data)
	{
		super(x, y, z, localX, localY, localZ);
		this.text1 = text1;
		this.text2 = text2;
		this.text3 = text3;
		this.text4 = text4;
		this.data = data;
	}
	
	public String getText1() { return text1; }
	public String getText2() { return text2; }
	public String getText3() { return text3; }
	public String getText4() { return text4; }
	public int getBlockData() { return data; }
	
	public void setText1(String text1) { this.text1 = text1; }
	public void setText2(String text2) { this.text2 = text2; }
	public void setText3(String text3) { this.text3 = text3; }
	public void setText4(String text4) { this.text4 = text4; }
}
