/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Maps Bedrock Edition's numeric biome ids to Java Edition biome ids. */
@Slf4j
public final class BedrockBiomes {
	private static final String FALLBACK = "minecraft:plains";
	private static final Map<Integer, String> JAVA_IDS = load();
	private static final Set<Integer> reported = ConcurrentHashMap.newKeySet();

	private BedrockBiomes() {}

	/** The Java biome id, such as {@code minecraft:swamp}, for a Bedrock biome id. */
	public static String javaId(int bedrockId) {
		String id = JAVA_IDS.get(bedrockId);
		if (id == null) {
			if (reported.add(bedrockId))
				log.warn("Unknown Bedrock biome id {}; using plains", bedrockId);
			return FALLBACK;
		}
		return id;
	}

	private static Map<Integer, String> load() {
		Map<Integer, String> ids = new HashMap<>();
		try (InputStream in = BedrockBiomes.class.getResourceAsStream("/bedrock/biomes.tsv")) {
			if (in == null)
				throw new IllegalStateException("Missing resource /bedrock/biomes.tsv");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isEmpty() || line.startsWith("#"))
						continue;
					String[] parts = line.split("\t");
					ids.put(Integer.parseInt(parts[0]), parts[2]);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return Map.copyOf(ids);
	}
}
