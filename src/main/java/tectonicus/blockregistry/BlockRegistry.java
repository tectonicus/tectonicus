/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tectonicus.Minecraft;
import tectonicus.configuration.MutableConfiguration;
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
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static tectonicus.blockregistry.BlockState.getRandomWeightedModel;


@Slf4j
@NoArgsConstructor
public class BlockRegistry
{
	@Getter
	private final Cache<String, BlockStateWrapper> blockStates = Caffeine.newBuilder().build();
	@Getter
	private final Cache<String, BlockModel> blockModels = Caffeine.newBuilder().build();
	@Getter
	private final Cache<String, BlockStateModelsWeight> singleVariantBlocks = Caffeine.newBuilder().build();
	private final Set<String> missingBlockModels = new HashSet<>();
	private TexturePack texturePack;
	private ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final ObjectReader OBJECT_READER = OBJECT_MAPPER.readerFor(JsonNode.class).with(JsonReadFeature.ALLOW_TRAILING_COMMA);
	private static final String ELEMENTS_FIELD = "elements";
	private static final String TEXTURES_FIELD = "textures";
        
        private static final Map<String, String> renamedBlocks = Map.ofEntries(
                new AbstractMap.SimpleImmutableEntry<>("minecraft:grass", "minecraft:short_grass") // Was renamed in 1.20.3
        );
		
	
	public BlockRegistry(Rasteriser rasteriser) //This is only used by the draw model test code
	{
		File minecraftJar = Minecraft.findLatestMinecraftJar();
		MutableConfiguration config = new MutableConfiguration();
		config.setMinecraftJar(minecraftJar);
		texturePack = new TexturePack(rasteriser, config, Collections.emptyList(), Collections.emptyList());
		try {
			zips = new ZipStack(minecraftJar, null, null);
		} catch (IOException e) {
			log.error("Exception: ", e);
		}
	}
	
	public BlockRegistry(TexturePack texturePack)
	{
		this.texturePack = texturePack;
		this.zips = texturePack.getZipStack();
		log.info("Loading all block state and block model json files...");
		deserializeBlockstates();
		deserializeModels();
		missingBlockModels.forEach(s -> log.warn("Missing block model json file: {}", s));
		checkBlockAttributes();
		log.info("All json files loaded.");
	}

	public BlockStateWrapper getBlock(String blockName) {
                BlockStateWrapper result = blockStates.getIfPresent(blockName);
                
                String newName;
                if (result == null && (newName = renamedBlocks.get(blockName)) != null) {
                    result = getBlock(newName);
                }
                
                return result;
	}
	public BlockStateModel getSingleVariantModel(String blockName) {
		return getRandomWeightedModel(singleVariantBlocks.getIfPresent(blockName));
	}
	public BlockModel getModel(String model) { return blockModels.getIfPresent(model); }
	public boolean containsSingleVariantBlock(String blockName) {
		return singleVariantBlocks.getIfPresent(blockName) != null;
	}
	
	public void deserializeBlockstates() {
		log.debug("Loading blockstate json from minecraft jar");
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null);
			 DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"))) {
			deserializeBlockstates(entries);
		} catch (NotDirectoryException e) {
			log.warn("No blockstates directory found. This is probably a pre 1.8 minecraft jar.");
		} catch (IOException e) {
			log.error("Exception: ", e);
		}

		if (zips.getOverrideFileName() != null) {
			log.debug("Loading blockstate json from resource pack");
			try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getOverrideFileName()), null);
				 DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"))) {
				deserializeBlockstates(entries);
			} catch(NotDirectoryException e) {
				log.info("No blockstate directory found in resource pack");
			} catch (Exception e) {
				log.error("Exception: ", e);
			}
		}
	}

	private void deserializeBlockstates(DirectoryStream<Path> entries) throws IOException {
		for (Path blockStateFile : entries) {
			if (blockStateFile.getFileName().toString().toLowerCase().endsWith(".json")) {
				String name = "minecraft:" + StringUtils.removeEnd(blockStateFile.getFileName().toString(), ".json");
				singleVariantBlocks.invalidate(name);  // This is needed when loading resource packs as some blocks may change to having multiple variants
				log.trace("Parsing {}.json", name);
				JsonNode root = OBJECT_MAPPER.readTree(Files.newBufferedReader(blockStateFile, StandardCharsets.UTF_8));

				BlockStateWrapper states = new BlockStateWrapper(name);
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

						states.addState(new BlockStateCase(whenClauses, new BlockStateModelsWeight(deserializeBlockStateModels(node.get("apply")))));
					});
				} else if (root.has("variants")) {
					JsonNode variants = root.get("variants");

					Iterator<Entry<String, JsonNode>> iter = variants.fields();
					while (iter.hasNext()) {
						Map.Entry<String, JsonNode> entry = iter.next();
						String key = entry.getKey();
						List<BlockStateModel> blockStateModels = deserializeBlockStateModels(entry.getValue());
						BlockStateModelsWeight modelsAndWeight = new BlockStateModelsWeight(blockStateModels);
						if (!key.contains("=")) {
							singleVariantBlocks.put(name, modelsAndWeight);
						}
						BlockVariant blockVariant = new BlockVariant(key, modelsAndWeight);
						states.addState(blockVariant);
					}
				} else {
					log.warn("Invalid blockstate file: {}", blockStateFile);
				}

				blockStates.put(name, states);
			}
		}
	}

	public void deserializeModels() {
		log.debug("Loading model json");
		try {
			int modelCount = 0;
			for (BlockStateWrapper blockStateWrapper : blockStates.asMap().values()) {
				List<BlockStateModel> models = blockStateWrapper.getAllModels();
				for (BlockStateModel model : models) {
					log.trace("Loading model: {} for {}", model, blockStateWrapper.getBlockName());
					model.setBlockModel(loadModel(model.getModel()));
					modelCount++;
				}
			}
			log.debug("Loaded {} models", modelCount);
		} catch (Exception e) {
			log.error("Something bad happened", e);
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
				stateModels = OBJECT_MAPPER.readValue("[" + models + "]", new TypeReference<List<BlockStateModel>>(){});
			}
		} catch (JsonProcessingException e) {
			log.error("Exception: ", e);
		}

		return stateModels;
	}
	
	public BlockModel loadModel(String modelName) throws Exception
	{
		if (!modelName.contains("block/")) {
			modelName = "block/" + modelName;
		}

                return blockModels.get(modelName, (mn) -> {
                        try
                        {
                                return loadModel(mn, StringUtils.EMPTY, new HashMap<>(), null);
                        }
                        catch (Exception e)
                        {
                                log.error("Exception: ", e);
                                return null;
                        }
                });
	}
	
	// Recurse through model files and get block model information
	public BlockModel loadModel(String modelPath, String modelName, Map<String, String> textureMap, JsonNode elements) throws Exception
	{
		//TODO: we may need to deal with this namespace for future mod support
		if (modelPath.contains("minecraft:")) {
			modelPath = modelPath.replace("minecraft:", "");
		}

		if (modelName.equals(StringUtils.EMPTY)) {
			modelName = modelPath;
		}

		String fullModelPath = "assets/minecraft/models/" + modelPath + ".json";
		JsonNode json;
		if (zips.hasFile(fullModelPath)) {
			json = OBJECT_READER.readTree(new InputStreamReader(zips.getStream(fullModelPath)));
		} else {
			missingBlockModels.add(fullModelPath);
			return null;
		}
		
		String parent;
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
				Map<String, String> textures = populateTextureMap(textureMap, json.get(TEXTURES_FIELD));
				for (Map.Entry<String, String> entry : textures.entrySet()) {
					if (entry.getValue() != null) {
						combineMap.putIfAbsent(entry.getKey(), entry.getValue());
					}
				}
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
				String texture = textureMap.get(tex.deleteCharAt(0).toString());
				if (texture != null) {
					newTexMap.put(key, texture);
				}
			}
			else
			{
				newTexMap.put(key, tex.toString());
			}
		}
		
		return newTexMap;
	}

	private void checkBlockAttributes() {
		for (BlockStateWrapper wrapper : blockStates.asMap().values()) {
                        for (BlockState state : wrapper.getStates()) {
				setBlockAttributes(wrapper, state.getModelsAndWeight().getModels());
			}
		}
	}

	//Set attributes on the entire block instead of just the individuals models. These are used to help with lighting and face culling
	private void setBlockAttributes(BlockStateWrapper wrapper, List<BlockStateModel> models) {
		for (BlockStateModel model : models) {
			BlockModel blockModel = model.getBlockModel();
			if (blockModel != null) {
				if (wrapper.isFullBlock() && !blockModel.isFullBlock()) {
					wrapper.setFullBlock(false);
				}

				if (!wrapper.isTransparent() && blockModel.isTranslucent() || wrapper.isFullBlock() && !blockModel.isSolid()) {
					wrapper.setTransparent(true);
				}
			}
		}
	}
}
