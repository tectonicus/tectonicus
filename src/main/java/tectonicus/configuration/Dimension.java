/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public enum Dimension {
	OVERWORLD("minecraft:overworld"),
	NETHER("minecraft:the_nether"),
	END("minecraft:the_end");

	private static final Map<String, Dimension> ID_LOOKUP = new HashMap<>(values().length);

	@Getter
	private final String id;

	static {
		for (Dimension dimension : values()) {
			ID_LOOKUP.put(dimension.id, dimension);
		}
	}

	public static Dimension byId(String id) {
		return ID_LOOKUP.getOrDefault(id, OVERWORLD);
	}
}
