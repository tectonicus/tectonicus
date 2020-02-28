/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import tectonicus.cache.swap.Swappable;
import tectonicus.util.Vector3l;

public class Portal implements Swappable
{
	private Vector3l position;
	
	public Portal()
	{
		position = new Vector3l();
	}
	
	public Portal(long x, long y, long z)
	{
		position = new Vector3l(x, y, z);
	}
	
	public long getX()
	{
		return position.x;
	}
	public long getY()
	{
		return position.y;
	}
	public long getZ()
	{
		return position.z;
	}
	
	@Override
	public void readFrom(DataInputStream source) throws Exception
	{
		position.x = source.readLong();
		position.y = source.readLong();
		position.z = source.readLong();
	}
	
	@Override
	public void writeTo(DataOutputStream dest) throws Exception
	{
		dest.writeLong(position.x);
		dest.writeLong(position.y);
		dest.writeLong(position.z);
	}
}
