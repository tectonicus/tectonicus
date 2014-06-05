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
	
	public PlayerFilter(final PlayerFilterType type, File playersFile) throws Exception
	{
		this.filter = type;
		this.filterFile = playersFile;
		
		if (filter == PlayerFilterType.Whitelist
			|| filter == PlayerFilterType.Blacklist)
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
	}
	
	
	public boolean passesFilter(Player player, PlayerList ops)
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
			return ops.contains(player.getName());
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
