/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import lombok.extern.slf4j.Slf4j;
import tectonicus.itemregistry.ItemLayers;
import tectonicus.util.Colour4f;
import tectonicus.util.FileUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Colours of spawn eggs in jars before 1.21.4, where Minecraft tinted one grey egg texture in code rather than
 * listing the colours in item model definitions. {@code spawnEggColors.json} holds the colours registered by 1.21.3,
 * plus zombie_pigman_spawn_egg from 1.13.2, which 1.16 renamed to zombified_piglin_spawn_egg.
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

	/**
	 * Colours the grey egg and spot layers of a spawn egg icon. Eggs from 1.21.5 on have one coloured texture and are
	 * returned unchanged, as is anything that is not a known spawn egg.
	 *
	 * @param itemId a spawn egg ID, such as "minecraft:pig_spawn_egg"
	 * @param layers the layers of the item model, bottom first
	 * @return tinted copies of the two layers, or the given layers
	 */
	public static List<BufferedImage> tint(String itemId, List<BufferedImage> layers) {
		int[] colors = COLORS.get(itemId);
		if (colors == null || layers.size() != 2)
			return layers;
		List<BufferedImage> tinted = new ArrayList<>(2);
		for (int i = 0; i < 2; i++) {
			BufferedImage source = layers.get(i);
			BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
			var graphics = copy.createGraphics();
			try {
				graphics.drawImage(source, 0, 0, null);
			} finally {
				graphics.dispose();
			}
			ItemLayers.tint(copy, new Colour4f(0xFF000000 | colors[i]));
			tinted.add(copy);
		}
		return tinted;
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
		return Map.copyOf(colors);
	}
}
