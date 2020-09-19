/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
public class BlockProperties {
    private Map<String, String> properties = new TreeMap<>();

    public BlockProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    @Override
    public String toString() {
        return properties.toString().replaceAll("^\\{|\\s+|}$", "");
    }

    public void put(String key, String value) {
        properties.put(key, value);
    }

    public String get(String key) {
        return properties.get(key);
    }

    public boolean containsKey(String prop) {
        return properties.containsKey(prop);
    }

    public boolean contains(String props) {
        return this.toString().contains(props);
    }
    public boolean containsAll(Map<String, String> props) {
        return properties.entrySet().containsAll(props.entrySet());
    }
}
