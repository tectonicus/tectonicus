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
import org.apache.commons.lang3.StringUtils;
import tectonicus.Minecraft;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tectonicus.util.Vector3f;

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
import java.util.Random;


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
	
	public BlockRegistry()
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
	}

	public BlockModel getModel(String model) { return blockModels.get(model); }
	
	
	public void deserializeBlockstates()
	{
		//TODO: need to use override pack blockstate files first
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null);
			DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"));)
		{
			int multipartTotal = 0;
			for (Path blockStateFile : entries)
			{
				String name = "minecraft:" + StringUtils.removeEnd(blockStateFile.getFileName().toString(), ".json");
				JsonNode root = OBJECT_MAPPER.readTree(Files.newBufferedReader(blockStateFile, StandardCharsets.UTF_8));

				BlockStateWrapper states = new BlockStateWrapper();
				if (root.has("multipart")) {
					multipartTotal += 1;

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

	public static List<BlockStateModel> deserializeBlockStateModels(JsonNode models) {
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
		return stateModels;
	}

	public void loadModels(List<BlockStateModel> models) throws Exception {
		for(BlockStateModel model : models)
		{
			String modelName = model.getModel();
			if(!blockModels.containsKey(modelName))
			{
				loadModel(modelName);
			}
		}
	}

	public void loadModels() throws Exception {
		for (Map.Entry<String, BlockStateWrapper> entry : blockStates.entrySet()) {
			BlockStateWrapper states = entry.getValue();
			if (!states.getCases().isEmpty()) {
				for(BlockStateCase bsc : states.getCases()) {
					loadModels(bsc.getModels());
				}
			} else if (!states.getVariants().isEmpty()){
				for(BlockVariant variant : states.getVariants()) {
					loadModels(variant.getModels());
				}
			} else {
				//Error no cases or variants found
			}
		}
	}
	
	public void loadModel(String modelName) throws Exception
	{
		if (!modelName.contains("block/")) {
			modelName = "block/" + modelName;
		}
		blockModels.put(modelName, loadModel(modelName, new HashMap<>(), null));
	}
	
	// Recurse through model files and get block model information
	public BlockModel loadModel(String modelPath, Map<String, String> textureMap, JsonNode elements) throws Exception
	{
		if (modelPath.contains("minecraft:")) {
			modelPath = modelPath.replace("minecraft:", "");
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
				return loadModel(parent, populateTextureMap(textureMap, json.get(TEXTURES_FIELD)), elements);
			}
			else
			{
				return loadModel(parent, textureMap, elements);
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
			if (json.has("ambientocclusion"))  //TODO: what is this doing?
				ao = false;
			
			if(json.has(ELEMENTS_FIELD) && elements == null)
			{
				elements = json.get(ELEMENTS_FIELD);
			}			
			
			return new BlockModel(modelPath, ao, deserializeBlockElements(combineMap, elements));
		} else {  //TODO: There is no block model so we need to use our own model for these blocks
			return new BlockModel(modelPath, false, Collections.emptyList());
		}
	}

	private List<BlockElement> deserializeBlockElements(Map<String, String> combineMap,	JsonNode elements)
	{
		List<BlockElement> elementsList = new ArrayList<>();
		
		for(JsonNode element : elements)
		{
			JsonNode from = element.get("from");
			Vector3f fromVector = new Vector3f(from.get(0).floatValue(), from.get(1).floatValue(), from.get(2).floatValue());
			JsonNode to = element.get("to");
			Vector3f toVector = new Vector3f(to.get(0).floatValue(), to.get(1).floatValue(), to.get(2).floatValue());
			
			org.joml.Vector3f rotationOrigin = new org.joml.Vector3f(8.0f, 8.0f, 8.0f);
			String rotationAxis = "y";
			org.joml.Vector3f rotAxis = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
			float rotationAngle = 0;
			boolean rotationScale = false;
			
			if(element.has(ROTATION_FIELD))
			{
				JsonNode rot = element.get(ROTATION_FIELD);
				JsonNode rotOrigin = rot.get("origin");
				rotationOrigin = new org.joml.Vector3f(rotOrigin.get(0).floatValue(), rotOrigin.get(1).floatValue(), rotOrigin.get(2).floatValue());

				rotationAxis = rot.get("axis").asText();
				if (rotationAxis.equals("x"))
					rotAxis = new org.joml.Vector3f(1.0f, 0.0f, 0.0f);
				else
					rotAxis = new org.joml.Vector3f(0.0f, 0.0f, 1.0f);
				
				rotationAngle = rot.get("angle").floatValue();
				
				if(element.has("rescale"))
					rotationScale = true;
			}
			
			boolean shaded = true;
			if(element.has("shade"))  //TODO: again what is this doing?  I don't think this is right
				shaded = false;						
			
			JsonNode faces = element.get("faces");
			SubTexture subTexture = new SubTexture(null, fromVector.x(), 16-toVector.y(), toVector.x(), 16-fromVector.y());
			BlockElement be = new BlockElement(fromVector, toVector, rotationOrigin, rotAxis, rotationAngle, rotationScale, shaded, deserializeElementFaces(combineMap, subTexture, faces, fromVector, toVector));
			elementsList.add(be);
		}
		return elementsList;
	}

	private Map<String, ElementFace> deserializeElementFaces(Map<String, String> combineMap, SubTexture texCoords, JsonNode faces, Vector3f fromVector, Vector3f toVector)
	{
		Map<String, ElementFace> elementFaces = new HashMap<>();

		Iterator<Entry<String, JsonNode>> iter = faces.fields();
		while (iter.hasNext()) {
			Map.Entry<String, JsonNode> entry = iter.next();

			String key = entry.getKey();
			JsonNode face = entry.getValue();

			float u0 = texCoords.u0;
			float v0 = texCoords.v0;
			float u1 = texCoords.u1;
			float v1 = texCoords.v1;

			if (key.equals("up") || key.equals("down"))
			{
				v0 = fromVector.z();
				v1 = toVector.z();
			}
			else if (key.equals("north"))
			{
				u0 = 16 - texCoords.u1;
				u1 = 16 - texCoords.u0;
			}
			else if (key.equals("east"))
			{
				u0 = 16 - toVector.z();
				u1 = 16 - fromVector.z();
			}
			else if (key.equals("west"))
			{
				u0 = fromVector.z();
				u1 = toVector.z();
			}


			int rotation = 0;
			if(face.has(ROTATION_FIELD))
				rotation = face.get(ROTATION_FIELD).asInt();

			//System.out.println("u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
			// TODO: Need to test more texture packs
			SubTexture subTexture = new SubTexture(null, u0*(1.0f/16.0f), v0*(1.0f/16.0f), u1*(1.0f/16.0f), v1*(1.0f/16.0f));

			StringBuilder tex = new StringBuilder(face.get("texture").asText());
			if(tex.charAt(0) == '#')
			{
				String texture = tex.deleteCharAt(0).toString();

				String texturePath = combineMap.get(texture);
				if (texturePath.contains("minecraft:block/")) { // 1.16+
					texturePath = texturePath.replace("minecraft:block/", "") + ".png";
				} else if (texturePath.contains("block/")) { // 1.13 - 1.15
					texturePath = StringUtils.removeStart(combineMap.get(texture), "block/") + ".png";
				} else { // 1.8 - 1.12
					texturePath = StringUtils.removeStart(combineMap.get(texture), "blocks/") + ".png";
				}

				SubTexture te = texturePack.findTexture(texturePath);

				final float texHeight = te.texture.getHeight();
				final float texWidth = te.texture.getWidth();
				final int numTiles = te.texture.getHeight()/te.texture.getWidth();

				u0 /= texWidth;
				v0 = (v0 / texWidth) / numTiles;
				u1 /= texWidth;
				v1 = (v1 / texWidth) / numTiles;

				if(face.has("uv"))
				{
					//System.out.println("Before: u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
					JsonNode uv = face.get("uv");
					u0 = uv.get(0).floatValue()/16.0f;
					v0 = (uv.get(1).floatValue()/16.0f) / numTiles;
					u1 = uv.get(2).floatValue()/16.0f;
					v1 = (uv.get(3).floatValue()/16.0f) / numTiles;
				}

				//System.out.println(texWidth + " x " + texHeight);
				int frame = 1;
				if(numTiles > 1)
				{
					Random rand = new Random();
					frame = rand.nextInt(numTiles)+1;
				}

				subTexture = new SubTexture(te.texture, u0, v0+(float)(frame-1)*(texWidth/texHeight), u1, v1+(float)(frame-1)*(texWidth/texHeight));
				//subTexture = new SubTexture(test, u0, v0, u1, v1);
				//System.out.println("u0="+subTexture.u0+" v0="+subTexture.v0+" u1="+subTexture.u1+" v1="+subTexture.v1);
			}

			boolean cullFace = false;
			if(face.has("cullface"))
				cullFace = true;

			boolean tintIndex = false;
			if(face.has("tintindex"))
				tintIndex = true;

			ElementFace ef = new ElementFace(subTexture, cullFace, rotation, tintIndex);
			elementFaces.put(key, ef);
		}

		return elementFaces;
	}
	
	private Map<String, String> populateTextureMap(Map<String, String> textureMap, JsonNode textures)
	{
		Map<String, String> newTexMap = new HashMap<>();

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
