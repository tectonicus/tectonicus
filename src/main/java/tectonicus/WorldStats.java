/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableLong;
import tectonicus.util.JsArrayWriter;
import tectonicus.util.JsonWriter;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WorldStats
{
	private int numChunks;
	private int numPortals;
	private int numPlayers;
	
	private final Map<IdDataPair, MutableLong> blockIdCounts;
	
	public WorldStats()
	{
		blockIdCounts = new HashMap<>();
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
	
	public int numChunks()
	{
		return numChunks;
	}

	public void outputBlockStats(File statsFile, String varNamePrefix, BlockTypeRegistry registry)
	{
		if (statsFile.exists())
			statsFile.delete();
		
		log.debug("Writing block stats to {}", statsFile.getAbsolutePath());
		
		// First merge with block id names (so that 'flowing lava' and 'stationary lava' becomes 'lava'
		Map<String, Long> nameCounts = new HashMap<>();
		Map<IdDataPair, Boolean> unknownBlockIds = new HashMap<>();
		for (Map.Entry<IdDataPair, MutableLong> entry : blockIdCounts.entrySet())
		{
			IdDataPair id = entry.getKey();

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
				count += entry.getValue().toLong();
				
				// Update the count
				nameCounts.put(name, count);
			}
			else
			{
				if (!unknownBlockIds.containsKey(id))
					unknownBlockIds.put(id, true);
			}
		}

		try (JsArrayWriter jsWriter = new JsArrayWriter(statsFile, varNamePrefix + "_blockStats")) {

			// Get the names and sort them so they're output in alphabetical order
			List<String> names = new ArrayList<>(nameCounts.keySet());
			Collections.sort(names);

			for (String key : names) {
				long count = nameCounts.get(key);

				Map<String, String> args = new HashMap<>();

				args.put("name", "\"" + key + "\"");

				if (key.equals("Bed"))
					count /= 2;

				String countStr = NumberFormat.getInstance().format(count);
				args.put("count", "\"" + countStr + "\"");

				jsWriter.write(args);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.info("Wrote {} block counts", nameCounts.size());

		if (!unknownBlockIds.isEmpty())
		{
			log.warn("Unknown block types:");
			for (IdDataPair id : unknownBlockIds.keySet())
				log.warn("\t" + id.id + ":" + id.data);
		}
	}
	
	public void outputWorldStats(File statsFile, String varNamePrefix) {
		if (statsFile.exists())
			statsFile.delete();
		
		log.debug("Writing world stats to {}", statsFile.getAbsolutePath());

		try (JsonWriter jsWriter = new JsonWriter(statsFile)) {
			jsWriter.startObject(varNamePrefix + "_worldStats");

			jsWriter.writeVariable("numChunks", "" + numChunks);
			jsWriter.writeVariable("numPortals", "" + numPortals);
			jsWriter.writeVariable("numPlayers", "" + numPlayers);

			jsWriter.endObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.info("Wrote world stats");
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
