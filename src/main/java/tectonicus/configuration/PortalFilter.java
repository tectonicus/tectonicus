/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import tectonicus.ChunkCoord;
import tectonicus.util.Vector3l;

public class PortalFilter
{
	private PortalFilterType type;
	
	public PortalFilter()
	{
		this.type = PortalFilterType.All;
	}
	
	public PortalFilter(PortalFilterType type)
	{
		this.type = type;
	}
	
	public boolean passesFilter(ChunkCoord coord, Vector3l position)
	{
		if (type == PortalFilterType.All)
			return true;
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
