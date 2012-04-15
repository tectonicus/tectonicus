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
