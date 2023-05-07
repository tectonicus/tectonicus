/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
public class BlockProperties {
    private Map<String, String> properties = new TreeMap<>();  // we use a TreeMap to get automatic sorting
    private String propertiesString;

    public BlockProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
        this.propertiesString = this.toString();
    }

    private static final Pattern toStringReplacePattern = Pattern.compile("^\\{|\\s+|}$");
    @Override
    public String toString() {
        return toStringReplacePattern.matcher(properties.toString()).replaceAll(StringUtils.EMPTY);
    }

    public String get(String key) {
        return properties.get(key);
    }

    public boolean containsKey(String prop) {
        return properties.containsKey(prop);
    }

    public boolean contains(String props) {
        return propertiesString.contains(props);
    }
    public boolean containsAll(Map<String, String> props) {
        return properties.entrySet().containsAll(props.entrySet());
    }
}
