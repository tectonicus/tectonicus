/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tectonicus.blockTypes.Air;

public class BlockTypeRegistry
{
	private Map<Integer, BlockType> blocks;
	private Map<IdDataPair, BlockType> boundBlocks;
	
	private BlockType defaultBlock;
	
	public BlockTypeRegistry()
	{
		blocks = new HashMap<Integer, BlockType>();
		boundBlocks = new HashMap<IdDataPair, BlockType>();
		
		defaultBlock = new Air();
	}
	
	public void setDefaultBlock(BlockType defaultType)
	{
		this.defaultBlock = defaultType;
	}
	
	public void register(final int blockId, BlockType type)
	{
		if (blocks.containsKey(blockId))
			blocks.remove(blockId);
		
		blocks.put(blockId, type);
	}
	
	public void register(final int blockId, final int data, BlockType type)
	{
		IdDataPair key = new IdDataPair(blockId, data);
		if (boundBlocks.containsKey(key))
			boundBlocks.remove(key);
			
		boundBlocks.put(key, type);
	}
	
	public BlockType find(final int id, final int data)
	{
		BlockType result = null;
		
		// Check bound blocks first
		result = boundBlocks.get(new IdDataPair(id, data));
		if (result != null)
			return result;
		
		// No bound block, so just try by id
		result = blocks.get(id);
		if (result != null)
			return result;
		
		// Not found at all, return default block
		return defaultBlock;
	}
	
	public Set<BlockType> getTypes()
	{
		Set<BlockType> allTypes = new HashSet<BlockType>();
		
		allTypes.addAll( blocks.values() );
		allTypes.addAll (boundBlocks.values() );
		
		return allTypes;
	}
}
