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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tectonicus.Minecraft;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tectonicus.util.FileUtils;
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
				JsonObject json = new JsonParser().parse(Files.newBufferedReader(entry)).getAsJsonObject();
				JsonObject variants = json.get("variants").getAsJsonObject();
				
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
					String modelPath = model.getModel();
					if(!blockModels.containsKey(modelPath))
					{
						Map<String, String> textureMap = new HashMap<>();
						blockModels.put(modelPath, loadModel("block/" + modelPath, zips, textureMap));
					}
				}
			}
		}		
	}
	
	// Recurse through model files and get block model information  TODO: This will need to change some with MC 1.9
	public BlockModel loadModel(String modelPath, ZipStack zips, Map<String, String> textureMap) throws Exception
	{
		JSONObject obj = new JSONObject(FileUtils.loadJSON(zips.getStream("assets/minecraft/models/" + modelPath + ".json")));
		String parent = "";
		if(obj.has("parent")) // Get texture information and then load parent file
		{
			parent = obj.getString("parent");

			return loadModel(parent, zips, populateTextureMap(textureMap, obj.getJSONObject("textures")));
		}
		else  //Load all elements
		{
			Map<String, String> combineMap = new HashMap<>(textureMap);
			if(obj.has("textures"))
			{
				combineMap.putAll(populateTextureMap(textureMap, obj.getJSONObject("textures")));
			}

			boolean ao = true;
			if (obj.has("ambientocclusion"))
				ao = false;
			
			JSONArray elements = obj.getJSONArray("elements");				
			
			return new BlockModel(modelPath, ao, deserializeBlockElements(combineMap, elements));
		}
	}

	private List<BlockElement> deserializeBlockElements(Map<String, String> combineMap,	JSONArray elements) throws JSONException 
	{
		List<BlockElement> elementsList = new ArrayList<>();
		
		for (int i = 0; i < elements.length(); i++)
		{
			JSONObject element = elements.getJSONObject(i);
			
			JSONArray from = element.getJSONArray("from");
			Vector3f fromVector = new Vector3f((float)from.getDouble(0), (float)from.getDouble(1), (float)from.getDouble(2));
			JSONArray to = element.getJSONArray("to");
			Vector3f toVector = new Vector3f((float)to.getDouble(0), (float)to.getDouble(1), (float)to.getDouble(2));
			
			Vector3f rotationOrigin = new Vector3f(8.0f, 8.0f, 8.0f);
			String rotationAxis = "y";
			float rotationAngle = 0;
			boolean rotationScale = false;
			
			if(element.has("rotation"))
			{
				JSONObject rot = element.getJSONObject("rotation");
				JSONArray rotOrigin = rot.getJSONArray("origin");
				rotationOrigin = new Vector3f((float)rotOrigin.getDouble(0), (float)rotOrigin.getDouble(1), (float)rotOrigin.getDouble(2));

				rotationAxis = rot.getString("axis");
				
				rotationAngle = (float) rot.getDouble("angle");
				
				if(element.has("rescale"))
					rotationScale = true;
			}
			
			boolean shaded = true;
			if(element.has("shade"))
				shaded = false;						
			
			JSONObject faces = element.getJSONObject("faces");
			SubTexture subTexture = new SubTexture(null, fromVector.x(), fromVector.y(), toVector.x(), toVector.y());
			BlockElement be = new BlockElement(fromVector, toVector, rotationOrigin, rotationAxis, rotationAngle, rotationScale, shaded, deserializeElementFaces(combineMap, subTexture, faces));
			elementsList.add(be);
		}
		return elementsList;
	}

	private Map<String, ElementFace> deserializeElementFaces(Map<String, String> combineMap, SubTexture texCoords, JSONObject faces) throws JSONException
	{
		Map<String, ElementFace> elementFaces = new HashMap<>();
		
		Iterator<?> keys = faces.keys();
		while(keys.hasNext())
		{
			String key = (String)keys.next();
			JSONObject face = (JSONObject) faces.get(key);
			
			float u0 = texCoords.u0;
			float v0 = texCoords.v0;
			float u1 = texCoords.u1;
			float v1 = texCoords.v1;
			
			int rotation = 0;
		    if(face.has("rotation"))
		    	rotation = face.getInt("rotation");
			
		    //System.out.println("u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
		    // TODO: Need to test more texture packs
			SubTexture subTexture = new SubTexture(null, u0*(1.0f/16.0f), v0*(1.0f/16.0f), u1*(1.0f/16.0f), v1*(1.0f/16.0f));
			
			StringBuilder tex = new StringBuilder(face.getString("texture"));
		    if(tex.charAt(0) == '#')
		    {
		    	String texture = tex.deleteCharAt(0).toString();
		    	
		    	SubTexture te = texturePack.findTexture(StringUtils.removeStart(combineMap.get(texture), "blocks/")+ ".png");
		    	
		    	final float texHeight = te.texture.getHeight();
				final float texWidth = te.texture.getWidth();
		    	final int numTiles = te.texture.getHeight()/te.texture.getWidth();
		    	
		    	u0 /= texWidth;
				v0 /= texWidth;
				u1 /= texWidth;
				v1 /= texWidth;
		    	
		    	if(face.has("uv"))
				{
		    		//System.out.println("Before: u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
					JSONArray uv = face.getJSONArray("uv");
					u0 = (float)(uv.getDouble(0)/16.0f);
					v0 = (float)(uv.getDouble(1)/16.0f) / numTiles;
					u1 = (float)(uv.getDouble(2)/16.0f);
					v1 = (float)(uv.getDouble(3)/16.0f) / numTiles;
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
	
	private Map<String, String> populateTextureMap(Map<String, String> textureMap, JSONObject textures) throws JSONException
	{
		Map<String, String> newTexMap = new HashMap<>();
		
		Iterator<?> keys = textures.keys();
		while(keys.hasNext())
		{
			String key = (String)keys.next();
		    StringBuilder tex = new StringBuilder((String) textures.get(key));
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
