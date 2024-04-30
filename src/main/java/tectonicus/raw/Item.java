/*
 * Copyright (c) 2024, Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.ArrayList;
import java.util.List;

public class Item
{
	public String id;
	public int damage;
	public int count;
	public int slot;
        public List<Object> components;
	
        public Item(final String id, List<Object> tag)
        {
                this(id, 0, 0, 0, tag);
        }
        
	public Item(final String id, final int damage, final int count, final int slot, List<Object> components)
	{
		this.id = id;
		this.damage = damage;
		this.count = count;
		this.slot = slot;
                this.components = components;
                
                if (this.components == null) {
                        this.components = new ArrayList<>();
                }
	}
        
        @SuppressWarnings("unchecked")
        public <T> T getComponent(Class<T> clazz) {
                T result = null;
                for (Object o : components) {
                        if (clazz.isInstance(o)) {
                                result = (T)o;
                        }
                }
                return result;
        }
}
