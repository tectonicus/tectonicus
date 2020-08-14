/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class BlockVariant
{
	private final String name;
	private final Map<String, String> states;
	private final List<BlockStateModel> models;
	
	public BlockVariant(String name, List<BlockStateModel> models)
	{
		this.name = name;
		this.states = new HashMap<>();
		this.models = models;

		String[] statesArray = name.split(",");
		
		for(String s : statesArray)
		{
			String[] state = s.split("=");
			if(state.length > 1)
				states.put(state[0], state[1]);
			else
				states.put(state[0], "");
		}
	}
	
	
	public void drawVariant(int x, int y, int z, BlockRegistry registry)
	{
		//TODO: get randomized index based on weight for variant model choice
		BlockStateModel vm = models.get(0);
		BlockModel bm = registry.getModel(vm.getModel());
		
		bm.drawModel(x, y, z, vm.getXRotation(), vm.getYRotation(), vm.isUvlock());
	}
}
