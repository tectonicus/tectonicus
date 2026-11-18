/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import lombok.extern.slf4j.Slf4j;
import tectonicus.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Colours of spawn eggs in jars before 1.21.4, where Minecraft tinted one grey egg texture in code rather than
 * listing the colours in item model definitions. {@code spawnEggColors.json} holds the colours registered by 1.13.2.
 */
@Slf4j
public final class SpawnEggColors {
	private static final Map<String, int[]> COLORS = load();

	private SpawnEggColors() {}

	/**
	 * @param itemId a spawn egg ID, such as "minecraft:pig_spawn_egg"
	 * @return the RGB colours of the egg (index 0) and its spots (index 1), or null if not known
	 */
	public static int[] of(String itemId) {
		int[] colors = COLORS.get(itemId);
		return colors == null ? null : colors.clone();
	}

	private static Map<String, int[]> load() {
		Map<String, int[]> colors = new HashMap<>();
		try (InputStream in = SpawnEggColors.class.getResourceAsStream("/spawnEggColors.json")) {
			if (in == null)
				throw new IOException("spawnEggColors.json is missing");
			FileUtils.getOBJECT_MAPPER().readTree(in).properties().forEach(e -> colors.put(e.getKey(), new int[] {
					Integer.parseInt(e.getValue().get(0).asString(), 16), Integer.parseInt(e.getValue().get(1).asString(), 16)}));
		} catch (IOException e) {
			log.warn("Unable to load spawn egg colours", e);
		}
		// Renamed in 1.16 with the same colours.
		if (colors.containsKey("minecraft:zombie_pigman_spawn_egg"))
			colors.put("minecraft:zombified_piglin_spawn_egg", colors.get("minecraft:zombie_pigman_spawn_egg"));
		return Map.copyOf(colors);
	}
}
