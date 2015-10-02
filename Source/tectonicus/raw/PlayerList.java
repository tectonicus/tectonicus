/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;
import org.json.JSONArray;

import tectonicus.util.FileUtils;

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
		
		String ext = "";
		int index = playerFile.getAbsolutePath().lastIndexOf('.');
		if (index > 0)
			ext = playerFile.getAbsolutePath().substring(index+1);
		if (ext.equals("json"))
		{			
			JSONArray array = new JSONArray(FileUtils.loadJSON(new FileInputStream(playerFile)));
			for (int i=0; i<array.length(); i++)
			{
				String name = array.getJSONObject(i).getString("name");
				playerNames.add(name.toLowerCase());
			}
		}
		else if (ext.equals("txt"))
		{
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
							playerNames.add( line.trim().toLowerCase() );
					}
				}
				
			}
			finally
			{
				if (scanner != null)
					scanner.close();
			}
		}
		else
		{
			System.out.println("No players file found at "+playerFile.getAbsolutePath());
		}
		
		System.out.println("\tfound "+playerNames.size()+" players");
	}
	
	public boolean contains(String playerName)
	{
		return playerNames.contains(playerName.toLowerCase()) ? true : false;
	}
}
