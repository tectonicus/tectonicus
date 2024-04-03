/*
 * Copyright (c) 2024, Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.List;

public class Item extends ItemBase
{
	public Item(final String id, final String customName, final int damage, final int count, final int slot, final List<Object> tag)
	{
		super(id, customName, damage, count, slot, tag);
	}
}
