/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class BlockVariant
{
	final private String name;
	final private Map<String, String> states;
	final private List<VariantModel> models;
	
	public BlockVariant(String name, List<VariantModel> models)
	{
		this.name = name;
		this.states = new HashMap<>();
		this.models = models;

		String statesArray[] = name.split(",");
		
		for(String s : statesArray)
		{
			String state[] = s.split("=");
			if(state.length > 1)
				states.put(state[0], state[1]);
			else
				states.put(state[0], "");
		}
	}
	
	public String getName() { return name; }
	public Map<String, String> getStates() { return Collections.unmodifiableMap(states); }
	public List<VariantModel> getModels() { return Collections.unmodifiableList(models); }
	
	
	public static class VariantModel 
	{
		final private String modelPath;
		final private int x, y, weight;
		final private boolean uvlocked;
		
		public VariantModel(String modelPath, int x, int y, boolean uvlocked, int weight)
		{
			this.modelPath = modelPath;
			this.x = x;
			this.y = y;
			this.uvlocked = uvlocked;
			this.weight = weight;
		}
		
		public String getModelPath() { return modelPath; }	
		public int getXRot() { return x; }	
		public int getYRot() { return y; }	
		public boolean isUVlocked() { return uvlocked; }	
		public int getWeight() { return weight;	}
		
		public static VariantModel deserializeVariantModel(JSONObject model) throws JSONException
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
	}
}
