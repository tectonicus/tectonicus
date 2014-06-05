/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class Item
{
	public int id;
	public int damage;
	public int count;
	public int slot;
	
	public Item(final int id, final int damage, final int count, final int slot)
	{
		this.id = id;
		this.damage = damage;
		this.count = count;
		this.slot = slot;
	}
}
