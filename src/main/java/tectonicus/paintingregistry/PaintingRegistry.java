/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.paintingregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PaintingRegistry {
	private final Map<String, PaintingVariant> paintings = new HashMap<>();
	private final ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public PaintingRegistry(TexturePack texturePack) {
		this.zips = texturePack.getZipStack();
		log.info("Loading all painting variant json files...");
		deserializePaintingJson();
		log.debug("Loaded {} painting json files.", paintings.size());
	}
	
	private void deserializePaintingJson() {
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null);
			 DirectoryStream<Path> variants = Files.newDirectoryStream(fs.getPath("/data/minecraft/painting_variant"))) {
			for (Path variantJson : variants) {
				PaintingVariant variant = OBJECT_MAPPER.readValue(Files.newBufferedReader(variantJson, StandardCharsets.UTF_8), PaintingVariant.class);
				paintings.put(variant.getAssetId(), variant);
			}
		} catch (NotDirectoryException e) {
			log.warn("No painting variant data found. This is probably an older version of Minecraft.");
		} catch (IOException e) {
			log.error("Error reading painting json", e);
		}
	}
	
	public PaintingVariant get(String variant) {
		return paintings.get(variant);
	}
	
	public boolean isEmpty() {
		return paintings.isEmpty();
	}
}
