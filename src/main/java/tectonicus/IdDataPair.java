/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class IdDataPair
{
	public final int id;
	public final int data;
	
	public IdDataPair(final int id, final int data)
	{
		this.id = id;
		this.data = data;
	}
	
	@Override
	public int hashCode()
	{
		return id ^ data;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof IdDataPair))
			return false;
		
		IdDataPair other = (IdDataPair)obj;
		
		return this.id == other.id && this.data == other.data;
	}
}
