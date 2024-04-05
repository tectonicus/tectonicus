/*
 * Copyright (c) 2024, Tectonicus other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.ArrayList;
import java.util.List;

public abstract class ItemBase
{
	public String id;
        public String customName;
	public int damage;
	public int count;
	public int slot;
        public List<Object> tag;
	
	public ItemBase(final String id, final String customName, final int damage, final int count, final int slot, List<Object> tag)
	{
		this.id = id;
                this.customName = customName;
		this.damage = damage;
		this.count = count;
		this.slot = slot;
                this.tag = tag;
                
                if (this.tag == null) {
                        this.tag = new ArrayList<>();
                }
	}
        
        @SuppressWarnings("unchecked")
        public <T> T getTag(Class<T> clazz) {
                T result = null;
                for (Object o : tag) {
                        if (clazz.isInstance(o)) {
                                result = (T)o;
                        }
                }
                return result;
        }
}
