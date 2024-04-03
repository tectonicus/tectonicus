/*
 * Copyright (c) 2024, Tectonicus other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class ArmorItem extends ItemBase
{
	public ArmorItem(final String id, ArmorTrimTag armorTrim, DisplayTag display)
	{
                super(id, null, 0, 0, 0, null);

                if (armorTrim != null) {
                        tag.add(armorTrim);
                }
                if (display != null) {
                        tag.add(display);
                }
	}
}
