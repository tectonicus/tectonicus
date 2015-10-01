/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tectonicus.Minecraft;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.blockTypes.BlockVariant.VariantModel;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.rasteriser.lwjgl.LwjglTexture;
import tectonicus.rasteriser.lwjgl.LwjglTextureUtils;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tectonicus.texture.ZipStack.ZipStackEntry;
import tectonicus.util.Vector3f;

public class BlockRegistry
{
	private List<Map<String, BlockVariant>> blockStates = new ArrayList<>();  //List of all blockstates
	private Map<String, BlockVariant> vMap = new HashMap<>();  //Map of variants of a single blockstate
	private Map<String, BlockModel> blockModels = new HashMap<>();
	private ZipStack zips;
	
	public BlockRegistry()
	{
		try {
			zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deserialize()
	{
		Enumeration<? extends ZipEntry> entries = zips.getBaseEntries();
		while(entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			if(entry.getName().contains("blockstates"))
			{
				ZipStackEntry zse = zips.getEntry(entry.getName());
				try 
				{
					vMap = loadVariants(loadJSON(zse.getInputStream()));
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				blockStates.add(vMap);
				//System.out.println(entry.getName());
			}
		}
	}
	
	private JSONObject loadJSON(InputStream iStream) throws Exception
	{
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder builder = new StringBuilder();
			
            String line = null;
            while ((line = reader.readLine()) != null)
            {
            	builder.append(line + "\n");
            }
            reader.close();

			return new JSONObject(builder.toString());
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}
	
	private Map<String, BlockVariant> loadVariants(JSONObject obj) throws Exception
	{
		Map<String, BlockVariant> vMap = new HashMap<>();
		
		BufferedReader reader = null;
		try
		{
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
			return vMap;
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}
	
	private VariantModel getModel(JSONObject model) throws Exception
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
				//modelTotal++;
				if(!blockModels.containsKey(modelPath))
				{
					//System.out.println(modelPath + ".json");
					Map<String, String> textureMap = new HashMap<>();
					blockModels.put(modelPath, loadModel("block/" + modelPath, zips, textureMap));  //TODO: Should we load all the models here or do it later?
				}
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
	
	// Recurse through model files and get block model information  TODO: This will need to change some with MC 1.9
	public BlockModel loadModel(String modelPath, ZipStack zips, Map<String, String> textureMap) throws Exception
	{
		BufferedReader reader = null;
		try
		{
			ZipStackEntry modelFile = zips.getEntry("assets/minecraft/models/" + modelPath + ".json");

			JSONObject obj = loadJSON(modelFile.getInputStream());
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
						
						
						
						int rotation = 0;
					    if(face.has("rotation"))
					    	rotation = face.getInt("rotation");
						
					    //System.out.println("u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
					    // TODO: Need to account for texture size
						SubTexture subTexture = new SubTexture(null, u0*(1.0f/16.0f), v0*(1.0f/16.0f), u1*(1.0f/16.0f), v1*(1.0f/16.0f));
						//SubTexture subTexture = new SubTexture(null, u0, v0, u1, v1);
						
						StringBuilder tex = new StringBuilder(face.getString("texture"));
					    if(tex.charAt(0) == '#')
					    {
					    	String texture = tex.deleteCharAt(0).toString();
					    	//System.out.println(textureMap.get(texture));
					    	BufferedImage img = loadTexture("assets/minecraft/textures/" + combineMap.get(texture) + ".png", zips);
					    	if(img != null)
					    		System.out.println("img loaded");
					    	
					    	Texture test = createTexture(img, TextureFilter.NEAREST);
					    	final float texHeight = test.getHeight();
							final float texWidth = test.getWidth();
					    	final int numTiles = test.getHeight()/test.getWidth();
					    	
					    	u0 = fromVector.x()/texWidth;
							v0 = fromVector.y()/texWidth;
							u1 = toVector.x()/texWidth;
							v1 = toVector.y()/texWidth;
					    	
					    	if(face.has("uv"))
							{
					    		System.out.println("Before: u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
								JSONArray uv = face.getJSONArray("uv");
								u0 = (float)(uv.getDouble(0)/16.0f);
								v0 = (float)(uv.getDouble(1)/16.0f) / numTiles;
								u1 = (float)(uv.getDouble(2)/16.0f);
								v1 = (float)(uv.getDouble(3)/16.0f) / numTiles;
								System.out.println("Before: u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
							}
					    	
					    	System.out.println(test.getWidth() + " x " + test.getHeight());
					    	int frame = 1;
					    	if(numTiles > 1)
							{
								Random rand = new Random();
								frame = rand.nextInt(numTiles)+1;
							}
					    	// TODO: Only load each texture once (use TexturePack)
					    	subTexture = new SubTexture(test, u0, v0+(float)(frame-1)*(texWidth/texHeight), u1, v1+(float)(frame-1)*(texWidth/texHeight));
					    	//subTexture = new SubTexture(test, u0, v0, u1, v1);
					    	System.out.println("u0="+subTexture.u0+" v0="+subTexture.v0+" u1="+subTexture.u1+" v1="+subTexture.v1);
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
	
	private Map<String, String> populateTextureMap(Map<String, String> textureMap, JSONObject textures) throws JSONException
	{
		Map<String, String> newTexMap = new HashMap<>();
		//JSONObject textures = obj.getJSONObject("textures");
		
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
	
	private BufferedImage loadTexture(String path, ZipStack stack)
	{
		InputStream in = null;
		
		try
		{
			// Check texture pack and minecraft jar
			ZipStack.ZipStackEntry entry = stack.getEntry(path);
			if (entry != null)
			{
				in = entry.getInputStream();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
//		if (in == null)
//		{
//			try
//			{
//				// Check classpath
//				in = getClass().getClassLoader().getResourceAsStream(path);
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//			}
//		}
		
		BufferedImage img = null;
		
		if (in != null)
		{
			try
			{
		
				img = ImageIO.read(in);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					in.close();
				}
				catch (Exception e) {}
			}
		}
		
		return TexturePack.copy( img );
	}
	
	private Texture createTexture(BufferedImage image, TextureFilter filter)
	{
		final int id = LwjglTextureUtils.createTexture(image, filter);
		Texture texture = new LwjglTexture(id, image.getWidth(), image.getHeight());
		return texture;
	}
}
