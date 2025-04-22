/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemmodeldefinitionregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
		log.debug("Loading item model definition json from minecraft jar");
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null)) {
                        Path itemModelDefinitionsPath = fs.getPath("/assets/minecraft/items");
                        if (Files.exists(itemModelDefinitionsPath)) {
                                try (DirectoryStream<Path> entries = Files.newDirectoryStream(itemModelDefinitionsPath)) {
                                        deserializeItemModels(entries);
                                }                                
                        }                            			
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
	}

	private void deserializeItemModels(DirectoryStream<Path> entries) throws IOException {
		for (Path itemJsonFile : entries) {
			ItemModelDefinition itemModel = OBJECT_MAPPER.readValue(Files.newBufferedReader(itemJsonFile, StandardCharsets.UTF_8), ItemModelDefinition.class);
			String name = StringUtils.removeEnd(itemJsonFile.getFileName().toString(), ".json");
			modelDefinitions.put(name, itemModel);
		}
	}
}
