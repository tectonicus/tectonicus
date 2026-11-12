/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import lombok.extern.slf4j.Slf4j;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.raw.SavedMap;
import tectonicus.texture.SubTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
final class SavedMaps implements AutoCloseable {
	private final Path dataDir;
	private final Rasteriser rasteriser;
	private final boolean legacyPalette;
	// Hashing and rendering use the same snapshot, including missing maps.
	private final Map<Integer, Optional<SavedMap>> maps = new ConcurrentHashMap<>();
	// Texture creation and destruction run on the render thread.
	private final Map<Integer, SubTexture> textures = new HashMap<>();

	SavedMaps(Path worldDir, Rasteriser rasteriser, boolean legacyPalette) {
		this.dataDir = worldDir.resolve("data");
		this.rasteriser = rasteriser;
		this.legacyPalette = legacyPalette;
	}

	byte[] getContentHash(int mapId) {
		return maps.computeIfAbsent(mapId, this::load).map(SavedMap::contentHash).orElse(null);
	}

	SubTexture getTexture(int mapId) {
		return textures.computeIfAbsent(mapId, id -> maps.computeIfAbsent(id, this::load)
				.map(map -> new SubTexture(rasteriser.createTexture(map.toImage(), TextureFilter.NEAREST), 0, 0, 1, 1))
				.orElse(null));
	}

	private Optional<SavedMap> load(int mapId) {
		Path path = dataDir.resolve("minecraft/maps/" + mapId + ".dat");
		if (Files.notExists(path))
			path = dataDir.resolve("map_" + mapId + ".dat");
		try {
			return Optional.of(SavedMap.load(path, legacyPalette));
		} catch (IOException | NegativeArraySizeException e) {
			log.warn("Could not load saved map {} from {}: {}", mapId, path, e.toString());
			return Optional.empty();
		}
	}

	@Override
	public void close() {
		for (SubTexture texture : textures.values())
			rasteriser.destroyTexture(texture.texture);
		textures.clear();
		maps.clear();
	}
}
