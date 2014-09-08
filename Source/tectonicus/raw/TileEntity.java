/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class TileEntity
{
	public int blockId;
	public int blockData;
	
	public int x, y, z;
	public int localX, localY, localZ;
	
	public int data, item;
	
	public int dir;
	
	public String text1, text2, text3, text4, motive;

	public TileEntity(int blockId, int blockData,  //Constructor for paintings
			  int x, int y, int z,
			  int localX, int localY, int localZ,
			  String motive, int dir)
	{
		init(blockId, blockData, x, y, z, localX, localY, localZ, 0, 0, null, null, null, null, motive, dir);
	}
	
	public TileEntity(int blockId, int blockData,  //Constructor for flower pots
					  int x, int y, int z,
					  int localX, int localY, int localZ,
					  int data, int item)
	{
		init(blockId, blockData, x, y, z, localX, localY, localZ, data, item, null, null, null, null, null, 0);
	}
	
	public TileEntity(int blockId, int data,  //Constructor for signs
					int x, int y, int z,
					int localX, int localY, int localZ,
					String text1, String text2, String text3, String text4)
	{
		init(blockId, data, x, y, z, localX, localY, localZ, 0, 0, text1, text2, text3, text4, null, 0);
	}
	
	private void init(int blockId, int blockData,
					int x, int y, int z,
					int localX, int localY, int localZ,
					int data, int item,
					String text1, String text2, String text3, String text4, String motive, int dir)
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
	}
}
