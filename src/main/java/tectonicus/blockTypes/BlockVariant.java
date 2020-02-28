/*
 * Copyright (c) 2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

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
	
	
	public void drawVariant(int x, int y, int z, BlockRegistry registry)
	{
		//TODO: get randomized index based on weight for variant model choice
		VariantModel vm = models.get(0);
		BlockModel bm = registry.getModel(vm.model);
		
		bm.drawModel(x, y, z, vm.getXRot(), vm.getYRot(), vm.isUVlocked());
	}
	
	public String getName() { return name; }
	public Map<String, String> getStates() { return Collections.unmodifiableMap(states); }
	public List<VariantModel> getModels() { return Collections.unmodifiableList(models); }
	
	public static BlockVariant deserializeVariant(String key, JsonElement variant)
	{
		List<VariantModel> models = new ArrayList<>();

		try {
			if (variant.isJsonObject()) //If only a single model
				variant = JsonParser.parseString("[" + variant.toString() + "]");
			
			models = new Gson().fromJson(variant.getAsJsonArray(), new TypeToken<List<VariantModel>>(){}.getType());
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
		
		return new BlockVariant(key, models);
	}
	
	
	public static class VariantModel 
	{
		private String model = "";
		private int x = 0; 
		private int y = 0;
		private int weight = 1;
		private boolean uvlock = false;
		
		public String getModel() { return model; }	
		public int getXRot() { return x; }	
		public int getYRot() { return y; }	
		public boolean isUVlocked() { return uvlock; }	
		public int getWeight() { return weight;	}
	}
}
