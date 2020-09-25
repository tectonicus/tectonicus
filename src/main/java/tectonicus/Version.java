/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Version {
    UNKNOWN_VERSION("unknown", -1),
    VERSION_4("1.4", 4),
    VERSION_5("1.5", 5),
    VERSIONS_6_TO_8("1.6-1.8", 6),
    VERSION_RV("1.RV-Pre1", 9),
    VERSIONS_9_TO_11("1.9-1.11", 9),
    VERSION_12("1.12", 12),
    VERSION_13("1.13", 13),
    VERSION_14("1.14", 14),
    VERSION_15("1.15", 15);

    private final String strVersion;
    private final int numVersion;
}
