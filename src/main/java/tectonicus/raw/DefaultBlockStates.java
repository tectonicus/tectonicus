/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * The properties of each block's default state, from {@code default_block_states.json}.
 * <p>
 * Saves only omit a block's properties when the block is in its default state; newer saves store such
 * palette entries as the bare block id. Without these defaults, code that reads properties directly, such
 * as a sign's {@code rotation}, would see none at all.
 */
@Slf4j
final class DefaultBlockStates {
	private static final Map<String, Map<String, String>> STATES = load();

	private DefaultBlockStates() {}

	/** A new properties object for the block's default state; empty for blocks without properties. */
	static BlockProperties of(String blockName) {
		Map<String, String> properties = STATES.get(blockName);
		return new BlockProperties(properties != null ? properties : Collections.emptyMap());
	}

	private static Map<String, Map<String, String>> load() {
		try (InputStream in = DefaultBlockStates.class.getClassLoader().getResourceAsStream("default_block_states.json")) {
			if (in == null) {
				log.warn("Default block states resource was not found");
				return Collections.emptyMap();
			}
			return Map.copyOf(new ObjectMapper().readValue(in, new TypeReference<Map<String, Map<String, String>>>() {}));
		} catch (IOException | JacksonException e) {
			log.error("Unable to load default block states", e);
			return Collections.emptyMap();
		}
	}
}
