/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import tectonicus.raw.RawSign;

public class ViewFilter
{
	private ViewFilterType type;
	
	public ViewFilter()
	{
		this.type = ViewFilterType.All;
	}
	
	public ViewFilter(ViewFilterType type)
	{
		this.type = type;
	}
	
	public boolean passesFilter(RawSign sign)
	{
		if (type == ViewFilterType.All)
		{
			String start = sign.text1.trim();
			return start.startsWith("#view");
		}
		else
			return false;
	}
	
	@Override
	public String toString()
	{
		// We need to override this so that MutableConfiguration.printActive works
		
		return type.toString();
	}
}
