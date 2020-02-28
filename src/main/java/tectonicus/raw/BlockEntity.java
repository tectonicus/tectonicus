/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class BlockEntity
{
	private int x, y, z;
	private final int localX, localY, localZ;
	
	public BlockEntity(int x, int y, int z, int localX, int localY, int localZ)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.localX = localX;
		this.localY = localY;
		this.localZ = localZ;
	}
	
	public int getX() {	return x; }
	public int getY() {	return y; }
	public int getZ() {	return z; }
	public int getLocalX() { return localX; }
	public int getLocalY() { return localY; }
	public int getLocalZ() { return localZ; }
	
	public void setX(int x) { this.x = x; }
	public void setZ(int z) { this.z = z; }
}
