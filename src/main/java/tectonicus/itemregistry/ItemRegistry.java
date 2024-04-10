/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
public class ItemRegistry {
	@Getter
	private final Map<String, ItemModel> models = new HashMap<>();
	private final ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public ItemRegistry(TexturePack texturePack) {
		this.zips = texturePack.getZipStack();
		log.info("Loading all item model json files...");
		deserializeItemModels();
		log.info("All item json files loaded.");
	}

	public void deserializeItemModels() {
		log.debug("Loading item json from minecraft jar");
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null);
			 DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/models/item"))) {
			deserializeItemModels(entries);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void deserializeItemModels(DirectoryStream<Path> entries) throws IOException {
		for (Path itemJsonFile : entries) {
			ItemModel itemModel = OBJECT_MAPPER.readValue(Files.newBufferedReader(itemJsonFile, StandardCharsets.UTF_8), ItemModel.class);
			String name = StringUtils.removeEnd(itemJsonFile.getFileName().toString(), ".json");
			models.put(name, itemModel);
		}
	}
}
