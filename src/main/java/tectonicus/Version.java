/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum Version {
    VERSION_UNKNOWN("unknown", 9999),
    VERSION_4("1.4", 4),
    VERSION_5("1.5", 5),
    VERSIONS_6_TO_8("1.6-1.8", 6),
    VERSION_RV("1.RV-Pre1", 9),
    VERSIONS_9_TO_11("1.9-1.11", 9),
    VERSION_9("1.9", 9),
    VERSION_10("1.10", 10),
    VERSION_11("1.11", 11),
    VERSION_12("1.12", 12),
    VERSION_13("1.13", 13),
    VERSION_14("1.14", 14),
    VERSION_15("1.15", 15),
    VERSION_16("1.16", 16),
    VERSION_17("1.17", 17),
    VERSION_18("1.18", 18);

    private final String name;
    private final int numVersion;

    private static final Map<String, Version> NAME_LOOKUP = new HashMap<>(values().length);

    static {
        for (Version version : values()) {
            NAME_LOOKUP.put(version.name, version);
        }
    }

    public static Version byName(String name) {
        return NAME_LOOKUP.getOrDefault(name.toLowerCase(), VERSION_UNKNOWN);
    }
}
