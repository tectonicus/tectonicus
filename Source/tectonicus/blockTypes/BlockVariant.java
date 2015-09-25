/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public String getName()	{ return name; }	
	public Map<String, String> getStates() { return states;	}
	public List<VariantModel> getModels() {	return models; }
	
	
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
		
	}
}
