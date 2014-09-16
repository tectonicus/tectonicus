/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.io.File;

import tectonicus.Minecraft;
import tectonicus.raw.PlayerList;
import tectonicus.raw.Player;

public class PlayerFilter
{
	private final PlayerFilterType filter;
	
	private File filterFile;
	
	private PlayerList playerList;
	
	public PlayerFilter()
	{
		this.filter = PlayerFilterType.All;
	}
	
	public PlayerFilter(final PlayerFilterType type)
	{
		this.filter = type;
	}
	
	public PlayerFilter(final PlayerFilterType type, File playersFile, File worldDir) throws Exception
	{
		this.filter = type;
		this.filterFile = playersFile;
		
		if ((filter == PlayerFilterType.Whitelist || filter == PlayerFilterType.Blacklist || filter == PlayerFilterType.Ops) && (playersFile.exists() && !playersFile.isDirectory()))
		{
			try
			{
				playerList = new PlayerList(playersFile);
			}
			catch (Exception e)
			{
				System.err.println("Couldn't load player filter file: "+e);
				throw e;
			}
		}
		else
		{
			if (filter == PlayerFilterType.Ops)
			{
				File opsFile = Minecraft.findServerPlayerFile(worldDir, "ops");
				loadPlayerList(opsFile);
			}
			else if (filter == PlayerFilterType.Whitelist)
			{
				File whitelist = Minecraft.findServerPlayerFile(worldDir, "whitelist");
				loadPlayerList(whitelist);
			}
			else if (filter == PlayerFilterType.Blacklist)
			{
				File blacklist = Minecraft.findServerPlayerFile(worldDir, "banned-players");
				loadPlayerList(blacklist);
			}
		}
	}
	
	private void loadPlayerList(File playerFile)
	{
		System.out.println("Loading players from "+playerFile.getAbsolutePath());
		try
		{
			playerList = new PlayerList(playerFile);
		}
		catch (Exception e)
		{
			System.out.println("Error while loading players from "+playerFile.getAbsolutePath());
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
		else if (filter == PlayerFilterType.Ops)
		{
			return playerList.contains(player.getName());
		}
		else if (filter == PlayerFilterType.Whitelist)
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
		// We need to override this so that MutableConfiguration.printActive works
		
		String result = "";
		
		result += filter;
		
		if (filterFile != null)
		{
			result += " + ";
			result += filterFile.getAbsolutePath();
		}
		
		return result;
	}
}
