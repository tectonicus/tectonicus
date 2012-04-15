/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
