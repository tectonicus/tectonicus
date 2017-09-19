/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableLong;

public class WorldStats
{
	private int numChunks;
	private int numPortals;
	private int numPlayers;
	
	private Map<IdDataPair, MutableLong> blockIdCounts;
	
	public WorldStats()
	{
		blockIdCounts = new HashMap<IdDataPair, MutableLong>();
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
	
	public void setNumPortals(int numPortals)
	{
		this.numPortals = numPortals;
	}
	
	public void incBlockId(final int blockId, final int data)
	{
		IdDataPair key = new IdDataPair(blockId, data);

		MutableLong count = blockIdCounts.get(key);
		if (count != null)
			count.increment();
		else
			blockIdCounts.put(key, new MutableLong(1L));
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
				count += blockIdCounts.get(id).toLong();
				
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
				args.put("count", "\""+countStr+"\"");
			
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

	private static class IdDataPair implements Comparable<IdDataPair>
	{
		public final int id;
		public final int data;
		
		public IdDataPair(final int id, final int data)
		{
			this.id = id;
			this.data = data;
		}
		
		@Override
		public int compareTo(IdDataPair o)
		{
			final int idDiff = id - o.id;
			if (idDiff != 0)
				return idDiff;
			
			return data - o.data;
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
