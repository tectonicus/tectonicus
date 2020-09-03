/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import tectonicus.Minecraft;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


@Log4j2
@NoArgsConstructor
public class BlockRegistry
{
	@Getter
	private final Map<String, BlockStateWrapper> blockStates = new HashMap<>();
	@Getter
	private final Map<String, BlockModel> blockModels = new HashMap<>();
	private TexturePack texturePack;
	private ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String ELEMENTS_FIELD = "elements";
	private static final String TEXTURES_FIELD = "textures";
	private static final String ROTATION_FIELD = "rotation";


	public BlockRegistry(String blah)
	{
		try {
			zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public BlockRegistry(Rasteriser rasteriser)
	{
		File minecraftJar = Minecraft.findMinecraftJar();
		texturePack = new TexturePack(rasteriser, minecraftJar, null, Collections.emptyList());
		try {
			zips = new ZipStack(minecraftJar, null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public BlockRegistry(TexturePack texturePack)
	{
		this.texturePack = texturePack;
		this.zips = texturePack.getZipStack();
		log.info("Loading all block state and block model json files...");
		deserializeBlockstates();
		log.info("All json files loaded.");
	}

	public BlockStateWrapper getBlock(String blockName) {
		return blockStates.get(blockName);
	}
	public BlockModel getModel(String model) { return blockModels.get(model); }
	
	
	public void deserializeBlockstates()
	{
		//TODO: need to use override pack blockstate files first
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null);
			DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"));)
		{
			for (Path blockStateFile : entries)
			{
				String name = "minecraft:" + StringUtils.removeEnd(blockStateFile.getFileName().toString(), ".json");
				JsonNode root = OBJECT_MAPPER.readTree(Files.newBufferedReader(blockStateFile, StandardCharsets.UTF_8));

				BlockStateWrapper states = new BlockStateWrapper();
				if (root.has("multipart")) {
					root.get("multipart").forEach(node -> {
						List<Map<String, String>> whenClauses = new ArrayList<>();
						if (node.has("when")) {
							JsonNode whenField = node.get("when");
							if (whenField.has("OR")) {
								whenField.get("OR").forEach(whenClause -> whenClauses.add(parseStates(whenClause)));
							} else {
								whenClauses.add(parseStates(whenField));
							}
						}

						states.addCase(new BlockStateCase(whenClauses, deserializeBlockStateModels(node.get("apply"))));
					});
				} else {
					JsonNode variants = root.get("variants");

					Iterator<Entry<String, JsonNode>> iter = variants.fields();
					while (iter.hasNext()) {
						Map.Entry<String, JsonNode> entry = iter.next();
						String key = entry.getKey();
						BlockVariant blockVariant = new BlockVariant(key, deserializeBlockStateModels(entry.getValue()));
						states.addVariant(blockVariant);
					}

				}
				blockStates.put(name, states);
			}				
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private Map<String, String> parseStates(JsonNode when) {
		Map<String, String> states = new HashMap<>();
		Iterator<Entry<String, JsonNode>> iter = when.fields();

		while (iter.hasNext()) {
			Map.Entry<String, JsonNode> entry = iter.next();
			states.put(entry.getKey(), entry.getValue().asText());
		}

		return states;
	}

	public List<BlockStateModel> deserializeBlockStateModels(JsonNode models) {
		List<BlockStateModel> stateModels = new ArrayList<>();
		try {
			if (models.isArray()) {
				stateModels = OBJECT_MAPPER.readValue(models.toString(), new TypeReference<List<BlockStateModel>>(){});
			} else {
				stateModels = OBJECT_MAPPER.readValue("[" + models.toString() + "]", new TypeReference<List<BlockStateModel>>(){});
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		try {
			loadBlockStateModels(stateModels);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return stateModels;
	}

	public void loadBlockStateModels(List<BlockStateModel> models) throws Exception {
		for(BlockStateModel stateModel : models)
		{
			String modelPath = stateModel.getModel();
			if(stateModel.getBlockModel() == null)
			{
				stateModel.setBlockModel(loadModel(modelPath));
			}
		}
	}
	
	public BlockModel loadModel(String modelName) throws Exception
	{
		if (!modelName.contains("block/")) {
			modelName = "block/" + modelName;
		}

		BlockModel model = loadModel(modelName, StringUtils.EMPTY, new HashMap<>(), null);

		blockModels.put(modelName, model);
		return model;
	}
	
	// Recurse through model files and get block model information
	public BlockModel loadModel(String modelPath, String modelName, Map<String, String> textureMap, JsonNode elements) throws Exception
	{
		if (modelPath.contains("minecraft:")) {
			modelPath = modelPath.replace("minecraft:", "");
		}

		if (modelName.equals(StringUtils.EMPTY)) {
			modelName = modelPath;
		}

		JsonNode json = OBJECT_MAPPER.readTree(new InputStreamReader(zips.getStream("assets/minecraft/models/" + modelPath + ".json")));
		
		String parent = "";
		if(json.has("parent")) // Get texture information and then load parent file
		{
			parent = json.get("parent").asText();

			if(json.has(ELEMENTS_FIELD) && elements == null)
			{
				elements = json.get(ELEMENTS_FIELD);
			}
			
			if(json.has(TEXTURES_FIELD))
			{
				return loadModel(parent, modelName, populateTextureMap(textureMap, json.get(TEXTURES_FIELD)), elements);
			}
			else
			{
				return loadModel(parent, modelName, textureMap, elements);
			}
		}
		else if(json.has(ELEMENTS_FIELD) || elements != null)//Load all elements
		{
			Map<String, String> combineMap = new HashMap<>(textureMap);
			if(json.has(TEXTURES_FIELD))
			{
				combineMap.putAll(populateTextureMap(textureMap, json.get(TEXTURES_FIELD)));
			}

			boolean ao = true;
			if (json.has("ambientocclusion")) {
				ao = json.get("ambientocclusion").asBoolean();
			}
			
			if(json.has(ELEMENTS_FIELD) && elements == null)
			{
				elements = json.get(ELEMENTS_FIELD);
			}			

			return new BlockModel(modelName, ao, combineMap, elements, texturePack);
		} else {  //TODO: There is no block model so we need to use our own model for these blocks
			return new BlockModel(modelName, false, null, null, null);
		}
	}

	private Map<String, String> populateTextureMap(Map<String, String> textureMap, JsonNode textures)
	{
		Map<String, String> newTexMap = new HashMap<>(textureMap);

		Iterator<Entry<String, JsonNode>> iter = textures.fields();

		while (iter.hasNext()) {
			Map.Entry<String, JsonNode> entry = iter.next();

			String key = entry.getKey();
			StringBuilder tex = new StringBuilder(entry.getValue().asText());

			if(tex.charAt(0) == '#')
			{
				newTexMap.put(key, textureMap.get(tex.deleteCharAt(0).toString()));
			}
			else
			{
				newTexMap.put(key, tex.toString());
			}
		}
		
		return newTexMap;
	}
}
