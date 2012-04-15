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
package tectonicus.raw;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import tectonicus.Minecraft;

public class PlayerList
{
	private ArrayList<String> playerNames;
	
	/** Creates an empty player list */
	public PlayerList()
	{
		playerNames = new ArrayList<String>();
	}
	
	/** Creates a player list from the contents of the text file.
	 *  Will throw an exception if the file is not present or ill-formed
	 */
	public PlayerList(File playerFile) throws Exception
	{
		playerNames = new ArrayList<String>();
		
		Scanner scanner = null;
		try
		{
			if (playerFile.exists())
			{
				scanner = new Scanner(playerFile);
				
				while (scanner.hasNextLine())
				{
					String line = scanner.nextLine();
					if (line != null)
						playerNames.add( line.trim() );
				}
			}
			else
			{
				System.out.println("No players file found at "+playerFile.getAbsolutePath());
			}
		}
		finally
		{
			if (scanner != null)
				scanner.close();
		}
		
		System.out.println("\tfound "+playerNames.size()+" players");
	}
	
	public boolean contains(String playerName)
	{
		for (String name : playerNames)
		{
			if (name.equalsIgnoreCase(playerName))
				return true;
		}
		return false;
	}
	
	public static PlayerList loadOps(File worldDir)
	{
		File opsFile = Minecraft.findOpsFile(worldDir);
		System.out.println("Loading ops from "+opsFile.getAbsolutePath());
		
		try
		{
			PlayerList ops = new PlayerList(opsFile);
			return ops;
		}
		catch (Exception e)
		{
			System.out.println("Error while loading ops from "+opsFile.getAbsolutePath());
			e.printStackTrace();
		}
		
		return new PlayerList();
	}
}
