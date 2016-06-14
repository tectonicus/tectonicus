/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import tectonicus.raw.SignEntity;

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
	
	public boolean passesFilter(SignEntity sign)
	{
		if (type == ViewFilterType.All)
		{
			String text1 = sign.getText1().trim();
			String text2 = sign.getText2().trim();
			String text3 = sign.getText3().trim();
			String text4 = sign.getText4().trim();
			
			return (text1.startsWith("#view") || text2.startsWith("#view") || text3.startsWith("#view") || text4.startsWith("#view")) ? true : false;
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
