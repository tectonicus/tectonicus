/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemregistry;

import tools.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ItemRegistry {
	@Getter
	private final Map<String, ItemModel> models = new HashMap<>();
	private final ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public ItemRegistry(TexturePack texturePack) {
		this.zips = texturePack.getZipStack();
		log.info("Loading all item model json files...");
		deserializeItemModels();
	}
	
	public void deserializeItemModels() {
		log.debug("Loading item json from minecraft jar and resource packs");
		try {
			List<String> files = zips.listFilesInDirectory("assets/minecraft/models/item");
			if (files.isEmpty()) {
				log.warn("No item models directory found.");
				return;
			}
			for (String file : files) {
				String fileName = file.substring(file.lastIndexOf('/') + 1);
				if (!fileName.toLowerCase().endsWith(".json"))
					continue;
				// The highest-priority resource pack that has the model replaces it as a whole.
				try (InputStream in = zips.getStream(file)) {
					if (in != null)
						models.put(Strings.CI.removeEnd(fileName, ".json"),
								OBJECT_MAPPER.readValue(new InputStreamReader(in, StandardCharsets.UTF_8), ItemModel.class));
				}
			}
			log.info("All item json files loaded.");
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
	}
	
	public ItemModel findUltimatePredecessor(ItemModel itemModel) {
		while (true) {
			String parentKey = Strings.CI.removeStart(itemModel.getParent(), "minecraft:");
			parentKey = Strings.CI.removeStart(parentKey, "item/");
			if (!models.containsKey(parentKey)) {
				return itemModel;
			}
			itemModel = models.get(parentKey);
		}
	}
	
	/**
	 * @return the textures of the model, or of its nearest parent that has some, such as the template that spawn eggs
	 *         use from 1.13 to 1.21.4; null if no model in the chain has textures
	 */
	public Map<String, String> findTextures(ItemModel itemModel) {
		for (int depth = 0; itemModel != null && depth < 32; depth++) {
			if (itemModel.getTextures() != null)
				return itemModel.getTextures();
			if (itemModel.getParent() == null)
				return null;
			String parentKey = Strings.CI.removeStart(itemModel.getParent(), "minecraft:");
			itemModel = models.get(Strings.CI.removeStart(parentKey, "item/"));
		}
		return null;
	}
	
	public List<Map<String, ArrayList<Float>>> getTransformsList(ItemModel itemModel) {
		List<Map<String, ArrayList<Float>>> transforms = new ArrayList<>();
		while (true) {
			transforms.add(itemModel.getTransform());
			String parentKey = Strings.CI.removeStart(itemModel.getParent(), "minecraft:");
			parentKey = Strings.CI.removeStart(parentKey, "item/");
			if (!models.containsKey(parentKey)) {
				return transforms;
			}
			itemModel = models.get(parentKey);
		}
	}
}
