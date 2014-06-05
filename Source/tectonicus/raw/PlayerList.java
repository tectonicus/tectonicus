/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
