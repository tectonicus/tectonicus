/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

public class ChestFilter
{
	private ChestFilterType type;
	
	public ChestFilter()
	{
		this.type = ChestFilterType.None;
	}
	
	public ChestFilter(ChestFilterType type)
	{
		this.type = type;
	}
	
	public boolean passesFilter(boolean unopenedChest)
	{
		if (type == ChestFilterType.All)
		{
			return true;
		}
		else if (type == ChestFilterType.Player && !unopenedChest)
		{
			return true;
		}	
		else
		{
			return false;
		}
	}
	
	@Override
	public String toString()
	{
		// We need to override this so that MutableConfiguration.printActive works
		return type.toString();
	}
}

