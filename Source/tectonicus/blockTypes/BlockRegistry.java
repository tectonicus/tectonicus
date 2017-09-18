/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import tectonicus.Minecraft;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tectonicus.util.Vector3f;

public class BlockRegistry
{
	private Map<String, List<BlockVariant>> blockStates = new HashMap<>();
	private Map<String, BlockModel> blockModels = new HashMap<>();
	private TexturePack texturePack;
	private ZipStack zips;
	
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
		texturePack = new TexturePack(rasteriser, Minecraft.findMinecraftJar(), null, Collections.<File>emptyList());
		try {
			zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, List<BlockVariant>> getBlockStates() { return Collections.unmodifiableMap(blockStates); }
	public Map<String, BlockModel> getBlockModels() { return Collections.unmodifiableMap(blockModels); }
	public List<BlockVariant> getVariants(String blockID) { return blockStates.get(blockID); }
	public BlockModel getModel(String model) { return blockModels.get(model); }
	
	
	public void deserializeBlockstates()
	{
		List<BlockVariant> blockVariants = new ArrayList<>();
		
		//TODO: need to use override pack blockstate files first
		try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zips.getBaseFileName()), null);
			DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath("/assets/minecraft/blockstates"));)
		{
			for (Path entry : entries)
			{
				JsonObject json = new JsonParser().parse(Files.newBufferedReader(entry, StandardCharsets.UTF_8)).getAsJsonObject();
				JsonObject variants = json.getAsJsonObject("variants");
				
				Set<Entry<String, JsonElement>> entrySet = variants.entrySet();
				for(Map.Entry<String,JsonElement> e : entrySet)
				{
					String key = e.getKey();
					blockVariants.add(BlockVariant.deserializeVariant(key, variants.get(key)));
				}
			
				String name = "minecraft:" + StringUtils.removeEnd(entry.getFileName().toString(), ".json");
				blockStates.put(name, blockVariants);
			}				
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void loadModels() throws Exception
	{
		for (Map.Entry<String, List<BlockVariant>> blockState : blockStates.entrySet())
		{
			for(BlockVariant variant : blockState.getValue())
			{
				for(BlockVariant.VariantModel model : variant.getModels())
				{
					String modelName = model.getModel();
					if(!blockModels.containsKey(modelName))
					{
						Map<String, String> textureMap = new HashMap<>();
						JsonArray elements = null;
						blockModels.put(modelName, loadModel("block/" + modelName, zips, textureMap, elements));
					}
				}
			}
		}		
	}
	
	// Recurse through model files and get block model information  TODO: This will need to change some with MC 1.9
	public BlockModel loadModel(String modelPath, ZipStack zips, Map<String, String> textureMap, JsonArray elements) throws Exception
	{
		JsonObject json = new JsonParser().parse(new InputStreamReader(zips.getStream("assets/minecraft/models/" + modelPath + ".json"))).getAsJsonObject();
		//JsonArray elements = null;
		
		String parent = "";
		if(json.has("parent")) // Get texture information and then load parent file
		{
			parent = json.get("parent").getAsString();

			if(json.has("elements") && elements == null)
			{
				elements = json.getAsJsonArray("elements");
			}
			
			if(json.has("textures"))
			{
				return loadModel(parent, zips, populateTextureMap(textureMap, json.getAsJsonObject("textures")), elements);
			}
			else
			{
				return loadModel(parent, zips, textureMap, elements);
			}
		}
		else  //Load all elements
		{
			Map<String, String> combineMap = new HashMap<>(textureMap);
			if(json.has("textures"))
			{
				combineMap.putAll(populateTextureMap(textureMap, json.getAsJsonObject("textures")));
			}

			boolean ao = true;
			if (json.has("ambientocclusion"))
				ao = false;
			
			if(json.has("elements") && elements == null)
			{
				elements = json.getAsJsonArray("elements");
			}			
			
			return new BlockModel(modelPath, ao, deserializeBlockElements(combineMap, elements));
		}
	}

	private List<BlockElement> deserializeBlockElements(Map<String, String> combineMap,	JsonArray elements) throws JsonSyntaxException 
	{
		List<BlockElement> elementsList = new ArrayList<>();
		List<BlockElement> testList;
		Gson gson = new Gson();
		System.out.println(elements);
		testList = gson.fromJson(elements.getAsJsonArray(), new TypeToken<List<Element>>(){}.getType());
		
		for(JsonElement e : elements)
		//for (int i = 0; i < elements.size(); i++)
		{
			Element testElement = gson.fromJson(e, Element.class);
			//testElement.getFaces().getUp().getTexture()
			
			JsonObject element = e.getAsJsonObject();
			
			JsonArray from = element.getAsJsonArray("from");
			Vector3f fromVector = new Vector3f(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat());
			JsonArray to = element.getAsJsonArray("to");
			Vector3f toVector = new Vector3f(to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat());
			
			org.joml.Vector3f rotationOrigin = new org.joml.Vector3f(8.0f, 8.0f, 8.0f);
			String rotationAxis = "y";
			org.joml.Vector3f rotAxis = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
			float rotationAngle = 0;
			boolean rotationScale = false;
			
			if(element.has("rotation"))
			{
				JsonObject rot = element.getAsJsonObject("rotation");
				JsonArray rotOrigin = rot.getAsJsonArray("origin");
				rotationOrigin = new org.joml.Vector3f(rotOrigin.get(0).getAsFloat(), rotOrigin.get(1).getAsFloat(), rotOrigin.get(2).getAsFloat());

				rotationAxis = rot.get("axis").getAsString();
				if (rotationAxis.equals("x"))
					rotAxis = new org.joml.Vector3f(1.0f, 0.0f, 0.0f);
				else
					rotAxis = new org.joml.Vector3f(0.0f, 0.0f, 1.0f);
				
				rotationAngle = rot.get("angle").getAsFloat();
				
				if(element.has("rescale"))
					rotationScale = true;
			}
			
			boolean shaded = true;
			if(element.has("shade"))
				shaded = false;						
			
			JsonObject faces = element.getAsJsonObject("faces");
			SubTexture subTexture = new SubTexture(null, fromVector.x(), 16-toVector.y(), toVector.x(), 16-fromVector.y());
			BlockElement be = new BlockElement(fromVector, toVector, rotationOrigin, rotAxis, rotationAngle, rotationScale, shaded, deserializeElementFaces(combineMap, subTexture, faces, fromVector, toVector));
			elementsList.add(be);
		}
		return elementsList;
	}

	private Map<String, ElementFace> deserializeElementFaces(Map<String, String> combineMap, SubTexture texCoords, JsonObject faces, Vector3f fromVector, Vector3f toVector) throws JsonSyntaxException
	{
		Map<String, ElementFace> elementFaces = new HashMap<>();
		
		Set<Entry<String, JsonElement>> entrySet = faces.entrySet();
		for(Map.Entry<String,JsonElement> e : entrySet)
		{
			String key = e.getKey();
			JsonObject face = faces.getAsJsonObject(key);
			
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
		    if(face.has("rotation"))
		    	rotation = face.get("rotation").getAsInt();
			
		    //System.out.println("u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
		    // TODO: Need to test more texture packs
			SubTexture subTexture = new SubTexture(null, u0*(1.0f/16.0f), v0*(1.0f/16.0f), u1*(1.0f/16.0f), v1*(1.0f/16.0f));
			
			StringBuilder tex = new StringBuilder(face.get("texture").getAsString());
		    if(tex.charAt(0) == '#')
		    {
		    	String texture = tex.deleteCharAt(0).toString();
		    	
		    	SubTexture te = texturePack.findTexture(StringUtils.removeStart(combineMap.get(texture), "blocks/")+ ".png");
		    	
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
					JsonArray uv = face.getAsJsonArray("uv");
					u0 = (float)(uv.get(0).getAsFloat()/16.0f);
					v0 = (float)(uv.get(1).getAsFloat()/16.0f) / numTiles;
					u1 = (float)(uv.get(2).getAsFloat()/16.0f);
					v1 = (float)(uv.get(3).getAsFloat()/16.0f) / numTiles;
				}
		    	
		    	System.out.println(texWidth + " x " + texHeight);
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
	
	private Map<String, String> populateTextureMap(Map<String, String> textureMap, JsonObject textures) throws JsonSyntaxException
	{
		Map<String, String> newTexMap = new HashMap<>();
		
		Set<Entry<String, JsonElement>> entrySet = textures.entrySet();
		for(Map.Entry<String,JsonElement> e : entrySet)
		{
			String key = e.getKey();
			StringBuilder tex = new StringBuilder(textures.get(key).getAsString());
			
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
