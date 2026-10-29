/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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
	OVERWORLD("minecraft:overworld", "Overworld"),
	NETHER("minecraft:the_nether", "The Nether"),
	END("minecraft:the_end", "The End"),
	OTHER("minecraft:other", "Other"),
	UNKNOWN("unknown", "Other");

	private static final Map<String, Dimension> ID_LOOKUP = new HashMap<>(values().length);

	@Getter
	private final String id;
	@Getter
	private final String name;

	static {
		for (Dimension dimension : values()) {
			ID_LOOKUP.put(dimension.id, dimension);
		}
	}

	public static Dimension byId(String id) {
		return ID_LOOKUP.getOrDefault(id, id != null && id.contains(":") ? OTHER : OVERWORLD);
	}
}
