/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import tectonicus.Minecraft;
import tectonicus.raw.Player;
import tectonicus.util.FileUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class PlayerFilter
{
	private final PlayerFilterType filter;
	private Path filterFile;
	private List<String> playerList;
	
	public PlayerFilter()
	{
		this.filter = PlayerFilterType.All;
	}
	
	public PlayerFilter(final PlayerFilterType type, Path playersFile, Path worldDir)
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
		log.info("Loading players from {}", playerFile);
		
		playerList = new ArrayList<>();
		
		try {
			if (playerFile.toString().toLowerCase().endsWith(".json"))
			{
				JsonNode node = FileUtils.getOBJECT_MAPPER().reader().readTree(new String(Files.readAllBytes(playerFile)));

				for (int i=0; i<node.size(); i++)
				{
					String name = node.get(i).get("name").asText();
					playerList.add(name.toLowerCase());
				}
			}
			else if (playerFile.toString().toLowerCase().endsWith(".txt") && Files.exists(playerFile))
			{
				List<String> lines = Files.readAllLines(playerFile, StandardCharsets.UTF_8);
				for (String line : lines)
					playerList.add( line.trim().toLowerCase() );
			}
			
			log.debug("\tfound {} players", playerList.size());
		} catch (Exception e) {
			log.error("Error while loading players from {}", playerFile, e);
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
