/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import tectonicus.cache.swap.Swappable;

public class Player implements Swappable
{
	private String name;
	
	public Player()
	{
		this.name = "Anon";
	}
	
	public Player(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	@Override
	public void readFrom(DataInputStream source) throws Exception
	{
		this.name = source.readUTF();
	}
	
	@Override
	public void writeTo(DataOutputStream dest) throws Exception
	{
		dest.writeUTF(name);
	}
}
