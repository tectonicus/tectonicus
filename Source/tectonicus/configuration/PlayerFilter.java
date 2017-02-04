/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import tectonicus.Minecraft;
import tectonicus.raw.Player;

public class PlayerFilter
{
	private final PlayerFilterType filter;
	private Path filterFile;
	private List<String> playerList;
	
	public PlayerFilter()
	{
		this.filter = PlayerFilterType.All;
	}
	
	public PlayerFilter(final PlayerFilterType type, Path playersFile, Path worldDir) throws Exception
	{
		this.filter = type;
		this.filterFile = playersFile;
		
		if ((filter == PlayerFilterType.Whitelist || filter == PlayerFilterType.Blacklist || filter == PlayerFilterType.Ops) && (Files.exists(playersFile) && !Files.isDirectory(playersFile)))
		{
			loadPlayerList(playersFile);
		}
		else
		{
			if (filter == PlayerFilterType.Ops)
			{
				Path opsFile = Minecraft.findServerPlayerFile(worldDir, "ops");
				loadPlayerList(opsFile);
			}
			else if (filter == PlayerFilterType.Whitelist)
			{
				Path whitelist = Minecraft.findServerPlayerFile(worldDir, "whitelist");
				loadPlayerList(whitelist);
			}
			else if (filter == PlayerFilterType.Blacklist)
			{
				Path blacklist = Minecraft.findServerPlayerFile(worldDir, "banned-players");
				loadPlayerList(blacklist);
			}
		}
	}
	
	private void loadPlayerList(Path playerFile)
	{
		System.out.println("Loading players from " + playerFile);
		
		playerList = new ArrayList<String>();
		
		try
		{
			if (playerFile.toString().toLowerCase().endsWith(".json"))
			{	
				JsonArray array = new JsonParser().parse(new String(Files.readAllBytes(playerFile))).getAsJsonArray();
				
				for (int i=0; i<array.size(); i++)
				{
					String name = array.get(i).getAsJsonObject().get("name").getAsString();
					playerList.add(name.toLowerCase());
				}
			}
			else if (playerFile.toString().toLowerCase().endsWith(".txt"))
			{
				if (Files.exists(playerFile))
				{
					List<String> lines = Files.readAllLines(playerFile);
					for (String line : lines)
						playerList.add( line.trim().toLowerCase() );
				}
			}
			
			System.out.println("\tfound " + playerList.size() + " players");
		}
		catch (Exception e)
		{
			System.out.println("Error while loading players from " + playerFile);
			e.printStackTrace();
		}
	}
	
	public boolean passesFilter(Player player)
	{
		if (filter == PlayerFilterType.None)
		{
			return false;
		}
		else if (filter == PlayerFilterType.All)
		{
			return true;
		}
		else if (filter == PlayerFilterType.Ops || filter == PlayerFilterType.Whitelist)
		{
			return playerList.contains(player.getName());
		}
		else if (filter == PlayerFilterType.Blacklist)
		{
			return !playerList.contains(player.getName());
		}
		else
		{
			throw new RuntimeException("Unknown player filter:"+filter);
		}
	}
	
	@Override
	public String toString()
	{
		// We override this so that MutableConfiguration.printActive works
		StringBuilder result = new StringBuilder(filter.toString());
		
		if (filterFile != null)
		{
			result.append(": ");
			result.append(filterFile);
		}
		
		return result.toString();
	}
}
