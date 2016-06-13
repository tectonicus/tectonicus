/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.List;

import tectonicus.blockTypes.Banner.Pattern;

public class BlockEntity
{
	private int blockId;
	private int blockData;
	
	private int x, y, z;
	private int localX, localY, localZ;
	
	private int data, item;
	
	private int dir;
	
	private String text1, text2, text3, text4, motive;
	
	private List<Pattern> patterns;

	public BlockEntity(int blockData, int x, int y, int z, //Constructor for banners
			  int localX, int localY, int localZ, List<Pattern> patterns)
	{
		init(0, blockData, x, y, z, localX, localY, localZ, 0, 0, null, null, null, null, null, 0, patterns);
	}
	
	public BlockEntity(int blockId, int blockData,  //Constructor for paintings
			  int x, int y, int z,
			  int localX, int localY, int localZ,
			  String motive, int dir)
	{
		init(blockId, blockData, x, y, z, localX, localY, localZ, 0, 0, null, null, null, null, motive, dir, null);
	}
	
	public BlockEntity(int blockId, int blockData,  //Constructor for flower pots
					  int x, int y, int z,
					  int localX, int localY, int localZ,
					  int data, int item)
	{
		init(blockId, blockData, x, y, z, localX, localY, localZ, data, item, null, null, null, null, null, 0, null);
	}
	
	public BlockEntity(int blockId, int data,  //Constructor for signs
					int x, int y, int z,
					int localX, int localY, int localZ,
					String text1, String text2, String text3, String text4)
	{
		init(blockId, data, x, y, z, localX, localY, localZ, 0, 0, text1, text2, text3, text4, null, 0, null);
	}
	
	private void init(int blockId, int blockData,
					int x, int y, int z,
					int localX, int localY, int localZ,
					int data, int item,
					String text1, String text2, String text3, String text4, String motive, int dir, List<Pattern> patterns)
	{
		this.blockId = blockId;
		this.blockData = blockData;
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.localX = localX;
		this.localY = localY;
		this.localZ = localZ;
		
		this.data = data;
		this.item = item;
		
		this.dir = dir;
		
		this.text1 = text1;
		this.text2 = text2;
		this.text3 = text3;
		this.text4 = text4;
		
		this.motive = motive;
		
		this.patterns = patterns;
	}
	
	public int getBlockId() { return blockId; }
	public int getBlockData() { return blockData; }
	public int getX() {	return x; }
	public int getY() {	return y; }
	public int getZ() {	return z; }
	public int getLocalX() { return localX; }
	public int getLocalY() { return localY; }
	public int getLocalZ() { return localZ; }
	public int getItem() { return item; }
	public int getData() { return data; }
	public int getDirection() { return dir; }
	public String getText1() { return text1; }
	public String getText2() { return text2; }
	public String getText3() { return text3; }
	public String getText4() { return text4; }
	public String getMotive() { return motive; }
	public List<Pattern> getPatterns() { return patterns; }
	
	public void setX(int x) { this.x = x; }
	public void setZ(int z) { this.z = z; }
	public void setText1(String text1) { this.text1 = text1; }
	public void setText2(String text2) { this.text2 = text2; }
	public void setText3(String text3) { this.text3 = text3; }
	public void setText4(String text4) { this.text4 = text4; }
}
