/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;
import java.io.FilenameFilter;

import tectonicus.texture.ZipStack;
import tectonicus.texture.ZipStack.ZipStackEntry;
import tectonicus.util.OsDetect;

public class Minecraft
{

	public static File findMinecraftDir()
	{
		File result;
		
		File userHome = new File(System.getProperty("user.home"));
		
		if (OsDetect.isMac())
		{
			result = new File(userHome, "Library/Application Support/minecraft");
		}
		else if (OsDetect.isWindows())
		{
			result = new File(System.getenv("APPDATA"), ".minecraft");
		}
		else
		{
			result = new File(userHome, ".minecraft");
		}
		
		return result;
	}
	
	public static File findMinecraftJar()
	{
		File versionsDir = new File(findMinecraftDir(), "versions");
		
		if(versionsDir.exists())
		{
			System.out.println("Searching for most recent Minecraft jar...");
			String[] directories = versionsDir.list(new FilenameFilter() {
			  @Override
			  public boolean accept(File dir, String name) {
			    return new File(dir, name).isDirectory();
			  }
			});
			
			String major = "0";
			String minor = "0";
			String patch = "0";
			for(String directory : directories)
			{
				String[] version = directory.split("\\.");
				try 
				{
					if(version.length == 2)
					{
						if(Integer.parseInt(major) < Integer.parseInt(version[0]))
						{
							major = version[0];
							minor = "0";
							patch = "0";
						}
						if(Integer.parseInt(minor) < Integer.parseInt(version[1]))
						{
							minor = version[1];
							patch = "0";
						}
					}
					else if (version.length == 3)
					{
						if(Integer.parseInt(major) < Integer.parseInt(version[0]))
						{
							major = version[0];
							minor = "0";
							patch = "0";
						}
						if(Integer.parseInt(minor) < Integer.parseInt(version[1]))
						{
							minor = version[1];
							patch = "0";
						}
						if(Integer.parseInt(patch) < Integer.parseInt(version[2]))
							patch = version[2];
					}
				}
				catch(NumberFormatException e)
				{
					System.out.println("Skipping directory with invalid version number: " + directory);
				}
			}
			String version;
			if(patch.equals("0"))
				version = major + "." + minor;
			else
				version = major + "." + minor + "." + patch;
			return new File(findMinecraftDir(), "versions/" + version + "/" + version + ".jar");
		}
		else	
			return new File(findMinecraftDir(), "bin/minecraft.jar");
	}
	
	public static File findWorldDir(final int index)
	{
		assert (index >= 0);
		assert (index < 5);
		
		return new File(Minecraft.findMinecraftDir(), "saves/World"+(index+1));
	}

	public static boolean isValidWorldDir(File worldDir)
	{
		if (worldDir == null)
			return false;
		
		File levelDat =  findLevelDat(worldDir);
		return levelDat.exists();
	}
	
	public static boolean isValidMinecraftJar(File minecraftJar)
	{
		if (minecraftJar == null)
			return false;
		
		if (!minecraftJar.exists())
			return false;
		
		try
		{
			ZipStack zips = new ZipStack(minecraftJar, null);
			
			//ZipStackEntry terrainEntry = zips.getEntry("terrain.png");
			
			return (zips.getEntry("terrain.png") != null || zips.getEntry("textures/blocks/activatorRail.png") != null || zips.getEntry("assets/minecraft/textures/blocks/rail_activator.png") != null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String getTexturePackVersion(File minecraftJar)
	{
		ZipStack zipStack;
		
		try
		{
			zipStack = new ZipStack(minecraftJar, null);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't open jar files for texture reading", e);
		}
		
		ZipStackEntry terrainEntry = zipStack.getEntry("terrain.png");
		if (terrainEntry != null)
			return "1.4";
		else
			return "1.5";
	}

	public static File findLevelDat(File worldDir)
	{
		if (worldDir == null)
			return null;
		
		return new File(worldDir, "level.dat");
	}
	
	public static File findPlayersDir(File worldDir)
	{
		if (worldDir == null)
			return null;
		
		File newDir = new File(worldDir, "playerdata");
		if (newDir.exists())
			return newDir;
		else
			return new File(worldDir, "players");
	}

	public static File findServerPlayerFile(File worldDir, String name)
	{
		if (worldDir == null)
			return null;
		
		File json = new File(worldDir.getParentFile(), name+".json");
		File txt = null;
		if (name.equals("whitelist"))
			txt = new File(worldDir.getParentFile(), "white-list.txt");
		else
			txt = new File(worldDir.getParentFile(), name+".txt");
		
		if (json.exists() && !json.isDirectory())
		{
			return json;
		}
		else if (txt.exists() && !txt.isDirectory())
		{		
			return txt;
		}
		else
		{
			return worldDir.getParentFile();
		}
	}

	/** Look for dimensionDir/region/*.mcr or dimensionDir/region/*.mca */
	public static boolean isValidDimensionDir(File dimensionDir)
	{
		File regionDir = new File(dimensionDir, "region");
		if (!regionDir.exists())
			return false;
		
		File[] mcRegionFiles = regionDir.listFiles(new McRegionFileFilter());
		
		File[] anvilRegionFiles = regionDir.listFiles(new AnvilFileFilter());
		
		return (mcRegionFiles != null && mcRegionFiles.length > 0)
				|| (anvilRegionFiles != null && anvilRegionFiles.length > 0);
	}
}
