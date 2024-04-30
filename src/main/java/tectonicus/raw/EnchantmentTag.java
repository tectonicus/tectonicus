/*
 * Copyright (c) 2024, Tectonicus and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class EnchantmentTag
{
        final public String id;
        final public Integer level;
        
        public EnchantmentTag(final String id, final Integer level) {
                this.id = id;
                this.level = level;
        }
}
