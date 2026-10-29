/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.configuration.Configuration;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.BlockProperties;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static tectonicus.blockregistry.BlockState.getRandomWeightedModel;


@Slf4j
@NoArgsConstructor
public class BlockRegistry {
	@Getter
	private final Cache<String, BlockStateWrapper> blockStates = Caffeine.newBuilder().build();
	@Getter
	private final Cache<String, BlockModel> blockModels = Caffeine.newBuilder().build();
	@Getter
	private final Cache<String, BlockStateModelsWeight> singleVariantBlocks = Caffeine.newBuilder().build();
	private final Set<String> missingBlockModels = new HashSet<>();
	private final Map<BakeKey, BakedBlockStateModel> bakedModels = new ConcurrentHashMap<>();
	private Map<String, Map<String, String>> defaultBlockStates = Collections.emptyMap();
	private TexturePack texturePack;
	private ZipStack zips;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final ObjectReader OBJECT_READER = OBJECT_MAPPER.readerFor(JsonNode.class).with(JsonReadFeature.ALLOW_TRAILING_COMMA);
	private static final String ELEMENTS_FIELD = "elements";
	private static final String TEXTURES_FIELD = "textures";
	
	private static final Map<String, String> renamedBlocks = Map.ofEntries(
		new AbstractMap.SimpleImmutableEntry<>("minecraft:grass", "minecraft:short_grass"),     // Was renamed in 1.20.3
		new AbstractMap.SimpleImmutableEntry<>("minecraft:chain", "minecraft:iron_chain")       // Renamed in 1.21.9 with the addition of copper chains
	);
	
	
	public BlockRegistry(Rasteriser rasteriser) //This is only used by the draw model test code
	{
		File minecraftJar = Minecraft.findLatestMinecraftJar();
		Configuration config = new Configuration();
		config.setMinecraftJar(minecraftJar);
		texturePack = new TexturePack(rasteriser, config, Collections.emptyList(), Collections.emptyList());
		try {
			zips = new ZipStack(minecraftJar, null, null);
		} catch (IOException e) {
			log.error("Exception: ", e);
		}
	}
	
	public BlockRegistry(TexturePack texturePack) {
		this.texturePack = texturePack;
		this.zips = texturePack.getZipStack();
		log.info("Loading all block state and block model json files...");
		deserializeBlockstates();
		deserializeModels();
		checkBlockAttributes();
	}
	
	public BlockStateWrapper getBlock(String blockName) {
		BlockStateWrapper result = blockStates.getIfPresent(blockName);
		
		String newName;
		if (result == null && (newName = renamedBlocks.get(blockName)) != null) {
			result = getBlock(newName);
		}
		
		return result;
	}
	
	public BlockStateModel getSingleVariantModel(String blockName, BlockProperties properties, int x, int y, int z) {
		return getRandomWeightedModel(singleVariantBlocks.getIfPresent(blockName),
			BlockState.createPositionRandom(blockName, properties, x, y, z));
	}
	
	public BlockModel getModel(String model) {
		return blockModels.getIfPresent(model);
	}

	public BakedBlockStateModel getBakedModel(BlockModel model, int xRotation, int yRotation, int zRotation, boolean uvlock) {
		if (model == null)
			return null;
		BakeKey key = new BakeKey(model, xRotation, yRotation, zRotation, uvlock);
		return bakedModels.computeIfAbsent(key,
				ignored -> new BakedBlockStateModel(model, xRotation, yRotation, zRotation, uvlock));
	}
	
	public boolean containsSingleVariantBlock(String blockName) {
		return singleVariantBlocks.getIfPresent(blockName) != null;
	}
	
	public void deserializeBlockstates() {
		log.debug("Loading blockstate json from minecraft jar");
		loadDefaultBlockStates();
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()));
			 DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"))) {
			deserializeBlockstates(entries);
		} catch (NotDirectoryException e) {
			log.warn("No blockstates directory found. This is probably a pre 1.8 minecraft jar.");
		} catch (IOException e) {
			log.error("Exception: ", e);
		}
		
		if (zips.getOverrideFileName() != null) {
			log.debug("Loading blockstate json from resource pack");
			try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getOverrideFileName()));
				 DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"))) {
				deserializeBlockstates(entries);
			} catch (NotDirectoryException e) {
				log.info("No blockstate directory found in resource pack");
			} catch (Exception e) {
				log.error("Exception: ", e);
			}
		}
		
		applyDefaultBlockStates();
	}
	
	private void loadDefaultBlockStates() {
		try (InputStream in = BlockRegistry.class.getClassLoader().getResourceAsStream("default_block_states.json")) {
			if (in == null) {
				log.warn("Default block states resource was not found");
				return;
			}
			
			defaultBlockStates = OBJECT_MAPPER.readValue(in, new TypeReference<>() {
			});
			log.debug("Loaded {} default block states", defaultBlockStates.size());
		} catch (IOException e) {
			log.error("Unable to load default block states", e);
		}
	}
	
	private void applyDefaultBlockStates() {
		for (BlockStateWrapper wrapper : blockStates.asMap().values()) {
			Map<String, String> defaultProperties = defaultBlockStates.get(wrapper.getBlockName());
			if (defaultProperties != null) {
				wrapper.setDefaultProperties(defaultProperties);
			}
		}
	}
	
	private void deserializeBlockstates(DirectoryStream<Path> entries) throws IOException {
		for (Path blockStateFile : entries) {
			String fileName = blockStateFile.getFileName().toString();
			JsonNode root = null;
			if (fileName.toLowerCase().endsWith(".json")) {
				if (fileName.contains("copper_golem_statue")) { //Use custom blockstate files for copper golem statues
					try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("blockstates/" + fileName)) {
						if (in != null) {
							log.trace("Loading local blockstate file for: {}", fileName);
							root = OBJECT_MAPPER.readTree(new InputStreamReader(in));
						}
					}
				} else {
					root = OBJECT_MAPPER.readTree(Files.newBufferedReader(blockStateFile, StandardCharsets.UTF_8));
				}
				
				String name = "minecraft:" + Strings.CI.removeEnd(fileName, ".json");
				singleVariantBlocks.invalidate(name);  // This is needed when loading resource packs as some blocks may change to having multiple variants
				log.trace("Parsing {}.json", name);
				
				BlockStateWrapper states = new BlockStateWrapper(name);
				if (root != null && root.has("multipart")) {
					root.get("multipart").forEach(node -> {
						List<Map<String, String>> whenClauses = new ArrayList<>();
						if (node.has("when")) {
							JsonNode whenField = node.get("when");
							if (whenField.has("OR")) {
								whenField.get("OR").forEach(whenClause -> whenClauses.add(parseStates(whenClause)));
							} else if (whenField.has("AND")) {
								Map<String, String> andClause = new HashMap<>();
								whenField.get("AND").forEach(whenCondition -> andClause.putAll(parseStates(whenCondition)));
								whenClauses.add(andClause);
							} else {
								whenClauses.add(parseStates(whenField));
							}
						}
						
						states.addState(new BlockStateCase(whenClauses, new BlockStateModelsWeight(deserializeBlockStateModels(node.get("apply")))));
					});
				} else if (root != null && root.has("variants")) {
					JsonNode variants = root.get("variants");
					
					for (Entry<String, JsonNode> entry : variants.properties()) {
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
					model.setBakedModel(getBakedModel(model.getBlockModel(), model.getXRotation(), model.getYRotation(),
							model.getZRotation(), model.isUvlock()));
					modelCount++;
				}
			}
			log.debug("Loaded {} models", modelCount);
			missingBlockModels.forEach(s -> log.warn("Missing block model json file: {}", s));
			
			if (modelCount > 0 && missingBlockModels.isEmpty())
				log.info("All json files loaded.");
			
		} catch (Exception e) {
			log.error("Something bad happened", e);
		}
	}

	private record BakeKey(BlockModel model, int xRotation, int yRotation, int zRotation, boolean uvlock) { }
	
	private Map<String, String> parseStates(JsonNode when) {
		Map<String, String> states = new HashMap<>();
		
		for (Entry<String, JsonNode> entry : when.properties()) {
			states.put(entry.getKey(), entry.getValue().asString());
		}
		
		return states;
	}
	
	public List<BlockStateModel> deserializeBlockStateModels(JsonNode models) {
		List<BlockStateModel> stateModels = new ArrayList<>();
		try {
			if (models.isArray()) {
				stateModels = OBJECT_MAPPER.readValue(models.toString(), new TypeReference<List<BlockStateModel>>() {
				});
			} else {
				stateModels = OBJECT_MAPPER.readValue("[" + models + "]", new TypeReference<List<BlockStateModel>>() {
				});
			}
		} catch (JacksonException e) {
			log.error("Exception: ", e);
		}
		
		return stateModels;
	}
	
	public BlockModel loadModel(String modelName) {
		String modelPath = modelName.replace("minecraft:", "");
		// Older blockstates omit block/. Modern packs can instead use paths directly under models/.
		if (!modelPath.startsWith("block/") && !hasModel(modelPath) && hasModel("block/" + modelPath))
			modelName = "block/" + modelPath;

		return blockModels.get(modelName, mn -> {
			try {
				return loadModel(mn, StringUtils.EMPTY, new HashMap<>(), null);
			} catch (Exception e) {
				log.error("Exception: ", e);
				return null;
			}
		});
	}

	private boolean hasModel(String path) {
		return zips.hasFile("assets/minecraft/models/" + path + ".json")
				|| Thread.currentThread().getContextClassLoader().getResource("models/" + path + ".json") != null;
	}
	
	// Recurse through model files and get block model information
	public BlockModel loadModel(String modelPath, String modelName, Map<String, ModelTexture> textureMap, JsonNode elements) throws Exception {
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
			try (InputStream in = zips.getStream(fullModelPath)) {
				json = OBJECT_READER.readTree(in);
			}
		} else {
			if (modelPath.contains("copper_golem")) { //maintain fewer model files by using the same model file for different oxidation states of the copper golem statue and just changing the texture.
				if (modelPath.contains("standing")) {
					modelPath = "block/copper_golem_standing";
				} else if (modelPath.contains("sitting")) {
					modelPath = "block/copper_golem_sitting";
				} else if (modelPath.contains("running")) {
					modelPath = "block/copper_golem_running";
				} else if (modelPath.contains("star")) {
					modelPath = "block/copper_golem_star";
				}
			}
			
			String resourcePath = "models/" + modelPath + ".json";
			try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
				if (in != null) {
					log.trace("Loading model from local resource: {}", resourcePath);
					json = OBJECT_READER.readTree(in);
				} else {
					missingBlockModels.add(fullModelPath);
					return null;
				}
			}
		}
		
		String parent;
		if (json.has("parent")) // Get texture information and then load parent file
		{
			parent = json.get("parent").asString();
			
			if (json.has(ELEMENTS_FIELD) && elements == null) {
				elements = json.get(ELEMENTS_FIELD);
			}
			
			if (json.has(TEXTURES_FIELD)) {
				return loadModel(parent, modelName, populateTextureMap(textureMap, json.get(TEXTURES_FIELD)), elements);
			} else {
				return loadModel(parent, modelName, textureMap, elements);
			}
		} else if (json.has(ELEMENTS_FIELD) || elements != null)//Load all elements
		{
			Map<String, ModelTexture> textures = json.has(TEXTURES_FIELD)
					? populateTextureMap(textureMap, json.get(TEXTURES_FIELD)) : textureMap;
			Map<String, ModelTexture> combineMap = resolveTextureMap(textures);
			if (modelName.contains("copper_golem")) { // Share geometry between oxidation states, with version-specific textures.
				for (Entry<String, ModelTexture> entry : combineMap.entrySet()) {
					ModelTexture texture = entry.getValue();
					entry.setValue(new ModelTexture(getTextureOverride(modelName, texture.sprite(), "models/block/copper_golem_texture_overrides.json"),
							texture.forceTranslucent()));
				}
			}
			
			boolean ao = true;
			if (json.has("ambientocclusion")) {
				ao = json.get("ambientocclusion").asBoolean();
			}
			
			if (json.has(ELEMENTS_FIELD) && elements == null) {
				elements = json.get(ELEMENTS_FIELD);
			}
			
			return new BlockModel(modelName, ao, combineMap, elements, texturePack);
		} else {  //TODO: There is no block model so we need to use our own model for these blocks
			return new BlockModel(modelName, false, null, null, null);
		}
	}
	
	static Map<String, ModelTexture> populateTextureMap(Map<String, ModelTexture> textureMap, JsonNode textures) {
		Map<String, ModelTexture> newTexMap = new HashMap<>(textureMap);
		
		for (Entry<String, JsonNode> entry : textures.properties()) {
			String key = entry.getKey();
			JsonNode value = entry.getValue();
			ModelTexture texture = null;
			if (value.isString()) {
				texture = new ModelTexture(value.asString());
			} else if (value.isObject() && value.has("sprite") && value.get("sprite").isString()) {
				texture = new ModelTexture(value.get("sprite").asString(),
						value.has("force_translucent") && value.get("force_translucent").asBoolean());
			} else {
				log.warn("Unexpected texture value: {} for key: {}", value, key);
			}
			
			// Models are visited child first. Keep aliases until every ancestor has been merged.
			if (texture != null)
				newTexMap.putIfAbsent(key, texture);
		}
		
		return newTexMap;
	}
	
	static Map<String, ModelTexture> resolveTextureMap(Map<String, ModelTexture> textures) {
		Map<String, ModelTexture> resolved = new HashMap<>();
		for (String key : textures.keySet()) {
			Set<String> visited = new HashSet<>();
			visited.add(key);
			ModelTexture texture = textures.get(key);
			while (texture != null && texture.sprite().startsWith("#")) {
				String reference = texture.sprite().substring(1);
				if (!visited.add(reference)) {
					texture = null;
					break;
				}
				texture = textures.get(reference);
			}
			if (texture != null)
				resolved.put(key, texture);
		}
		return resolved;
	}
	
	private String getTextureOverride(String modelName, String texture, String resourcePath) {
		String overrideTexture = texture;
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
			if (in != null) {
				log.trace("Loading override textures from local resource");
				Map<String, String> textureOverrides = OBJECT_MAPPER.readValue(in, new TypeReference<>() {
				});
				if (texturePack.getVersion().getNumVersion() >= Version.VERSION_26_1.getNumVersion()) {
					overrideTexture = textureOverrides.getOrDefault(modelName + "_26_1", texture);
				} else {
					overrideTexture = textureOverrides.getOrDefault(modelName, texture);
				}
			}
		} catch (IOException e) {
			log.error("Exception: ", e);
		}
		
		return overrideTexture;
	}
	
	private void checkBlockAttributes() {
		for (BlockStateWrapper wrapper : blockStates.asMap().values()) {
			wrapper.setEmissiveBlock(EmissiveBlock.forBlockName(wrapper.getBlockName()));
			setBlockAttributes(wrapper);
		}
	}
	
	//Set attributes on the entire block instead of just the individuals models. These are used to help with lighting and face culling
	static void setBlockAttributes(BlockStateWrapper wrapper) {
		boolean multipart = wrapper.getStates().stream().anyMatch(BlockStateCase.class::isInstance);
		boolean hasOpaqueFullBlockModel = false;
		for (BlockStateModel model : wrapper.getAllModels()) {
			BlockModel blockModel = model.getBlockModel();
			if (blockModel != null) {
				if (wrapper.isFullBlock() && !blockModel.isFullBlock()) {
					wrapper.setFullBlock(false);
				}
				
				if (blockModel.isFullBlock()
					&& (blockModel.isSolid())) {
					hasOpaqueFullBlockModel = true;
				}
				
				if (!wrapper.isTransparent() && blockModel.isTranslucent() || wrapper.isFullBlock() && !blockModel.isSolid()) {
					wrapper.setTransparent(true);
				}
			}
		}
		
		// Multipart overlays do not remove an opaque cube supplied by the body.
		// Keep fullBlock conservative for lighting individual partial model parts;
		// alternatives such as single/double slab variants must remain partial.
		wrapper.setFullOpaqueBlock(hasOpaqueFullBlockModel && (wrapper.isFullBlock() || multipart));
	}
}
