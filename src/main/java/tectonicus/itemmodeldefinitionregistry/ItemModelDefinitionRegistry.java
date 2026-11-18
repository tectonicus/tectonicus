/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemmodeldefinitionregistry;

import tools.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ItemModelDefinitionRegistry {
	@Getter
	private final Map<String, ItemModelDefinition> modelDefinitions = new HashMap<>();
	private final ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public ItemModelDefinitionRegistry(TexturePack texturePack) {
		this.zips = texturePack.getZipStack();
		log.info("Loading all item model definition json files...");
		deserializeItemModels();
		log.info("All item model definition json files loaded.");
	}

	public void deserializeItemModels() {
		log.debug("Loading item model definition json from minecraft jar and resource packs");
		try {
			for (String file : zips.listFilesInDirectory("assets/minecraft/items")) {
				String fileName = file.substring(file.lastIndexOf('/') + 1);
				if (!fileName.toLowerCase().endsWith(".json"))
					continue;
				// The highest-priority resource pack that has the definition replaces it as a whole.
				try (InputStream in = zips.getStream(file)) {
					if (in != null)
						modelDefinitions.put(Strings.CI.removeEnd(fileName, ".json"),
								OBJECT_MAPPER.readValue(new InputStreamReader(in, StandardCharsets.UTF_8), ItemModelDefinition.class));
				}
			}
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
	}
}
