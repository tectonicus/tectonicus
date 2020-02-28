/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

public class Pair<S, T>
{
	public final S first;
	public final T second;
	
	public Pair(S first, T second)
	{
		this.first = first;
		this.second = second;
	}
}
