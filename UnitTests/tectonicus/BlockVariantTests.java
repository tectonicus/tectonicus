/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import tectonicus.blockTypes.BlockModel;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.blockTypes.BlockVariant;
import tectonicus.blockTypes.BlockVariant.VariantModel;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tectonicus.texture.ZipStack.ZipStackEntry;
import tectonicus.util.Vector3f;

public class BlockVariantTests
{
	private TexturePack texturePack;
	
	@Test
	public void createSingleStateMap()
	{
		BlockVariant bv = new BlockVariant("normal", null);
		Map<String, String> states = bv.getStates();
		
		Map<Object, Object> testStates = new HashMap<>();
		testStates.put("normal", "");
		
		assertTrue(states.equals(testStates));
	}
	
	@Test
	public void createMultipleStatesMap()
	{
		BlockVariant bv = new BlockVariant("attached=false,facing=north,powered=false,suspended=false", null);
		Map<String, String> states = bv.getStates();
		
		Map<Object, Object> testStates = new HashMap<>();
		testStates.put("attached", "false");
		testStates.put("facing", "north");
		testStates.put("powered", "false");
		testStates.put("suspended", "false");
		
		assertTrue(states.equals(testStates));
	}
	
	@Test
	public void createModelVariants() throws Exception
	{
		Map<String, BlockVariant> vMap = new HashMap<>();
		
		BufferedReader reader = null;
		try
		{
			ZipStack zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
			ZipStackEntry modelFile = zips.getEntry("assets/minecraft/blockstates/grass.json");

			reader = new BufferedReader(new InputStreamReader(modelFile.getInputStream()));
            StringBuilder builder = new StringBuilder();
			
            String line = null;
            while ((line = reader.readLine()) != null)
            {
            	builder.append(line + "\n");
            }
            reader.close();

			JSONObject obj = new JSONObject(builder.toString());
			JSONObject variants = obj.getJSONObject("variants");
			
			Iterator<?> keys = variants.keys();
			while(keys.hasNext()) 
			{
			    String key = (String)keys.next();
			    Object variant = variants.get(key);
			    List<VariantModel> models = new ArrayList<>();
			    
				try {
					if (variant instanceof JSONObject) {  // If only a single model
						JSONObject model = (JSONObject) variant;
						models.add(getModel(model));
					} else { // if more than one model
						JSONArray array = (JSONArray) variant;
						for (int i = 0; i < array.length(); i++) {
							JSONObject model = array.getJSONObject(i);
							models.add(getModel(model));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			    
			    BlockVariant bv = new BlockVariant(key, models);
			    vMap.put(key, bv);
			}
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
		
		BlockVariant bv = vMap.get("snowy=false");
		assertTrue(bv.getModels() != null);
	}
	
	// Get a single model
	private VariantModel getModel(JSONObject model) throws JSONException
	{
		String modelPath = "";
		int x = 0;
		int y = 0;
		int weight = 1;
		boolean uvlock = false;
		
		Iterator<?> keys = model.keys();
		while (keys.hasNext()) 
		{
			String key = (String) keys.next();

			if (key.equals("model")) {
				modelPath = model.getString(key);
				//loadModel(modelPath);
			} else if (key.equals("x"))	{
				x = model.getInt(key);
			} else if (key.equals("y")) {
				y = model.getInt(key);
			} else if (key.equals("uvlock")) {
				uvlock = model.getBoolean(key);
			} else if (key.equals("weight")) {
				weight = model.getInt(key);
			}
		}
		
		return new VariantModel(modelPath, x, y, uvlock, weight);
	}
	
	
	@Test
	public void modelTest() throws Exception
	{
		//texturePack = new TexturePack(null, Minecraft.findMinecraftJar(), null, null);
		Map<String, String> textureMap = new HashMap<>();
		BlockModel bm = loadModel("block/fire_u1", textureMap);
		assertThat(bm.getElements().size(), equalTo(2));
	}
	
	// Recurse through model files and get block model information  TODO: This will need to change some with MC 1.9
	public BlockModel loadModel(String modelPath, Map<String, String> textureMap) throws Exception
	{
		BufferedReader reader = null;
		try
		{
			ZipStack zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
			ZipStackEntry modelFile = zips.getEntry("assets/minecraft/models/" + modelPath + ".json");

			reader = new BufferedReader(new InputStreamReader(modelFile.getInputStream()));
            StringBuilder builder = new StringBuilder();
			
            String line = null;
            while ((line = reader.readLine()) != null)
            {
            	builder.append(line + "\n");
            }
            reader.close();

			JSONObject obj = new JSONObject(builder.toString());
			String parent = "";
			if(obj.has("parent")) // Get texture information and then load parent file
			{
				parent = obj.getString("parent");

				Map<String, String> newTexMap = new HashMap<>();
				JSONObject textures = obj.getJSONObject("textures");
				
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
				return loadModel(parent, newTexMap);
			}
			else  //Load all elements
			{
				List<BlockElement> elementsList = new ArrayList<>();
				
				boolean ao = true;
				if (obj.has("ambientocclusion"))
					ao = false;
				
				JSONArray elements = obj.getJSONArray("elements");				
				for (int i = 0; i < elements.length(); i++)
				{
					Map<String, ElementFace> elementFaces = new HashMap<>();
					
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
					
					Iterator<?> keys = faces.keys();
					while(keys.hasNext())
					{
						String key = (String)keys.next();
						JSONObject face = (JSONObject) faces.get(key);
						
						float u0 = fromVector.x();
						float v0 = fromVector.y();
						float u1 = toVector.x();
						float v1 = toVector.y();
						
						if(face.has("uv"))
						{
							JSONArray uv = face.getJSONArray("uv");
							u0 = (float)uv.getDouble(0);
							v0 = (float)uv.getDouble(1);
							u1 = (float)uv.getDouble(2);
							v1 = (float)uv.getDouble(3);
						}
						
						StringBuilder tex = new StringBuilder(face.getString("texture"));
					    if(tex.charAt(0) == '#')
					    {
					    	String texture = tex.deleteCharAt(0).toString();
					    	//System.out.println(textureMap.get(texture));
					    	// TODO: Need to convert UV coords to numbers between 0 and 1, account for animated textures, etc.
					    	//SubTexture subTexture = texturePack.findTexture(texture);
					    }
					    
					    SubTexture texture = new SubTexture(null, u0, v0, u1, v1);
					    
					    boolean cullFace = false;
					    if(face.has("cullface"))
					    	cullFace = true;
					    
					    int rotation = 0;
					    if(face.has("rotation"))
					    	rotation = face.getInt("rotation");
					    
					    boolean tintIndex = false;
					    if(face.has("tintindex"))
					    	tintIndex = true;
					    
					    ElementFace ef = new ElementFace(texture, cullFace, rotation, tintIndex);
					    elementFaces.put(key, ef);
					}
					//System.out.println(elementFaces);
					BlockElement be = new BlockElement(fromVector, toVector, rotationOrigin, rotationAxis, rotationAngle, rotationScale, shaded, elementFaces);
					elementsList.add(be);
				}

				return new BlockModel(modelPath, ao, elementsList);
			}
		}
		finally
		{			
			if (reader != null)
				reader.close();
		}
	}
	
	//TODO:  Move and rewrite this test once we start parsing the block variants
	@Test
	public void createBlockVariantMap()
	{
		Map<String, BlockVariant> variants = new HashMap<>();
		
		BlockVariant bv1 = new BlockVariant("attached=true,facing=south,powered=false,suspended=false", null);
		BlockVariant bv2 = new BlockVariant("attached=true,facing=south,powered=true,suspended=false", null);
		BlockVariant bv3 = new BlockVariant("attached=true,facing=west,powered=true,suspended=true", null);
		
		variants.put(bv1.getName(), bv1);
		variants.put(bv2.getName(), bv2);
		variants.put(bv3.getName(), bv3);
		
		assertTrue(variants.containsKey("attached=true,facing=west,powered=true,suspended=true"));
	}
	
	@Test
	public void testBlockModel()
	{
		BlockModel bm = new BlockModel("", false, null);
		BlockElement test = new BlockElement(null, null, null, "", 0, false, false, new HashMap<String, ElementFace>());
		test.getFrom();
	}
}
