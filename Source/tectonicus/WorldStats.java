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

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WorldStats
{
	private int numChunks;
	private int numPortals;
	private int numPlayers;
	
	private Map<IdDataPair, Long> blockIdCounts;
	
	public WorldStats()
	{
		blockIdCounts = new HashMap<IdDataPair, Long>();
	}
	
	public void setNumPlayers(final int numPlayers)
	{
		this.numPlayers = numPlayers;
	}
	
	public void incNumChunks()
	{
		numChunks++;
	}
	
	public void incNumPortals()
	{
		numPortals++;
	}
	
	public void incBlockId(final int blockId, final int data)
	{
		IdDataPair key = new IdDataPair(blockId, data);
		
		if (!blockIdCounts.containsKey(key))
			blockIdCounts.put(key, 0L);
		
		Long count = blockIdCounts.get(key);
		count = count + 1;
		blockIdCounts.put(key, count);
	}
	
//	public Map<Integer, Long> getStats()
//	{
//		return new HashMap<Integer, Long>( blockIdCounts );
//	}
	
	public int numChunks()
	{
		return numChunks;
	}

	public void outputBlockStats(File statsFile, String varNamePrefix, BlockTypeRegistry registry)
	{
		if (statsFile.exists())
			statsFile.delete();
		
		System.out.println("Outputting block stats to "+statsFile.getAbsolutePath());
		
		// First merge with block id names (so that 'flowing lava' and 'stationary lava' becomes 'lava'
		Map<String, Long> nameCounts = new HashMap<String, Long>();
		Map<IdDataPair, Boolean> unknownBlockIds = new HashMap<IdDataPair, Boolean>();
		for (IdDataPair id : blockIdCounts.keySet())
		{
			// Find the name
			BlockType type = registry.find(id.id, id.data);
			if (type != null)
			{
				String name = type.getName();
				
				// Ensure it exists
				if (!nameCounts.containsKey(name))
					nameCounts.put(name, 0L);
				
				// Get the existing count
				long count = nameCounts.get(name);
				count += blockIdCounts.get(id);
				
				// Update the count
				nameCounts.put(name, count);
			}
			else
			{
				if (!unknownBlockIds.containsKey(id))
					unknownBlockIds.put(id, true);
			}
		}

		JsArrayWriter jsWriter = null;
		try
		{
			jsWriter = new JsArrayWriter(statsFile, varNamePrefix+"_blockStats");
			
			// Get the names and sort them so they're output in alphabetical order
			ArrayList<String> names = new ArrayList<String>( nameCounts.keySet() );
			Collections.sort(names);
			
			for (String key : names)
			{
				final long count = nameCounts.get(key);
				
				HashMap<String, String> args = new HashMap<String, String>();
				
				args.put("name", "\""+key+"\"");
				
				String countStr = NumberFormat.getInstance().format(count);
				args.put("count", "'"+countStr+"'");
			
				jsWriter.write(args);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jsWriter != null)
				jsWriter.close();
		}
		
		System.out.println("Outputted "+nameCounts.size()+" block counts");

		if (!unknownBlockIds.isEmpty())
		{
			System.out.println("Unknown block types:");
			for (IdDataPair id : unknownBlockIds.keySet())
				System.out.println("\t" + id.id + ":" + id.data);
		}
	}
	
	public void outputWorldStats(File statsFile, String varNamePrefix)
	{
		if (statsFile.exists())
			statsFile.delete();
		
		System.out.println("Outputting world stats to "+statsFile.getAbsolutePath());
		
		JsonWriter jsWriter = null;
		try
		{
			jsWriter = new JsonWriter(statsFile);
			jsWriter.startObject(varNamePrefix+"_worldStats");
			
			jsWriter.writeVariable("numChunks", ""+numChunks);
			jsWriter.writeVariable("numPortals", ""+numPortals);
			jsWriter.writeVariable("numPlayers", ""+numPlayers);
			
			jsWriter.endObject();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jsWriter != null)
				jsWriter.close();
		}
		
		System.out.println("Outputted world stats");
	}

	private static class IdDataPair
	{
		public final int id;
		public final int data;
		
		public IdDataPair(final int id, final int data)
		{
			this.id = id;
			this.data = data;
		}
		
		@Override
		public int hashCode()
		{
			return id ^ data;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof IdDataPair))
				return false;
			
			IdDataPair other = (IdDataPair)obj;
			
			return this.id == other.id && this.data == other.data;
		}
	}
}
