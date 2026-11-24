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
import java.util.HashMap;
import java.util.Map;

/**
 * The properties of each block's default state, from {@code default_block_states.json}. Loaded once and shared
 * by chunk parsing and the block registry.
 * <p>
 * Since 26.3, saves omit a block's properties when the block is in its default state and store such palette
 * entries as the bare block id. Without these defaults, code that reads properties directly, such as a sign's
 * {@code rotation}, would see none at all.
 */
@Slf4j
public final class DefaultBlockStates {
	private static final BlockProperties NO_PROPERTIES = BlockProperties.immutableCopyOf(Map.of());
	private static final Map<String, BlockProperties> STATES = load();

	private DefaultBlockStates() {}

	/** The block's default state, shared and immutable; empty for blocks without properties. */
	public static BlockProperties of(String blockName) {
		return STATES.getOrDefault(blockName, NO_PROPERTIES);
	}

	/** The block's default property values; empty for blocks without properties. */
	public static Map<String, String> get(String blockName) {
		return of(blockName).getProperties();
	}

	private static Map<String, BlockProperties> load() {
		try (InputStream in = DefaultBlockStates.class.getClassLoader().getResourceAsStream("default_block_states.json")) {
			if (in == null) {
				log.warn("Default block states resource was not found");
				return Collections.emptyMap();
			}
			Map<String, Map<String, String>> states = new ObjectMapper().readValue(in, new TypeReference<Map<String, Map<String, String>>>() {});
			Map<String, BlockProperties> result = new HashMap<>(states.size() * 2);
			states.forEach((name, properties) -> result.put(name, BlockProperties.immutableCopyOf(properties)));
			log.debug("Loaded {} default block states", result.size());
			return Map.copyOf(result);
		} catch (IOException | JacksonException e) {
			log.error("Unable to load default block states", e);
			return Collections.emptyMap();
		}
	}
}
