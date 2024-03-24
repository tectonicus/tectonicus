/*
 * Copyright (c) 2024, Tectonicus and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class ArmorTrimTag
{
        public String material;
        public String pattern;
        
        public ArmorTrimTag(String material, String pattern) {
                this.material = material;
                this.pattern = pattern;
        }
}
